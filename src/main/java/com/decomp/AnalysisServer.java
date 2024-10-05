package com.decomp;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "start", mixinStandardHelpOptions = true, description = "Start the analysis server.")
public class AnalysisServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServer.class);
    public void run() {
        int portNumber = 50100;
        String envVar = System.getenv("SERVICE_JAVA_ANALYSIS_PORT");
        if (envVar!=null){
            portNumber = Integer.parseInt(envVar);
        }
        logger.info("Starting " + AnalysisServer.class.getName() + " on port " + portNumber + "!");
        Server server = ServerBuilder.forPort(portNumber).addService(new AnalyzerImp()).build();
        try {
            server.start();
        }
        catch (IOException e){
            logger.info("Failed to start server due to error: \"" + e.getMessage() + "\"");
        }
        try {
            server.awaitTermination();
        }
        catch (InterruptedException e2){
            ;
        }
        logger.info("Closing server!");
    }
}
