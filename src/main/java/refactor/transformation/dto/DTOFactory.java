package refactor.transformation.dto;

import com.decomp.refactor.ClassDTO;
import com.decomp.refactor.Field;
import com.decomp.refactor.FieldType;
import com.decomp.refactor.TypeSource;
import refactor.ProjectMetadata;
import refactor.analysis.AnalysisContext;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;

public class DTOFactory {

    private final Factory factory;
    private final AnalysisContext context;
    private final Map<String, CtClass<?>> processedClasses;
    private final MapperFactory mapperFactory;
    private final static String PACKAGE_SUFFIX = ".dto";

    public DTOFactory(Factory factory, AnalysisContext context) {
        this.factory = factory;
        this.context = context;
        processedClasses = new HashMap<>();
        mapperFactory = new MapperFactory(factory);
    }

    public Map.Entry<CtClass<?>, CtInterface<?>> generateDto(String name, String packageName) {
        CtType<?> ctType = context.getCtType(name);
        ClassDTO classDTO = context.getDTO(name);
        String className = ProjectMetadata.CLASSNAME_PREFIX + ctType.getSimpleName() + ProjectMetadata.DTO_SUFFIX;
        String dtoPackageName = packageName + PACKAGE_SUFFIX;
        String qualifiedName = dtoPackageName + "." + className;
        if (processedClasses.containsKey(qualifiedName)) {
            return new AbstractMap.SimpleEntry<>(processedClasses.get(qualifiedName),
                    mapperFactory.getMapper(qualifiedName).getMapper());
        }
        CtPackage ctPackage = factory.Package().getOrCreate(dtoPackageName);
        CtClass<?> dtoClass = factory.Class().create(ctPackage, className);
        dtoClass.setModifiers(Collections.singleton(ModifierKind.PUBLIC));
        CtInterface<?> mapper = mapperFactory.getOrGenerateMapper(ctType, dtoClass).getMapper();

        Map<String, FieldType> fieldTypes = new HashMap<>();
        for (Field field : classDTO.getFieldsList()) {
            FieldType fieldType = field.getType();
            fieldTypes.put(fieldType.getFullName(), fieldType);
        }

        for (CtField<?> field : ctType.getFields()) {
            FieldType fieldType = fieldTypes.get(field.getType().getQualifiedName());
            addFieldToDto(dtoClass, field, fieldType, packageName);
        }

        addGettersAndSetters(dtoClass);

        processedClasses.put(dtoClass.getQualifiedName(), dtoClass);
        return new AbstractMap.SimpleEntry<>(dtoClass, mapper);
    }

    private void addFieldToDto(CtClass<?> dtoClass, CtField<?> field, FieldType fieldType, String packageName) {
        Set<ModifierKind> modifiers = new HashSet<>();
        modifiers.add(ModifierKind.PUBLIC);
        if (fieldType.getIsSerializable() & !fieldType.getTypeSource().equals(TypeSource.LIBRARY)) {
            dtoClass.addField(factory.Field().create(dtoClass, modifiers, field.getType(), field.getSimpleName()));
        } else {
            if (fieldType.getTypeSource().equals(TypeSource.LOCAL)) {
                Map.Entry<CtClass<?>, CtInterface<?>> dtoAndMapper = generateDto(fieldType.getFullName(), packageName);
                CtTypeReference<?> newFieldReference = dtoAndMapper.getKey().getReference();
                dtoClass.addField(factory.Field().create(dtoClass, modifiers, newFieldReference, field.getSimpleName()));
                MapperGenerator mapper = mapperFactory.getMapper(dtoClass.getQualifiedName());
                mapper.addAnotherMapper(dtoAndMapper.getValue());
            } else {
                // Spoon's pretty printer does not print byte[] correctly so we'll use List<Byte> instead
                CtTypeReference<?> listByteType = ProjectMetadata.getByteArrayType(factory);
                CtField<?> byteArrayField = factory.Field().create(dtoClass, modifiers, listByteType, field.getSimpleName());
                dtoClass.addField(byteArrayField);
                MapperGenerator mapper = mapperFactory.getMapper(dtoClass.getQualifiedName());
                mapper.addByteDataMapping(field.getType(), field.getSimpleName());
            }
        }
    }

    private void addGettersAndSetters(CtClass<?> dtoClass) {
        for (CtField<?> field : dtoClass.getFields()) {
            String fieldName = field.getSimpleName();
            String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Set<ModifierKind> modifiers = new HashSet<>();
            modifiers.add(ModifierKind.PUBLIC);


            // Add getter
            String getterName;
            if (field.getType().equals(factory.Type().BOOLEAN_PRIMITIVE))
                getterName = "is" + capitalizedFieldName;
            else
                getterName = "get" + capitalizedFieldName;
            CtCodeSnippetStatement getterBody = factory.createCodeSnippetStatement("return this." + fieldName);
            CtMethod<?> getter = factory.Method().create(dtoClass, modifiers, field.getType(), getterName, new ArrayList<>(), new HashSet<>(), null);
            getter.setBody(getterBody);
            dtoClass.addMethod(getter);

            // Add setter
            String setterName = "set" + capitalizedFieldName;
            CtCodeSnippetStatement setterBody = factory.createCodeSnippetStatement("this." + fieldName + " = " + fieldName);
            CtParameter<?> setterParameter = factory.createParameter(null, field.getType(), fieldName);
            CtMethod<?> setter = factory.Method().create(dtoClass, modifiers, factory.Type().voidPrimitiveType(), setterName,
                    Collections.singletonList(setterParameter), new HashSet<>(), null);
            setter.setBody(setterBody);
            setterParameter.setParent(setter);
            dtoClass.addMethod(setter);
        }
    }
}
