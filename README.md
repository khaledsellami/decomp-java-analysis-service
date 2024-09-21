# Decomp-Java-Analysis-Service

Decomp-Java-Analysis-Service is an AST-parser for generating metadata about the classes and methods declared in a given Java source code for the purpose of monolith to microservice decomposition.

## Description

Given a Java source code, the Decomp-Java-Analysis-Service generates the Abstract Syntax Tree (AST) of the source code and leverages this structure to extract the metadata about the classes and methods and their interactions. The metadata includes the following information:
- Type Data: Information about the classes and interfaces declared in the source code (for example: full name, methods, referenced types, corresponding source code, etc.).
- Executable Data: Information about the methods declared in the source code (for example: full name, parent class, return type, parameters, corresponding source code, etc.).
- Invocation Data: Information about the invocations between the methods declared in the source code (for example: caller class, caller method, invoked class, invoked method).

Detailed information about the metadata can be found in the `src/main/java/resources/analyze.proto` file.

This tool was built on top of the [Spoon](https://spoon.gforge.inria.fr/) source code analysis library. 

The Decomp-Java-Analysis-Service is a part of a research project that aims to provide a standarized framework for analyzing monolithic applications, decomposing them into microservices, visualizing the decompositions, evaluating the decompositions and refactoring the monolith.


## Getting Started

### Dependencies

This module requires the following dependencies:
* Java 11 or higher
* Maven 3.9.7 or higher


### Installing

First, clone the repository to your local machine:
```
 git clone https://github.com/khaledsellami/decomp-java-analysis-service.git
```
Then, build the project using Maven:
```
cd ./decomp-java-analysis-service
mvn clean compile assembly:single
```
This command will generate a Jar file in the `target` directory named `DecompAnalysis.jar` which packages the project and its dependencies.

### Executing program

The are two ways to interact with the analysis service:

#### 1. Using the Command Line Interface

You can use the CLI of the analysis service to generate the static analysis of a given Java source code:
```shell
java -jar target/DecompAnalysis.jar analyze your_app_name \
      -p /path/or/github/link/to/source/code \
      -o /path/to/output
```
The program will save the generated results in "/path/to/output/your_app_name".

For example, you can analyze the `decomp-java-analysis-service` project itself by running the following command:
```shell
java -jar target/DecompAnalysis.jar analyze decomp_analysis \
      -p https://github.com/khaledsellami/decomp-java-analysis-service.git
```

#### 2. Using the analysis gRPC server

You can run the gRPC server that will serve the analysis service.
```
java -jar target/DecompAnalysis.jar start
# or simply
java -jar target/DecompAnalysis.jar
```
Afterward, you can build your own gRPC client with the programming language of your choice to interact with the analysis service through port 50100. The gRPC server interface is defined in the `src/main/java/resources/analyze.proto` file.


### Help

If you wish to get more information about the available options in the CLI script, you can run the following command:
```
java -jar target/DecompAnalysis.jar analyze --help
```

In addition, you can get more information about the data exposed by the parser service by inspecting the `src/main/java/resources/analyze.proto` file.

## Roadmap
* Improve the documentation of this module
* Add support for Git repository managers (GitLab, Bitbucket, etc.)

## Authors

Khaled Sellami - [khaledsellami](https://github.com/khaledsellami) - khaled.sellami.1@ulaval.ca

## Version History

* 1.3.1
    * Initial Documented Release

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.