import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AnalysisServer {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServer.class);
    public static void main(String[] args) throws IOException, InterruptedException {
        logger.debug("Starting server!");
        logger.debug(AnalysisServer.class.getName());
        Server server = ServerBuilder
                .forPort(50100)
                .addService(new AnalyzerImp()).build();

        server.start();
        server.awaitTermination();
        logger.debug("Closing server!");
    }
}
