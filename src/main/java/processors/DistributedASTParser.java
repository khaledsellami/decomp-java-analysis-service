package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.Method_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import refactor.processors.DTOProcessor;
import spoon.Launcher;
import spoon.OutputType;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistributedASTParser extends ASTParser{
    private String repoPath;

    private String appName;
    private boolean ignoreTest;
    private ArrayList<String> serviceNames;
    private static Logger logger = LoggerFactory.getLogger(DistributedASTParser.class);


    public DistributedASTParser(String repoPath, String appName, boolean ignoreTest) {
        super(repoPath, appName, ignoreTest);
        this.repoPath = repoPath;
        this.appName = appName;
        this.ignoreTest = ignoreTest;
        serviceNames = new ArrayList<>();
    }

    public String findServiceName(String input_path, int it){
        Pattern pattern = Pattern.compile(".*/(.*)/(src/main/java|src|src/test)/?");
        Matcher matcher = pattern.matcher(input_path);
        String serviceName = "NO_NAME_FOUND_" + it;
        if (matcher.find())
        {
            serviceName = matcher.group(1);
        }
        if (serviceNames.contains(serviceName)){
            final String sname = serviceName;
            serviceName = serviceName + "_" + serviceNames.stream().filter(p -> p.startsWith(sname)).count();
        }
        return serviceName;
    }

    public ProcessedContainers analyze_one(String input_path, int it){
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
        String serviceName = findServiceName(input_path, it);
        logger.info("Working on microservice \"" + serviceName + "\" in '" + input_path + "'");
        serviceNames.add(serviceName);
        launcher.addInputResource(input_path);

        ArrayList<Class_> objects = new ArrayList<Class_>();
        ArrayList<Method_> methods = new ArrayList<Method_>();
        TypeProcessor typeProcessor = new TypeProcessor(objects, methods, appName, serviceName);
        launcher.addProcessor(typeProcessor);
        InvocationProcessor invocationProcessor = new InvocationProcessor(objects, methods, appName, serviceName);
        launcher.addProcessor(invocationProcessor);
        DTOProcessor dtoProcessor = new DTOProcessor();
        launcher.addProcessor(dtoProcessor);
        launcher.run();
        ProcessedContainers output = new ProcessedContainers(typeProcessor.getObjects(), typeProcessor.getMethods(),
                invocationProcessor.getFailedMaps(), new ArrayList<>(), dtoProcessor.getDTOs());
        return output;
    }

    public ProcessedContainers analyze() {
        logger.info("Starting analysis for distributed project " + appName + " in path " + repoPath);
        ArrayList<String> input_paths = new ArrayList<>();
        find_src(repoPath, input_paths, this.ignoreTest);
//        ArrayList<Class_> allObjects = new ArrayList<>();
//        ArrayList<Method_> allMethods = new ArrayList<>();
//        ArrayList<Invocation_> allInvocations = new ArrayList<>();
        ProcessedContainers output = new ProcessedContainers();
        serviceNames = new ArrayList<>();
        int it = 0;
        for (String input_path : input_paths){
            ProcessedContainers analysisResults = analyze_one(input_path, it);
            output.classes.addAll(analysisResults.classes);
            output.methods.addAll(analysisResults.methods);
            output.invocations.addAll(analysisResults.invocations);
            output.imports.addAll(analysisResults.imports);
            it++;
        }
        logger.info("Process finished successfully");
        logger.info("Detected " + output.classes.size() + " classes and interfaces");
        logger.info("Detected " + output.methods.size() + " methods");
        logger.info("Found " + output.invocations.size() + " failed matches");
        logger.info("Detected " + output.imports.size() + " imports");
        return output;
    }
}
