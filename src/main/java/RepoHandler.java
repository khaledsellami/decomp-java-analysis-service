import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class RepoHandler {
    private String appName;
    private String appRepo;

    private static final Logger logger = LoggerFactory.getLogger(RepoHandler.class);
    private static final String dataPath = "./data/repositories/";
    private static final String tempPath = "./data/temp/";

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppRepo() {
        return appRepo;
    }

    public void setAppRepo(String appRepo) {
        this.appRepo = appRepo;
    }


    public RepoHandler(String appName, String appRepo) {
        this.appName = appName;
        this.appRepo = appRepo;
    }

    public String getOrClone() throws IOException {
        String repoPath = Paths.get(dataPath, appName).toString();
        File f = new File(repoPath);
        if (f.exists() && f.isDirectory()){
            logger.debug("Source code for " + appName + " found locally!");
            return repoPath;
        }
        repoPath = Paths.get(tempPath, appName).toString();
        f = new File(repoPath);
        if (f.exists() && f.isDirectory()){
            logger.debug("Source code for " + appName + " found in temp folder!");
            return repoPath;
        }
        try {
            logger.debug("Cloning source code for " + appName + " !");
            Git.cloneRepository()
                    .setURI(appRepo)
                    .setDirectory(new File(repoPath))
                    .call();
        }
        catch (GitAPIException e){
            throw new IOException("Git clone failed!");
        }
        return repoPath;
    }
}
