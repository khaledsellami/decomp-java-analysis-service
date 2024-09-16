package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.CodeSpan;
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

    @Override
    public void process(CtType ctType) {
        // logger.info("Started processing type \"" + ctType.getQualifiedName() + "\"");
        Class_.Builder object_ = Class_.newBuilder();
        boolean isAnnotation = false;
        String logText = "class";
        CtClass ctClass;
        CtAnnotationType ctAnnotationType;
        if (ctType.isInterface()) {
            logText = "interface";
            object_.setIsInterface(true);
            isAnnotation = false;
            ctClass = null;
            ctAnnotationType = null;
        }
        else {
            try{
                ctClass = (CtClass) ctType;
                object_.setIsInterface(false);
                isAnnotation = false;
                ctAnnotationType = null;
            }
            catch (ClassCastException e){
                try {
                    ctAnnotationType = (CtAnnotationType) ctType;
                    object_.setIsInterface(false);
                    isAnnotation = true;
                    ctClass = null;
                }
                catch (ClassCastException e2){
                    logger.error(
                            "encountered cast error in line " + ctType.getOriginalSourceFragment().getSourcePosition()
                    );
                    return;
                }
            }
        }
        object_.setIsImplicit(ctType.isImplicit());
        object_.setIsAnonymous(ctType.isAnonymous());
        object_.setSimpleName(ctType.getSimpleName());
        object_.setFullName(ctType.getQualifiedName());
        object_.setContent(ctType.toString());
        CodeSpan span = buildCodeSpan(ctType.getPosition());
        if (span!=null){
            object_.setSpan(span);
        }
        else {
            logger.error("Span not found for " + ctType.getQualifiedName());
        }
        try {
            object_.setFilePath(ctType.getPosition().getFile().toString());
        }
        catch (NullPointerException e){
            logger.error("File not found for \"" + ctType.getQualifiedName() + "\"");
            object_.setFilePath("$$UNKNOWNPATH$$");
        }
        List<String> textAndNames = new ArrayList<>();
        textAndNames.add(ctType.getSimpleName());
        //logger.info("Adding referenced types for \"" + ctType.getSimpleName() + "\"");
        List<String> referencedTypes = new ArrayList<>(ctType.getReferencedTypes().size());
        ctType.getReferencedTypes().forEach(type -> referencedTypes.add(type.getQualifiedName()));
        object_.addAllReferencedTypes(referencedTypes);
        //logger.info("Adding field types for \"" + ctType.getSimpleName() + "\"");
        List<String> fieldTypes = new ArrayList<>(ctType.getFields().size());
        for (Object f: ctType.getAllFields()){
            CtFieldReference field = (CtFieldReference) f;
            fieldTypes.add(field.getType().getQualifiedName());
            textAndNames.add(field.getSimpleName());
            //f.getReferencedTypes().forEach(type -> fieldTypes.add(type.getQualifiedName()));
        }
        object_.addAllFieldTypes(fieldTypes);
        //logger.info("Adding nested types for \"" + ctType.getSimpleName() + "\"");
        List<String> nestedTypes = new ArrayList<>(ctType.getNestedTypes().size());
        for (Object t: ctType.getNestedTypes()){
            CtType type = (CtType) t;
            nestedTypes.add(type.getQualifiedName());
            textAndNames.add(type.getSimpleName());
        }
        object_.addAllNestedTypes(nestedTypes);
        //logger.info("Adding extended or implemented types for \"" + ctType.getSimpleName() + "\"");
        List<String> inheritedTypes = new ArrayList<>();
        CtTypeReference superClass = ctType.getSuperclass();
        if (superClass != null){
            inheritedTypes.add(superClass.getQualifiedName());
        }
        ctType.getSuperInterfaces().forEach(ref -> inheritedTypes.add(ref.getQualifiedName()));
        object_.addAllInheritedTypes(inheritedTypes);
        //logger.info("Adding methods and parameter and return types for \"" + ctType.getSimpleName() + "\"");
        List<String> parameterTypes = new ArrayList<>();
        List<String> returnTypes = new ArrayList<>(ctType.getMethods().size());
        List<String> classMethods = new ArrayList<>(ctType.getMethods().size());
        for (Object m:ctType.getAllMethods()){
            CtMethod method = (CtMethod) m;
            List<String> methodTextAndNames = new ArrayList<>();
            //String methodName = method.getSimpleName();
            //logger.info("Processing method \"" + methodName + "\" for " + logText + " \"" +
            // ctType.getSimpleName() + "\"");
            Method_.Builder method_ = Method_.newBuilder();
            // start executable
            method_.setIsLambda(false);
            method_.setIsConstructor(false);
            method_.setSimpleName(method.getSimpleName());
            method_.setParentName(ctType.getQualifiedName());
            // find if method contains source code in the repository or is inherited from a third party package
            if (method.getPosition().isValidPosition()) {
                method_.setContent(method.toString());
                method_.setIsLocal(true);
                span = buildCodeSpan(method.getPosition());
                if (span!=null){
                    method_.setSpan(span);
                }
                else {
                    logger.error("Span not found for " + method.getSimpleName() + " in " + ctType.getQualifiedName());
                }
            }
            else{
                method_.setContent(method.toString());
                method_.setIsLocal(false);
            }
            // get return type
            returnTypes.add(method.getType().getQualifiedName());
            method_.setReturnType(method.getType().getQualifiedName());
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
            methods.add(method_.build());
        }
        List<String> classConstructors = new ArrayList<>();
        if ((!object_.getIsInterface())&&(!isAnnotation)){
            for (Object c:ctClass.getConstructors()){
                CtConstructor constructor = (CtConstructor) c;
                List<String> constructorTextAndNames = new ArrayList<>();
                Method_.Builder method_ = Method_.newBuilder();
                if (constructor.getPosition().isValidPosition()) {
                    method_.setContent(constructor.toString());
                    method_.setIsLocal(true);
                    span = buildCodeSpan(constructor.getPosition());
                    if (span!=null){
                        method_.setSpan(span);
                    }
                    else {
                        logger.error("Span not found for " + constructor.getSimpleName() + " in " + ctType.getQualifiedName());
                    }
                }
                else{
                    method_.setContent(constructor.toString());
                    method_.setIsLocal(false);
                }
                // start executable
                method_.setIsLambda(false);
                method_.setIsConstructor(true);
                method_.setSimpleName(ctType.getSimpleName());
                method_.setParentName(ctType.getQualifiedName());
                // get return type
                method_.setReturnType("void");
                // get parameters
                List<String> parameters = new ArrayList<>();
                List<String> methodParameterTypes = new ArrayList<>();
                for (Object p:constructor.getParameters()){
                    CtParameter parameter = (CtParameter) p;
                    //methodName += "::" + parameter.getSimpleName();
                    parameterTypes.add(parameter.getType().getQualifiedName());
                    methodParameterTypes.add(parameter.getType().getQualifiedName());
                    parameters.add(parameter.getSimpleName());
                }
                method_.addAllParameterNames(parameters);
                method_.addAllParameterTypes(methodParameterTypes);
                // get referenced types
                List<String> methodReferencedTypes = new ArrayList<>(constructor.getReferencedTypes().size());
                constructor.getReferencedTypes().forEach(type -> methodReferencedTypes.add(type.getQualifiedName()));
                method_.addAllReferencedTypes(methodReferencedTypes);
                // process and add name
                classConstructors.add(constructor.getSignature());
                // add textual terms and comments
                constructor.getComments().forEach(comment -> {
                    textAndNames.add(comment.getContent());
                    constructorTextAndNames.add(comment.getContent());
                });
                textAndNames.add(constructor.getDocComment());
                constructor.getElements(new TypeFilter(CtVariableReference.class)).forEach(
                        var -> {
                            textAndNames.add(((CtVariableReference) var).getSimpleName());
                            constructorTextAndNames.add(((CtVariableReference) var).getSimpleName());
                        } );
                method_.addAllTextAndNames(constructorTextAndNames);
                //methodName = ctType.getQualifiedName() + "::" + methodName;
                method_.setFullName(ctType.getQualifiedName() + "::" + constructor.getSignature());
                method_.setAppName(this.getAppName());
                if (this.getServiceName()!=null)
                    method_.setServiceName(this.getServiceName());
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
