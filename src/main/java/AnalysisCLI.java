import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@CommandLine.Command(name = "analyze", mixinStandardHelpOptions = true,
        description = "Statically analyzes a monolithic application through the client line interface.")
public class AnalysisCLI implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisCLI.class);
    @CommandLine.Parameters
    private String appName;
    @CommandLine.Option(
            names = {"-p", "--path"},
            description = "The path to source code of the application",
            required = false)
    private String appPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "The output path to save the results in",
            required = false)
    private String outputPath;
    private final DataLoader dataLoader;
//    private final List<String> ALLOWED_APPS = Arrays.asList("petclinic", "plants");

    public AnalysisCLI() {
        this.dataLoader = new DataLoader();
    }
    @Override
    public void run() {
//        if (!ALLOWED_APPS.contains(appName)){
//            logger.info("Unauthorized  application '"+ appName +
//                    "'. Please provide a choice among [" + ALLOWED_APPS + "]");
//            return;
//        }
        logger.info("Analyzing "+appName+"!");
        if (outputPath != null){
            dataLoader.setOutputPath(outputPath);
        }
        if (!dataLoader.exists(appName)){
            if (appPath == null){
                RepoHandler repoHandler = new RepoHandler(appName, "");
                try{
                    appPath = repoHandler.getOrClone();
                }
                catch(IOException e){
                    logger.info("Encountered error when loading the source code: \""+e.getMessage()+"\"!");
                    return;
                }
            }
            try{
                dataLoader.analyze(appName, appPath);
            }
            catch(IOException e) {
                logger.info("Encountered error when analyzing the source code: \"" + e.getMessage() + "\"!");
            }
        }
        dataLoader.restoreDefaultOutputPath();
    }
}
