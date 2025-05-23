package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.Method_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.List;

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

    private boolean setIsAnnotationAndInterface(Class_.Builder object_, CtType ctType) throws ClassCastException {
        boolean isAnnotation = false;
        CtClass ctClass;
        CtAnnotationType ctAnnotationType;
        if (ctType.isInterface()) {
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
                    throw new ClassCastException();
                }
            }
        }
        return isAnnotation;
    }

    private void addFilePath(CtType ctType, Class_.Builder object_) {
        try {
            object_.setFilePath(ctType.getPosition().getFile().toString());
        }
        catch (NullPointerException e){
            logger.error("File not found for \"" + ctType.getQualifiedName() + "\"");
            object_.setFilePath("$$UNKNOWNPATH$$");
        }
    }

    private void parseFields(CtType ctType, Class_.Builder object_, List<String> textAndNames) {
        List<String> fieldTypes = new ArrayList<>(ctType.getFields().size());
        for (Object f: ctType.getAllFields()){
            CtFieldReference field = (CtFieldReference) f;
            fieldTypes.add(field.getType().getQualifiedName());
            textAndNames.add(field.getSimpleName());
            //f.getReferencedTypes().forEach(type -> fieldTypes.add(type.getQualifiedName()));
        }
        object_.addAllFieldTypes(fieldTypes);
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
        // start executable
        method_.setIsLambda(false);
        method_.setIsConstructor(isConstructor);
        method_.setSimpleName(method.getSimpleName());
        method_.setParentName(ctType.getQualifiedName());
        // find if method contains source code in the repository or is inherited from a third party package
        if (method.getPosition().isValidPosition()) {
            method_.setContent(method.toString());
            method_.setIsLocal(true);
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
        }
        // get parameters
        List<String> parameters = new ArrayList<>();
        List<String> methodParameterTypes = new ArrayList<>();
        for (Object p:method.getParameters()){
            CtParameter parameter = (CtParameter) p;
            //methodName += "::" + parameter.getSimpleName();
            parameterTypes.add(parameter.getType().getQualifiedName());
            methodParameterTypes.add(parameter.getType().getQualifiedName());
            parameters.add(parameter.getSimpleName());
            textAndNames.add(parameter.getSimpleName());
            methodTextAndNames.add(parameter.getSimpleName());
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
        boolean isAnnotation;
        try {
            isAnnotation = setIsAnnotationAndInterface(object_, ctType);
        } catch (ClassCastException e) {
            logger.error("encountered cast error in line " + ctType.getOriginalSourceFragment().getSourcePosition());
            return;
        }
        object_.setIsImplicit(ctType.isImplicit());
        object_.setIsAnonymous(ctType.isAnonymous());
        object_.setSimpleName(ctType.getSimpleName());
        object_.setFullName(ctType.getQualifiedName());
        object_.setContent(ctType.toString());
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
            try {
                CtClass ctClass = (CtClass) ctType;
                for (Object c : ctClass.getConstructors()) {
                    CtConstructor constructor = (CtConstructor) c;
                    Method_.Builder method_ = Method_.newBuilder();
                    parseMethod(constructor, method_, ctType, textAndNames, parameterTypes, returnTypes, classConstructors,
                            true);
                    methods.add(method_.build());
                }
            }
            catch (ClassCastException e) {
                logger.error("encountered cast error in line " + ctType.getOriginalSourceFragment().getSourcePosition());
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
