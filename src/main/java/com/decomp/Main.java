package com.decomp;

import picocli.CommandLine;


@CommandLine.Command(name = "st_analyzer", synopsisSubcommandLabel = "COMMAND",
        subcommands = {AnalysisCLI.class, AnalysisServer.class},
        mixinStandardHelpOptions = true, version = "1.3.0",
        description = "Statically analyzes a monolithic application.")
public class Main implements Runnable {

    public void run() {
        int exitCode = new CommandLine(new AnalysisServer()).execute();
        System.exit(exitCode);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
