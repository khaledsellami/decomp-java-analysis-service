package refactor.processors;

import com.decomp.refactor.extension.MethodAPITypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import refactor.analysis.DTOAnalyzer;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodAPIProcessor extends AbstractProcessor<CtType<?>> {
    private Map<String, MethodAPITypes> apiTypesMap;
    private DTOAnalyzer analyzer;
    private Logger logger = LoggerFactory.getLogger(MethodAPIProcessor.class);

    public MethodAPIProcessor() {
        super();
        apiTypesMap = new HashMap<>();
        analyzer = new DTOAnalyzer();
    }

    public MethodAPIProcessor(DTOAnalyzer analyzer) {
        super();
        apiTypesMap = new HashMap<>();
        this.analyzer = analyzer;
    }

    public void parseApiTypes(CtExecutable<?> executable, String fullName){
        MethodAPITypes.Builder methodAPITypesBuilder = MethodAPITypes.newBuilder();
        methodAPITypesBuilder.setFullName(fullName);
        CtTypeReference<?> returnType = executable.getType();
        if (returnType != null) {
            methodAPITypesBuilder.setOutputType(analyzer.getFieldTypeWithGenerics(returnType));
        }
        for (CtParameter<?> parameter : executable.getParameters()) {
            CtTypeReference<?> parameterType = parameter.getType();
            methodAPITypesBuilder.addInputTypes(analyzer.getFieldTypeWithGenerics(parameterType));
        }
        apiTypesMap.put(fullName, methodAPITypesBuilder.build());
    }

    @Override
    public void process(CtType<?> ctType) {
        for (CtMethod<?> method:ctType.getAllMethods()){
            String fullName = ctType.getQualifiedName() + "::" + method.getSignature();
            parseApiTypes(method, fullName);
        }
        try {
            CtClass<?> ctClass = (CtClass<?>) ctType;
            for (CtConstructor<?> constructor:ctClass.getConstructors()){
                String fullName = ctType.getQualifiedName() + "::" + constructor.getSignature();
                parseApiTypes(constructor, fullName);
            }
        }
        catch (ClassCastException ignored){
            ;
        }
    }

    public Map<String, MethodAPITypes> getApiTypesMap() {
        return apiTypesMap;
    }

    public List<MethodAPITypes> getApiTypesMapValues() {
        return new ArrayList<>(apiTypesMap.values());
    }
}
