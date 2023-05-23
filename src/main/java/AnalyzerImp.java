import com.decomp.analysis.*;
import io.grpc.stub.StreamObserver;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

public class AnalyzerImp extends AnalyzerGrpc.AnalyzerImplBase {
    private DataLoader dataLoader;

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
        if (!dataLoader.exists(appName)){
            RepoHandler repoHandler = new RepoHandler(appName, appRepo);
            String appPath = repoHandler.getOrClone();
            dataLoader.analyze(appName, appPath);
        }
    }

}
