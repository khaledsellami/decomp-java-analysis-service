package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.CodeSpan;
import com.decomp.analysis.Method_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.util.*;

import static processors.Utils.buildCodeSpan;

public class TypeProcessor extends AbstractProcessor<CtType> {
    private List<Class_> objects;
    private List<Method_> methods;
    private Logger logger = LoggerFactory.getLogger(TypeProcessor.class);
    private String appName;
    private String serviceName;
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<Class_> getObjects() {
        return objects;
    }

    public List<Method_> getMethods() {
        return methods;
    }

    public TypeProcessor() {
        super();
        objects = new ArrayList<>();
        methods = new ArrayList<>();
        this.appName = null;

    }

    public TypeProcessor(ArrayList<Class_> objects, ArrayList<Method_> methods, String appName) {
        super();
        this.objects = objects;
        this.methods = methods;
        this.appName = appName;
        this.serviceName = null;
    }

    public TypeProcessor(ArrayList<Class_> objects, ArrayList<Method_> methods, String appName, String serviceName) {
        super();
        this.objects = objects;
        this.methods = methods;
        this.appName = appName;
        this.serviceName = serviceName;
    }

    private boolean setIsAnnotationAndInterface(Class_.Builder object_, CtType ctType) {
        boolean isAnnotation = false;
        String logText = "class";
        CtClass ctClass;
        CtAnnotationType ctAnnotationType;
        if (ctType.isInterface()) {
            logText = "interface";
            object_.setIsInterface(true);
            isAnnotation = false;
        } else {
            try {
                ctClass = (CtClass) ctType;
                object_.setIsInterface(false);
                isAnnotation = false;
            } catch (ClassCastException e) {
                try {
                    ctAnnotationType = (CtAnnotationType) ctType;
                    object_.setIsInterface(false);
                    isAnnotation = true;
                } catch (ClassCastException e2) {
                    logger.error(
                            "encountered cast error in line " + ctType.getOriginalSourceFragment().getSourcePosition()
                    );
                }
            }
        }
        return isAnnotation;
    }

    private void setSpan(CtType ctType, Class_.Builder object_){
        CodeSpan span = buildCodeSpan(ctType);
        if (span!=null){
            object_.setSpan(span);
        }
        else {
            logger.error("Span not found for " + ctType.getQualifiedName());
        }
    }

    private void setSpan(CtExecutable method, Method_.Builder method_, CtType ctType){
        CodeSpan span = buildCodeSpan(method);
        if (span!=null){
            method_.setSpan(span);
        }
        else {
            logger.error("Span not found for " + method.getSimpleName() + " in " + ctType.getQualifiedName());
        }
    }

    private void addFilePath(CtType ctType, Class_.Builder object_) {
        File file = ctType.getPosition().getFile();
        if (file!=null){
            object_.setFilePath(file.toString());
        }
        else {
            logger.error("File not found for " + ctType.getQualifiedName());
            object_.setFilePath("$$UNKNOWNPATH$$");
        }
    }

    private List<String> getAllGenerics(CtTypeReference ctTypeReference){
        List<String> generics = new ArrayList<>();
        if (!ctTypeReference.getActualTypeArguments().isEmpty()) {
            for (CtTypeReference<?> t : ctTypeReference.getActualTypeArguments()) {
                generics.add(t.getQualifiedName());
            }
        }
        return generics;
    }

    private void parseFields(CtType ctType, Class_.Builder object_, List<String> textAndNames) {
        List<String> fieldTypes = new ArrayList<>(ctType.getFields().size());
        List<String> genericTypes = new ArrayList<>();
        for (Object f: ctType.getAllFields()){
            CtFieldReference field = (CtFieldReference) f;
            fieldTypes.add(field.getType().getQualifiedName());
            textAndNames.add(field.getSimpleName());
            //f.getReferencedTypes().forEach(type -> fieldTypes.add(type.getQualifiedName()));
            CtTypeReference<?> fieldType = field.getType();
            genericTypes.addAll(getAllGenerics(fieldType));
        }
        object_.addAllFieldTypes(fieldTypes);
        object_.addAllGenericInFieldTypes(genericTypes);
    }

    private void parseNestedTypes(CtType ctType, Class_.Builder object_, List<String> textAndNames) {
        List<String> nestedTypes = new ArrayList<>(ctType.getNestedTypes().size());
        for (Object t: ctType.getNestedTypes()){
            CtType type = (CtType) t;
            nestedTypes.add(type.getQualifiedName());
            textAndNames.add(type.getSimpleName());
        }
        object_.addAllNestedTypes(nestedTypes);
    }

    private void parseInheritedTypes(CtType ctType, Class_.Builder object_) {
        List<String> inheritedTypes = new ArrayList<>();
        CtTypeReference superClass = ctType.getSuperclass();
        if (superClass != null){
            inheritedTypes.add(superClass.getQualifiedName());
        }
        ctType.getSuperInterfaces().forEach(ref -> inheritedTypes.add(ref.getQualifiedName()));
        object_.addAllInheritedTypes(inheritedTypes);
    }

    private void parseMethod(CtExecutable method, Method_.Builder method_, CtType ctType, List<String> textAndNames,
                             List<String> parameterTypes, List<String> returnTypes, List<String> classMethods,
                             boolean isConstructor) {
        List<String> methodTextAndNames = new ArrayList<>();
        List<String> genericInReferences = new ArrayList<>();
        // start executable
        method_.setIsLambda(false);
        method_.setIsConstructor(isConstructor);
        method_.setSimpleName(method.getSimpleName());
        method_.setParentName(ctType.getQualifiedName());
        // find if method contains source code in the repository or is inherited from a third party package
        if (method.getPosition().isValidPosition()) {
            method_.setContent(method.toString());
            method_.setIsLocal(true);
            setSpan(method, method_, ctType);
        }
        else{
            method_.setContent(method.toString());
            method_.setIsLocal(false);
        }
        // get return type
        if (isConstructor)
            method_.setReturnType("void");
        else {
            returnTypes.add(method.getType().getQualifiedName());
            method_.setReturnType(method.getType().getQualifiedName());
            genericInReferences.addAll(getAllGenerics(method.getType()));
        }
        // get parameters
        List<String> parameters = new ArrayList<>();
        List<String> methodParameterTypes = new ArrayList<>();
        for (Object p:method.getParameters()){
            CtParameter parameter = (CtParameter) p;
            //methodName += "::" + parameter.getSimpleName();
            CtTypeReference<?> parameterType = parameter.getType();
            parameterTypes.add(parameterType.getQualifiedName());
            methodParameterTypes.add(parameterType.getQualifiedName());
            parameters.add(parameter.getSimpleName());
            textAndNames.add(parameter.getSimpleName());
            methodTextAndNames.add(parameter.getSimpleName());
            genericInReferences.addAll(getAllGenerics(parameterType));
        }
        method_.addAllParameterNames(parameters);
        method_.addAllParameterTypes(methodParameterTypes);
        // get referenced types
        List<String> methodReferencedTypes = new ArrayList<>(method.getReferencedTypes().size());
        method.getReferencedTypes().forEach(type -> methodReferencedTypes.add(type.getQualifiedName()));
        method_.addAllReferencedTypes(methodReferencedTypes);
        // process and add name
        classMethods.add(method.getSignature());
        // add textual terms and comments
        textAndNames.add(method.getSimpleName());
        methodTextAndNames.add(method.getSimpleName());
        method.getComments().forEach(comment -> {
            textAndNames.add(comment.getContent());
            methodTextAndNames.add(comment.getContent());
        });
        textAndNames.add(method.getDocComment());
        methodTextAndNames.add(method.getDocComment());
        method.getElements(new TypeFilter(CtVariableReference.class)).forEach(
                var -> {
                    textAndNames.add(((CtVariableReference) var).getSimpleName());
                    methodTextAndNames.add(((CtVariableReference) var).getSimpleName());
                });
        method_.addAllTextAndNames(methodTextAndNames);
        // add all local variable types
        List<String> variableTypes = new ArrayList<>();
        for (CtElement lv : method.getBody().getElements(e -> e instanceof CtLocalVariable)) {
            CtLocalVariable<?> localVariable = (CtLocalVariable<?>) lv;
            // Get the variable name and type
            CtTypeReference<?> variableType = localVariable.getType();
            // Store the variable name and its qualified type name
            variableTypes.add(variableType.getQualifiedName());
            genericInReferences.addAll(getAllGenerics(variableType));
        }
        method_.addAllVariableTypes(variableTypes);
        method_.addAllGenericInReferencedTypes(genericInReferences);
        //methodName = ctType.getQualifiedName() + "::" + methodName;
        method_.setFullName(ctType.getQualifiedName() + "::" + method.getSignature());
        method_.setAppName(this.getAppName());
        if (this.getServiceName()!=null)
            method_.setServiceName(this.getServiceName());
    }


    @Override
    public void process(CtType ctType) {
        // logger.info("Started processing type \"" + ctType.getQualifiedName() + "\"");
        Class_.Builder object_ = Class_.newBuilder();
        boolean isAnnotation = setIsAnnotationAndInterface(object_, ctType);
        object_.setIsImplicit(ctType.isImplicit());
        object_.setIsAnonymous(ctType.isAnonymous());
        object_.setSimpleName(ctType.getSimpleName());
        object_.setFullName(ctType.getQualifiedName());
        object_.setContent(ctType.toString());
        setSpan(ctType, object_);
        addFilePath(ctType, object_);
        List<String> textAndNames = new ArrayList<>();
        textAndNames.add(ctType.getSimpleName());
        //logger.info("Adding referenced types for \"" + ctType.getSimpleName() + "\"");
        List<String> referencedTypes = new ArrayList<>(ctType.getReferencedTypes().size());
        ctType.getReferencedTypes().forEach(type -> referencedTypes.add(type.getQualifiedName()));
        object_.addAllReferencedTypes(referencedTypes);
        //logger.info("Adding field types for \"" + ctType.getSimpleName() + "\"");
        parseFields(ctType, object_, textAndNames);
        //logger.info("Adding nested types for \"" + ctType.getSimpleName() + "\"");
        parseNestedTypes(ctType, object_, textAndNames);
        //logger.info("Adding extended or implemented types for \"" + ctType.getSimpleName() + "\"");
        parseInheritedTypes(ctType, object_);
        //logger.info("Adding methods and parameter and return types for \"" + ctType.getSimpleName() + "\"");
        List<String> parameterTypes = new ArrayList<>();
        List<String> returnTypes = new ArrayList<>(ctType.getMethods().size());
        List<String> classMethods = new ArrayList<>(ctType.getMethods().size());
        for (Object m:ctType.getAllMethods()){
            //String methodName = method.getSimpleName();
            //logger.info("Processing method \"" + methodName + "\" for " + logText + " \"" +
            // ctType.getSimpleName() + "\"");
            Method_.Builder method_ = Method_.newBuilder();
            CtMethod method = (CtMethod) m;
            parseMethod(method, method_, ctType, textAndNames, parameterTypes, returnTypes, classMethods, false);
            methods.add(method_.build());
        }
        List<String> classConstructors = new ArrayList<>();
        if ((!object_.getIsInterface())&&(!isAnnotation)){
            CtClass ctClass = (CtClass) ctType;
            for (Object c:ctClass.getConstructors()){
                CtConstructor constructor = (CtConstructor) c;
                Method_.Builder method_ = Method_.newBuilder();
                parseMethod(constructor, method_, ctType, textAndNames, parameterTypes, returnTypes, classConstructors,
                        true);
                methods.add(method_.build());
            }
        }
        // add doc strings and all comments
        ctType.getComments().forEach(comment -> textAndNames.add(comment.getContent()));
        textAndNames.add(ctType.getDocComment());
        // set object fields
        object_.addAllParameterTypes(parameterTypes);
        object_.addAllReturnTypes(returnTypes);
        object_.addAllMethods(classMethods);
        object_.addAllConstructors(classConstructors);
        object_.addAllTextAndNames(textAndNames);
        object_.setAppName(this.getAppName());
        if (this.getServiceName()!=null)
            object_.setServiceName(this.getServiceName());

        objects.add(object_.build());
        //logger.info("Finished processing " + logText + " \"" + ctType.getQualifiedName() + "\"");
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
