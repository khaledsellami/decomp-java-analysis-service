import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class AnalysisServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder
                .forPort(50100)
                .addService(new AnalyzerImp()).build();

        server.start();
        server.awaitTermination();
    }
}
