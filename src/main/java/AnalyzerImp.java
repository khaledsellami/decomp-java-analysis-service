import com.decomp.analysis.*;
import io.grpc.stub.StreamObserver;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;
import java.util.List;

public class AnalyzerImp extends AnalyzerGrpc.AnalyzerImplBase {
    private DataLoader dataLoader;
    private static final Logger logger = LoggerFactory.getLogger(AnalyzerImp.class);

    public AnalyzerImp() {
        this.dataLoader = new DataLoader();
    }

    @Override
    public void initRepo(AstRequest request, StreamObserver<AstReply> responseObserver) {
        AstReply reply;
        try{
            loadApp(request);
            reply = AstReply.newBuilder().setMessage("Loading Successful").build();
        } catch (IOException e) {
            reply = AstReply.newBuilder().setMessage("Loading Failed").build();
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getClasses(AstRequest request, StreamObserver<Class_> responseObserver) {
        try{
            loadApp(request);
            List<Class_> classes = dataLoader.getClasses(request.getAppName());
            for (Class_ class_: classes){
                responseObserver.onNext(class_);
            }
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getMethods(AstRequest request, StreamObserver<Method_> responseObserver) {
        try{
            loadApp(request);
            List<Method_> methods = dataLoader.getMethods(request.getAppName());
            for (Method_ method_: methods){
                responseObserver.onNext(method_);
            }
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getInvocations(AstRequest request, StreamObserver<Invocation_> responseObserver) {
        try{
            loadApp(request);
            List<Invocation_> invocations = dataLoader.getInvocations(request.getAppName());
            for (Invocation_ invocation_: invocations){
                responseObserver.onNext(invocation_);
            }
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    private void loadApp(AstRequest request) throws IOException {
        String appName = request.getAppName();
        String appRepo = request.getAppRepo();
        boolean isDistributed = false;
        if (request.hasIsDistributed())
            isDistributed = request.getIsDistributed();
        boolean includeTest = false;
        if (request.hasIncludeTest())
            includeTest = request.getIncludeTest();
        String envVar = System.getenv("INCLUDE_TEST");
        includeTest = (includeTest|((envVar!=null)&&(envVar.equals("true"))));
        if (!dataLoader.exists(appName)){
            String appPath;
            if (isURL(appRepo)|appRepo.isEmpty()) {
                logger.debug("Using the link '" + appRepo + "' to clone the repository.");
                RepoHandler repoHandler = new RepoHandler(appName, appRepo);
                appPath = repoHandler.getOrClone();
            }
            else {
                logger.debug("Using the path '" + appRepo + "' to analyze the repository.");
                appPath = appRepo;
            }
            dataLoader.analyze(appName, appPath, !includeTest, isDistributed);
        }
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
