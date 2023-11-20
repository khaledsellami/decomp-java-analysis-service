import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AnalysisServer {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServer.class);
    public static void main(String[] args) throws IOException, InterruptedException {
        int portNumber = 50100;
        logger.info("Starting " + AnalysisServer.class.getName() + " on port " + portNumber + "!");
        Server server = ServerBuilder.forPort(portNumber).addService(new AnalyzerImp()).build();

        server.start();
        server.awaitTermination();
        logger.info("Closing server!");
    }
}
