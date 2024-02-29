import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(name = "analyze", mixinStandardHelpOptions = true,
        description = "Statically analyzes a monolithic application through the client line interface.")
public class AnalysisCLI implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisCLI.class);
    @CommandLine.Parameters
    private String appName;
    @CommandLine.Option(
            names = {"-p", "--path"},
            description = "The path to source code of the application or link to the repository",
            required = false)
    private String appPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "The output path to save the results in",
            required = false)
    private String outputPath;

    @CommandLine.Option(
            names = {"-t", "--test"},
            description = "include test files")
    private boolean includeTest;

    @CommandLine.Option(
            names = {"-d", "--distributed"},
            description = "application has a distributed architecture.")
    private boolean isDistributed;
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
            if ((appPath == null)||(appPath.isEmpty())){
                logger.info("Loading source code from default path!");
                RepoHandler repoHandler = new RepoHandler(appName, "");
                try{
                    appPath = repoHandler.getOrClone();
                }
                catch(IOException e){
                    logger.info("Encountered error when loading the source code: \""+e.getMessage()+"\"!");
                    return;
                }
            }
            else {
                if (isURL(appPath)){
                    logger.info("Cloning source code from link!");
                    RepoHandler repoHandler = new RepoHandler(appName, appPath);
                    try{
                        appPath = repoHandler.getOrClone();
                    }
                    catch(IOException e){
                        logger.info("Encountered error when loading the source code: \""+e.getMessage()+"\"!");
                        return;
                    }
                }
                else
                    logger.info("Loading source code from the given path!");
            }
            try{
                dataLoader.analyze(appName, appPath, !includeTest, isDistributed);
            }
            catch(IOException e) {
                logger.info("Encountered error when analyzing the source code: \"" + e.getMessage() + "\"!");
            }
        }
        dataLoader.restoreDefaultOutputPath();
    }

    private Boolean isURL(String pathOrURL) {
        String regex = "^(?:http|ftp)s?://" +                  // http:// or https://
                "(?:(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\\.)+(?:[A-Z]{2,6}\\.?|[A-Z0-9-]{2,}\\.?)|" +  // domain...
                "localhost|" +                           // localhost...
                "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})" +  // ...or ip
                "(?::\\d+)?" +                          // optional port
                "(?:/?|[/?]\\S+)$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(pathOrURL);
        return matcher.find();
    }
}
