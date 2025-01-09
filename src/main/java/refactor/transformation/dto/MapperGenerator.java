package refactor.transformation.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import refactor.ProjectMetadata;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.*;
import spoon.support.reflect.code.CtIfImpl;
import spoon.support.reflect.code.CtReturnImpl;

import java.util.*;

public class MapperGenerator {
    public static final String MAPPER_QUALIFIER = ProjectMetadata.MapStructPackage + ".Mapper";
    public static final String MAPPERS_QUALIFIER = ProjectMetadata.MapStructPackage + ".factory.Mappers";
    public static final String INIT_KRYO_NAME = "createKryoInstance";
    public static final String KRYO_QUALIFIER = "com.esotericsoftware.kryo.Kryo";
    public static final String PACKAGE_SUFFIX = ".mapper";
    public static final String FORWARD_MAP_INPUT_NAME = "entity";
    public static final String REVERSE_MAP_INPUT_NAME = "dto";
    private final Factory factory;
    private final CtType<?> originalType;
    private final CtType<?> dtoType;
    private String mapperName;
    private CtInterface<?> mapperType;
    private CtMethod<?> forwardMapMethod;
    private CtMethod<?> reverseMapMethod;
    private CtAnnotation<?> mapperAnnotation;
    private final Set<CtInterface<?>> reusedMappers;
    private CtMethod<?> initKryoMethod;
    private CtMethod<?> fieldValueMethod;
    private Logger logger = LoggerFactory.getLogger(MapperGenerator.class);


    public MapperGenerator(Factory factory, CtType<?> originalType, CtType<?> dtoType) {
        this.factory = factory;
        this.originalType = originalType;
        this.dtoType = dtoType;
        reusedMappers = new HashSet<>();
        initKryoMethod = null;
        fieldValueMethod = null;

        generateMapper();
    }

    public CtInterface<?> getMapper() {
        return mapperType;
    }

    private void generateMapper() {
        this.mapperName = ProjectMetadata.CLASSNAME_PREFIX + originalType.getSimpleName() + ProjectMetadata.MAPPER_SUFFIX;
        CtPackage parentPackage = dtoType.getPackage().getDeclaringPackage();
        String mapperPackageName;
        if (parentPackage != null){
            mapperPackageName = parentPackage.getQualifiedName() + PACKAGE_SUFFIX;
        }
        else {
            mapperPackageName = PACKAGE_SUFFIX.substring(1);
        }
        CtPackage mapperPackage = factory.Package().getOrCreate(mapperPackageName);
        this.mapperType = factory.Interface().create(mapperPackage, mapperName);
        this.mapperType.addModifier(ModifierKind.PUBLIC);

        mapperAnnotation = factory.Annotation().annotate(mapperType, factory.Type().createReference(MAPPER_QUALIFIER));

        CtField<?> instanceField = generateInstanceField();
        mapperType.addField(instanceField);

        generateMapMethods();
    }

    private void updateMapperAnnotation(){
        if (!reusedMappers.isEmpty()) {
            Set<CtLiteral<Class<?>>> mapperLiterals = new HashSet<>();
            Set<CtFieldAccess<?>> mapperClasses = new HashSet<>();
            CtNewArray<Class<?>> mappersArray = factory.createNewArray();
            for (CtInterface<?> mapper : reusedMappers) {
                CtTypeReference<?> usedMapperRef = mapper.getReference();
                CtLiteral<Class<?>> usedMapperLiteral = factory.Code().createLiteral(usedMapperRef.getClass());
                mapperLiterals.add(usedMapperLiteral);
                CtFieldAccess<?> fieldAccess = factory.Code().createClassAccess(mapper.getReference());
                mapperClasses.add(fieldAccess);
                mappersArray.addElement(fieldAccess);
            }
            mapperAnnotation.addValue("uses", mappersArray);
        }
    }

    private void generateMapMethods(){
        Set<ModifierKind> mapMethodModifiers = Set.of(ModifierKind.PUBLIC);

        String forwardMethodName = originalType.getSimpleName() + "To" + dtoType.getSimpleName();
        List<CtParameter<?>> typeParameters = new ArrayList<>();
        typeParameters.add(factory.createParameter(null, originalType.getReference(), FORWARD_MAP_INPUT_NAME));
        forwardMapMethod = factory.Method().create(
                mapperType,
                mapMethodModifiers,
                dtoType.getReference(),
                forwardMethodName,
                typeParameters,
                null
        );

        String reverseMethodName = dtoType.getSimpleName() + "To" + originalType.getSimpleName();
        List<CtParameter<?>> dtoTypeParameters = new ArrayList<>();
        dtoTypeParameters.add(factory.createParameter(null, dtoType.getReference(), REVERSE_MAP_INPUT_NAME));
        reverseMapMethod = factory.Method().create(
                mapperType,
                mapMethodModifiers,
                originalType.getReference(),
                reverseMethodName,
                dtoTypeParameters,
                null
        );
    }

    private CtField<?> generateInstanceField(){
        Set<ModifierKind> instanceFieldModifiers = Set.of(ModifierKind.PUBLIC, ModifierKind.STATIC, ModifierKind.FINAL);
        CtTypeReference<?> mappersClassRef = factory.Type().createReference(MAPPERS_QUALIFIER);
        CtExecutableReference<?> getMapperMethodRef = factory.Executable().createReference(
                mappersClassRef,
                null,
                "getMapper",
                List.of(factory.Type().createReference("java.lang.Class"))
        );
        CtFieldAccess<?> classFieldAccess = factory.Code().createClassAccess(mapperType.getReference());
        CtInvocation<?> getMapperInvocation = factory.Code().createInvocation(
                factory.Code().createTypeAccessWithoutCloningReference(mappersClassRef),
                getMapperMethodRef,
                classFieldAccess
        );
        CtTypeReference mapperReference = mapperType.getReference();
        return factory.Field().create(
                mapperType,
                instanceFieldModifiers,
                mapperReference, "INSTANCE",
                getMapperInvocation
        );
    }

    public void addAnotherMapper(CtInterface<?> anotherMapper){
        reusedMappers.add(anotherMapper);
        updateMapperAnnotation();
    }

    public void addByteDataMapping(CtTypeReference<?> fieldType, String fieldName){
        String serializeMethodName = getSerializeMethodName(fieldType);
        String deserializeMethodName = getDeserializeMethodName(fieldType);
        if (!mapperType.getMethodsByName(serializeMethodName).isEmpty()){
            logger.debug("Serialize method for " + fieldType.getSimpleName() + " already exists");
        }
        else {
            if (initKryoMethod == null){
                addKryoInitializationMethod();
            }
            updateKryoInitializationMethod(fieldType);
            CtMethod<?> serializeMethod = generateSerializeMethod(fieldType);
            CtMethod<?> deserializeMethod = generateDeserializeMethod(fieldType);
            mapperType.addMethod(serializeMethod);
            mapperType.addMethod(deserializeMethod);
        }
        addMappingAnnotation(forwardMapMethod, fieldName, serializeMethodName, FORWARD_MAP_INPUT_NAME, fieldType);
        addMappingAnnotation(reverseMapMethod, fieldName, deserializeMethodName, REVERSE_MAP_INPUT_NAME, ProjectMetadata.getByteArrayType(factory));
    }

    private void addMappingAnnotation(CtMethod<?> method, String fieldName, String targetMethodName, String inputName, CtTypeReference<?> fieldType){
        if (fieldValueMethod == null){
            addGetFieldValueMethod();
        }
        CtAnnotation<?> mappingAnnotation = factory.Annotation().annotate(method, factory.Type().createReference(ProjectMetadata.MapStructPackage + ".Mapping"));
        mappingAnnotation.addValue("target", fieldName);
        String expression = "java(" + targetMethodName + "((" + fieldType.getQualifiedName() + ") getFieldValue(" + inputName + ", \"" + fieldName + "\")))";
        mappingAnnotation.addValue("expression", expression);
    }

    private void addGetFieldValueMethod(){
        if (fieldValueMethod != null){
            return;
        }
        Set<ModifierKind> methodModifiers = Set.of(ModifierKind.PUBLIC);

        String methodName = "getFieldValue";
        List<CtParameter<?>> typeParameters = new ArrayList<>();
        typeParameters.add(factory.createParameter(null, factory.Type().OBJECT, "source"));
        typeParameters.add(factory.createParameter(null, factory.Type().STRING, "fieldName"));
        // Create method signature
        fieldValueMethod = factory.Method().create(
                mapperType,
                methodModifiers,
                factory.Type().OBJECT,
                methodName,
                typeParameters,
                null
        );
        fieldValueMethod.setDefaultMethod(true);
        // create try-catch block
        CtTry fieldValueTryCatch = factory.createTry();
        CtBlock<?> tryBlock = factory.createBlock();
        CtTypeReference<?> fieldRef = factory.Type().createReference("java.lang.reflect.Field");
        CtExpression fieldClass = factory.Code().createCodeSnippetExpression("source.getClass().getDeclaredField(fieldName)");
        CtLocalVariable<?> fieldVar = factory.createLocalVariable(fieldRef, "field", fieldClass);
        tryBlock.addStatement(fieldVar);
        CtStatement tryStatement = factory.Code().createCodeSnippetStatement(
                "field.setAccessible(true);\n" +
                "return field.get(source)"
        );
        tryBlock.addStatement(tryStatement);
        fieldValueTryCatch.setBody(tryBlock);
        // create catch block
        CtCatch fieldValueCatch = factory.createCatch();
        CtCatchVariable eVar = factory.createCatchVariable(factory.Type().createReference("java.lang.Exception"), "e");
        fieldValueCatch.setParameter(eVar);
        CtBlock<?> catchBody = factory.createBlock();
        CtStatement catchStatement = factory.Code().createCodeSnippetStatement(
                "e.printStackTrace();\n" +
                "return null"
        );
        catchBody.addStatement(catchStatement);
        fieldValueCatch.setBody(catchBody);
        fieldValueTryCatch.addCatcher(fieldValueCatch);
        fieldValueMethod.setBody(fieldValueTryCatch);
        // add method to mapper
        mapperType.addMethod(fieldValueMethod);
    }

    private void updateKryoInitializationMethod(CtTypeReference<?> fieldType){
        if (initKryoMethod == null){
            addKryoInitializationMethod();
        }
        CtBlock<?> body = initKryoMethod.getBody();
        // create statement kryo.register(fieldType);
        CtCodeSnippetStatement registerStatement = factory.Code().createCodeSnippetStatement("kryo.register(" + fieldType.getSimpleName() + ".class)");
        body.addStatement(body.getStatements().size() - 1, registerStatement);
    }

    private void addKryoInitializationMethod(){
        Set<ModifierKind> initMethodModifiers = Set.of(ModifierKind.PUBLIC);
        initKryoMethod = factory.Method().create(
                mapperType,
                initMethodModifiers,
                factory.Type().createReference(KRYO_QUALIFIER),
                INIT_KRYO_NAME,
                new ArrayList<>(),
                null
        );
        initKryoMethod.setDefaultMethod(true);
        CtBlock<Object> body = factory.createBlock();
        // add Kryo kryo = new Kryo();
        CtTypeReference<?> kryoType = factory.Type().createReference(KRYO_QUALIFIER);
        CtLocalVariable<?> kryoVar = factory.createLocalVariable(kryoType, "kryo", factory.Code().createConstructorCall(factory.Type().createReference(KRYO_QUALIFIER)));
//        CtStatement initKryoStatement = factory.Code().createCodeSnippetStatement("Kryo kryo = new Kryo()");
        CtStatement NoRegisterStatement = factory.Code().createCodeSnippetStatement("kryo.setRegistrationRequired(false)");
        // add kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        CtTypeReference<?> stdInstantiatorStrategyType = factory.Type().createReference("org.objenesis.strategy.StdInstantiatorStrategy");
        CtConstructorCall stdInstantiatorStrategy = factory.Code().createConstructorCall(stdInstantiatorStrategyType);
        CtTypeReference<?> defaultInstantiatorStrategyType = factory.Type().createReference("com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy");
        CtConstructorCall defaultInstantiatorStrategy = factory.Code().createConstructorCall(defaultInstantiatorStrategyType, stdInstantiatorStrategy);
        CtTypeReference<?> instantiatorStrategyType = factory.Type().createReference("com.esotericsoftware.kryo.instantiator.strategy.InstantiatorStrategy");
        CtExecutableReference<?> setInstantiatorStrategyRef = factory.Executable().createReference(kryoType, null, "setInstantiatorStrategy", List.of(instantiatorStrategyType));
        CtExpression<?> varAccess = factory.Code().createVariableRead(kryoVar.getReference(), false);
        CtInvocation<?> setInstantiatorStrategy = factory.Code().createInvocation(varAccess, setInstantiatorStrategyRef, defaultInstantiatorStrategy);
//        CtStatement InstantiatorStatement = factory.Code().createCodeSnippetStatement("kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()))");
        CtStatement returnStatement = factory.Code().createCodeSnippetStatement("return kryo");
//        body.addStatement(initKryoStatement);
        body.addStatement(kryoVar);
        body.addStatement(NoRegisterStatement);
//        body.addStatement(InstantiatorStatement);
        body.addStatement(setInstantiatorStrategy);
        body.addStatement(returnStatement);
        initKryoMethod.setBody(body);
        mapperType.addMethod(initKryoMethod);
    }
    
    private String getSerializeMethodName(CtTypeReference<?> fieldType){
        return fieldType.getSimpleName() + "ToBytes";
    }
    
    private String getDeserializeMethodName(CtTypeReference<?> fieldType){
        return "BytesTo" + fieldType.getSimpleName();
    }

    private CtMethod<?> generateSerializeMethod(CtTypeReference<?> fieldType){
        Set<ModifierKind> mapMethodModifiers = Set.of(ModifierKind.PUBLIC);
//        CtTypeReference<?> byteArrayType = factory.Type().createArrayReference(factory.Type().BYTE_PRIMITIVE);
        CtTypeReference<?> byteArrayType = ProjectMetadata.getByteArrayType(factory);

        String serializeMethodName = getSerializeMethodName(fieldType);
        List<CtParameter<?>> typeParameters = new ArrayList<>();
        // Create method signature
        CtParameter<?> inputParam = factory.createParameter(null, fieldType, "field");
        typeParameters.add(inputParam);
        CtMethod<?> serializeMethod = factory.Method().create(
                mapperType,
                mapMethodModifiers,
                byteArrayType,
                serializeMethodName,
                typeParameters,
                null
        );
        serializeMethod.setDefaultMethod(true);
        // Create method body
        CtBlock<Object> body = factory.createBlock();
        serializeMethod.setBody(body);
        // Add if (field == null)
        CtIf ifNullCheck = new CtIfImpl();
        CtBinaryOperator<Boolean> nullCheck = factory.createBinaryOperator(
                factory.createVariableRead(inputParam.getReference(), false),
                factory.createLiteral(null),
                BinaryOperatorKind.EQ);
        ifNullCheck.setCondition(nullCheck);
        // Add return null;
        CtReturn<?> returnNull = new CtReturnImpl<>();
        returnNull.setReturnedExpression(factory.createLiteral(null));
        ifNullCheck.setThenStatement(returnNull);
        body.addStatement(ifNullCheck);
        // Add Kryo kryo = createKryoInstance();
        CtStatement createKryoInstance = factory.Code().createCodeSnippetStatement("Kryo kryo = createKryoInstance()");
        body.addStatement(createKryoInstance);
        // Add ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CtTypeReference<?> byteArrayOutputStreamType = factory.Type().createReference("java.io.ByteArrayOutputStream");
        CtConstructorCall byteArrayOutputStreamConstructor = factory.Code().createConstructorCall(byteArrayOutputStreamType);
        CtLocalVariable<?> outputStreamVar = factory.createLocalVariable(byteArrayOutputStreamType, "outputStream", byteArrayOutputStreamConstructor);
        body.addStatement(outputStreamVar);
        // Add Output output = new Output(outputStream);
        CtTypeReference<?> outputType = factory.Type().createReference("com.esotericsoftware.kryo.io.Output");
        CtVariableAccess<?> outputStreamVarRead = factory.Code().createVariableRead(outputStreamVar.getReference(), false);
        CtConstructorCall outputConstructorCall = factory.Code().createConstructorCall(outputType, outputStreamVarRead);
        CtLocalVariable<?> outputVar = factory.createLocalVariable(outputType, "output", outputConstructorCall);
        body.addStatement(outputVar);
        // Add kryo.writeObject(output, field);
        CtStatement writeObject = factory.Code().createCodeSnippetStatement("kryo.writeClassAndObject(output, field)");
//        CtStatement writeObject = factory.Code().createCodeSnippetStatement("kryo.writeObject(output, field)");
        body.addStatement(writeObject);
        // Add output.close();
        CtStatement closeOutput = factory.Code().createCodeSnippetStatement("output.close()");
        body.addStatement(closeOutput);
        // Add List<Byte> byteList = new ArrayList<>()
        CtTypeReference<?> listByteType = ProjectMetadata.getByteArrayType(factory);
        CtTypeReference<?> arrayListType = factory.Type().createReference(ArrayList.class);
        CtConstructorCall arrayListConstructor = factory.Code().createConstructorCall(arrayListType);
        CtLocalVariable<?> byteListVar = factory.createLocalVariable(listByteType, "byteList", arrayListConstructor);
        body.addStatement(byteListVar);
        // Add byteArray conversion
        CtStatement byteArrayConversion = factory.Code().createCodeSnippetStatement(
                """
                for (byte b : outputStream.toByteArray()) {
                    byteList.add(b);
                }
                return byteList"""
        );
        body.addStatement(byteArrayConversion);
        // Add return outputStream.toByteArray();
//        CtStatement returnStatement = factory.Code().createCodeSnippetStatement("return outputStream.toByteArray()");
//        body.addStatement(returnStatement);

        return serializeMethod;
    }

    private CtMethod<?> generateDeserializeMethod(CtTypeReference<?> fieldType){
        Set<ModifierKind> mapMethodModifiers = Set.of(ModifierKind.PUBLIC);
//        CtTypeReference<?> byteArrayType = factory.Type().createArrayReference(factory.Type().BYTE_PRIMITIVE);
        CtTypeReference<?> byteArrayType = ProjectMetadata.getByteArrayType(factory);

        String deserializeMethodName = getDeserializeMethodName(fieldType);
        List<CtParameter<?>> typeParameters = new ArrayList<>();
        // Create method signature
        CtParameter<?> inputParam = factory.createParameter(null, byteArrayType, "data");
        typeParameters.add(inputParam);
        CtMethod<?> deserializeMethod = factory.Method().create(
                mapperType,
                mapMethodModifiers,
                fieldType,
                deserializeMethodName,
                typeParameters,
                null
        );
        deserializeMethod.setDefaultMethod(true);
        // Create method body
        CtBlock<Object> body = factory.createBlock();
        deserializeMethod.setBody(body);
        CtStatement ifNullCheck = factory.Code().createCodeSnippetStatement("if (data == null) return null");
        body.addStatement(ifNullCheck);
        CtStatement createKryoInstance = factory.Code().createCodeSnippetStatement("Kryo kryo = createKryoInstance()");
        body.addStatement(createKryoInstance);
        // Add byte[] byteArray = new byte[data.size()];
        CtArrayTypeReference<?> simpleByteArrayType = factory.Type().createArrayReference(factory.Type().BYTE_PRIMITIVE);
        CtExpression byteInit = factory.Code().createCodeSnippetExpression("new byte[data.size()]");
        CtLocalVariable<?> byteArrayVar = factory.createLocalVariable(simpleByteArrayType, "byteArray", byteInit);
//        body.addStatement(byteArrayVar);
        // add for loop
        CtStatement forLoop = factory.Code().createCodeSnippetStatement(
                """
                byte[] byteArray = new byte[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    byteArray[i] = data.get(i);
                }"""
        );
        body.addStatement(forLoop);
        // Add ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
        CtTypeReference<?> byteArrayInputStreamType = factory.Type().createReference("java.io.ByteArrayInputStream");
        CtVariableAccess<?> byteArrayVariableRead = factory.Code().createVariableRead(byteArrayVar.getReference(), false);
        CtConstructorCall constructorCall = factory.Code().createConstructorCall(byteArrayInputStreamType, byteArrayVariableRead);
        CtLocalVariable<?> inputStreamVar = factory.createLocalVariable(byteArrayInputStreamType, "inputStream", constructorCall);
        body.addStatement(inputStreamVar);

        // Add ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
//        CtTypeReference<?> byteArrayInputStreamType = factory.Type().createReference("java.io.ByteArrayInputStream");
//        CtVariableAccess<?> dataVariableRead = factory.Code().createVariableRead(inputParam.getReference(), false);
//        CtConstructorCall constructorCall = factory.Code().createConstructorCall(byteArrayInputStreamType, dataVariableRead);
//        CtLocalVariable<?> inputStreamVar = factory.createLocalVariable(byteArrayInputStreamType, "inputStream", constructorCall);
//        body.addStatement(inputStreamVar);
        // Add Input input = new Input(inputStream);
        CtTypeReference<?> inputType = factory.Type().createReference("com.esotericsoftware.kryo.io.Input");
        CtVariableAccess<?> inputStreamVarRead = factory.Code().createVariableRead(inputStreamVar.getReference(), false);
        CtConstructorCall inputConstructorCall = factory.Code().createConstructorCall(inputType, inputStreamVarRead);
        CtLocalVariable<?> inputVar = factory.createLocalVariable(inputType, "input", inputConstructorCall);
        body.addStatement(inputVar);
        // Add fieldType field = kryo.readObject(input, fieldType.class);
//        CtStatement fieldAcces = factory.Code().createCodeSnippetStatement(fieldType.getSimpleName() + " field = kryo.readObject(input, "+ fieldType.getSimpleName() + ".class)")
        CtStatement fieldAcces = factory.Code().createCodeSnippetStatement(fieldType.getSimpleName() + " field = ("+ fieldType.getSimpleName() + ") kryo.readClassAndObject(input)");
        body.addStatement(fieldAcces);
        // Add input.close();
        CtStatement closeInput = factory.Code().createCodeSnippetStatement("input.close()");
        body.addStatement(closeInput);
        // Add return field;
        CtStatement returnField = factory.Code().createCodeSnippetStatement("return field");
        body.addStatement(returnField);
        return deserializeMethod;
    }
}
