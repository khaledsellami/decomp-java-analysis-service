package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.Invocation_;
import com.decomp.analysis.Method_;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.List;

public class InvocationProcessor extends AbstractProcessor<CtInvocation> {
    private List<Class_> objects;
    private List<Method_> methods;
    private Logger logger = LoggerFactory.getLogger(InvocationProcessor.class);
    public int successfulMatches = 0;
    public int failedMatches = 0;
    private List<Invocation_> failedMaps = new ArrayList<>();
    private String appName;
    private String serviceName;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public InvocationProcessor(List<Class_> objects, List<Method_> methods, String appName){
        this.objects = objects;
        this.methods = methods;
        this.appName = appName;
        this.serviceName = null;
    }

    public InvocationProcessor(List<Class_> objects, List<Method_> methods, String appName, String serviceName){
        this.objects = objects;
        this.methods = methods;
        this.appName = appName;
        this.serviceName = serviceName;
    }

    public Integer findMethod(String methodName){
        int i = 0;
        for (Method_ e:methods){
            if (e.getFullName().equals(methodName))
                return i;
            i++;
        }
        logger.error("Method " + methodName + " not found!");
        return null;
    }
    public Integer findObject(String objectName){
        int i = 0;
        for (Class_ o:objects){
            if (o.getFullName().equals(objectName))
                return i;
            i++;
        }
        return null;
    }

    public boolean isLocal(String className){
        for (Class_ o:objects){
            if (o.getFullName().equals(className))
                return true;
        }
        //logger.error("Object " + className + " not found!");
        return false;
    }

    public Pair<String, CtType> detectInvoker(CtExecutable invokerExe, CtInvocation ctInvocation){
        if (invokerExe==null){
            logger.error("Failed to find method for " +
                    ctInvocation.getOriginalSourceFragment().getSourcePosition().toString());
            return new ImmutablePair<>("$$FIELDCALL$$", ctInvocation.getParent(CtType.class));
        }
        else {
            if (invokerExe.getReference().isConstructor()){
                return new ImmutablePair<>(invokerExe.getSignature(), (CtType) invokerExe.getParent());
            }
            try {
                CtMethod ctMethod = (CtMethod) invokerExe;
            }
            catch (ClassCastException e){
                try {
                    CtAnonymousExecutable ctAnonymousExecutable = (CtAnonymousExecutable) invokerExe;
                    return new ImmutablePair<>("$$FIELDCALL$$", (CtType) ctAnonymousExecutable.getParent());
                }
                catch (ClassCastException e1){
                    try {
                        CtLambda ctLambda = (CtLambda) invokerExe;
                        return detectInvoker(ctLambda.getParent(CtExecutable.class), ctInvocation);
                    }
                    catch (ClassCastException e2){
                        logger.error("Failed to find parent for " +
                                ctInvocation.getOriginalSourceFragment().getSourcePosition().toString());
                        return new ImmutablePair<>("$$FIELDCALL$$", ctInvocation.getParent(CtType.class));
                    }
                }
            }
            return new ImmutablePair<>(invokerExe.getSignature(), (CtType) invokerExe.getParent());
        }
    }

    @Override
    public void process(CtInvocation ctInvocation) {
        CtExecutable invokerExe = ctInvocation.getParent(CtExecutable.class);
        CtExecutableReference invokedMethod = ctInvocation.getExecutable();
        CtTypeReference invoked = invokedMethod.getDeclaringType();
        Pair<String, CtType> pair = detectInvoker(invokerExe, ctInvocation);
        String invokerMethod = pair.getLeft();
        CtType invoker = pair.getRight();
        Invocation_.Builder invocation_ = Invocation_.newBuilder();
        invocation_.setAppName(this.getAppName());
        if (this.getServiceName()!=null)
            invocation_.setServiceName(this.getServiceName());
        if (invoked != null ){
//            logger.info("Object \"" + invoker.getQualifiedName() + "\" has used \"" + invokedMethod.getSignature() +
//                    "\" to invoke \"" + invoked.getQualifiedName() + "\"");
            successfulMatches++;
            invocation_.setInvokedMethod(invokedMethod.getSignature());
            invocation_.setInvokingObject(invoker.getQualifiedName());
            invocation_.setInvokingMethod(invokerMethod);
            invocation_.setInvokedObject(invoked.getQualifiedName());
            invocation_.setLocal(isLocal(invoked.getQualifiedName()));
            if (invokerMethod.equals("$$FIELDCALL$$")){
                Integer object_id = findObject(invoker.getQualifiedName());
                Class_.Builder object_ = objects.get(object_id).toBuilder();
                object_.addFieldCalls(invocation_.build());
                objects.set(object_id, object_.build());
                return;
            }
            else {
                Integer method_id = findMethod(invoker.getQualifiedName() + "::" + invokerMethod);
                if (method_id==null){
                    failedMatches++;
                    return;
                }
                Method_.Builder method_ = methods.get(method_id).toBuilder();
                if (invocation_.getLocal()){
                    method_.addLocalInvocations(invocation_.build());
                }
                else
                    method_.addInvocations(invocation_.build());
                methods.set(method_id, method_.build());
            }
        }
        else {
            failedMatches++;
            invocation_.setInvokedMethod(invokedMethod.getSignature());
            invocation_.setInvokingObject(invoker.getQualifiedName());
            invocation_.setInvokingMethod(invokerMethod);
            invocation_.setInvokedObject("$$UNKNOWN$$");
            invocation_.setLocal(false);
            failedMaps.add(invocation_.build());
        }
    }

    public List<Invocation_> getFailedMaps() {
        return failedMaps;
    }

    public void setFailedMaps(List<Invocation_> failedMaps) {
        this.failedMaps = failedMaps;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
