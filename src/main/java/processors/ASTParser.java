package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.Invocation_;
import com.decomp.analysis.Method_;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.OutputType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ASTParser {
    private String repoPath;

    private String appName;
    private boolean ignoreTest;
    private static Logger logger = LoggerFactory.getLogger(ASTParser.class);

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }


    public ASTParser(String repoPath, String appName, boolean ignoreTest) {
        this.repoPath = repoPath;
        this.appName = appName;
        this.ignoreTest = ignoreTest;
    }

    public static void find_src(String path, ArrayList<String> found, boolean ignoreTest){
        // TODO redo
        File file = new File(path);
//        Path srcPath = Path.of(path).resolve("main/java");
        Path srcPath = Paths.get(path).resolve("main/java");
        if (file.isDirectory()){
            if (((path.endsWith("src"))||(path.endsWith("src/")))&&(Files.exists(srcPath))){
                if (ignoreTest){
                    found.add(srcPath.toString());
                }
                else
                    found.add(path);

            }
            else {
                if ((path.endsWith("test"))||(path.endsWith("test/"))){
                    if (ignoreTest)
                        return;
                    if (path.endsWith("test/"))
                        found.add(path+"java");
                    else
                        found.add(path+"/java");
                }
                else {
                    try {
                        Files.list(file.toPath())
                                .forEach(p -> {
                                    find_src(p.toString(), found, ignoreTest);
                                });
                    }
                    catch (IOException e){
                        logger.info(e.toString());
                    }
                }
            }
        }
    }

    public Triple<List<Class_>, List<Method_>, List<Invocation_>> analyze() {
        logger.info("Starting analysis for project " + appName);
        logger.info("Creating Spoon Launcher");
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
        ArrayList<String> input_paths = new ArrayList<>();
        find_src(repoPath, input_paths, this.ignoreTest);
        //launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        for (String input_path : input_paths){
            logger.info("Adding PATH \"" + input_path + "\" as source");
            launcher.addInputResource(input_path);
        }
        logger.info("Creating type processor");
        ArrayList<Class_> objects = new ArrayList<Class_>();
        ArrayList<Method_> methods = new ArrayList<Method_>();
        TypeProcessor typeProcessor = new TypeProcessor(objects, methods, appName);
        launcher.addProcessor(typeProcessor);
        logger.info("Creating invocation processor");
        InvocationProcessor invocationProcessor = new InvocationProcessor(objects, methods, appName);
        launcher.addProcessor(invocationProcessor);
        logger.info("Starting process");
        launcher.run();
        logger.info("Process finished successfully");
        logger.info("Detected " + typeProcessor.getObjects().size() + " classes and interfaces");
        logger.info("Detected " + typeProcessor.getMethods().size() + " methods");
        logger.info("Found " + invocationProcessor.successfulMatches + " successful matches and " +
                invocationProcessor.failedMatches + " failed matches");
        return new ImmutableTriple<>(
                typeProcessor.getObjects(), typeProcessor.getMethods(), invocationProcessor.getFailedMaps());

    }

    public boolean getIgnoreTest() {
        return ignoreTest;
    }

    public void setIgnoreTest(boolean ignoreTest) {
        this.ignoreTest = ignoreTest;
    }
}
