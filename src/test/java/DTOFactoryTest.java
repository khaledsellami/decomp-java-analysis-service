import com.decomp.analysis.Class_;
import com.decomp.analysis.Method_;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processors.ImportProcessor;
import processors.InvocationProcessor;
import processors.ProcessedContainers;
import processors.TypeProcessor;
import refactor.processors.DTOProcessor;
import refactor.processors.RefactTypeProcessor;
import spoon.Launcher;
import spoon.OutputType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import refactor.analysis.AnalysisContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import refactor.transformation.dto.DTOFactory;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.support.sniper.SniperJavaPrettyPrinter;

public class DTOFactoryTest extends ValidateSpring {

    private Launcher launcher;
    private Factory factory;
    private AnalysisContext context;
    private DTOFactory dtoFactory;

//    @BeforeEach
    public void setUp(String input_path, String appName) {
        launcher = new Launcher();
//        launcher.getEnvironment().setPrettyPrinterCreator(() -> {
//                    return new SniperJavaPrettyPrinter(launcher.getEnvironment());
//                }
//        );
//        launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
//        String input_path = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/example-1";
//        String appName = "example-1";
        launcher.addInputResource(input_path);
        ArrayList<Class_> objects = new ArrayList<Class_>();
        ArrayList<Method_> methods = new ArrayList<Method_>();
        RefactTypeProcessor typeProcessor = new RefactTypeProcessor(objects, methods, appName);
        launcher.addProcessor(typeProcessor);
        InvocationProcessor invocationProcessor = new InvocationProcessor(objects, methods, appName);
        launcher.addProcessor(invocationProcessor);
        ImportProcessor importProcessor = new ImportProcessor();
        launcher.addProcessor(importProcessor);
        DTOProcessor dtoProcessor = new DTOProcessor();
        launcher.addProcessor(dtoProcessor);
        launcher.run();
        factory = launcher.getFactory();
        context = new AnalysisContext(objects, typeProcessor.getTypes(), dtoProcessor.getDTOs());
        dtoFactory = new DTOFactory(factory, context);
    }

    @Test
    public void testGenerateDto() {
        List<String> names = List.of("example.original.Person", "example.original.Address");
        String packageName = "example.refactored";
        String outputPath = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/example-2";
        String input_path = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/example-1";
        setUp(input_path, "example-1");

        Map<String, Map.Entry<CtClass<?>, CtInterface<?>>> results = new HashMap<>();
        for (String name : names) {
            Map.Entry<CtClass<?>, CtInterface<?>> result = dtoFactory.generateDto(name, packageName);
            results.put(name, result);
        }
        List<String> newClasses = new ArrayList<>();
        for (String name : names) {
            CtType<?> dtoClass = results.get(name).getKey();
            newClasses.add(dtoClass.getQualifiedName());
            CtInterface<?> mapper = results.get(name).getValue();
            newClasses.add(mapper.getQualifiedName());
        }
        launcher.setOutputFilter(newClasses.toArray(String[]::new));
        launcher.setSourceOutputDirectory(outputPath);
        launcher.getEnvironment().setAutoImports(true);
        launcher.prettyprint();

        // check if the files were created
        for (String name : names) {
            CtType<?> dtoClass = results.get(name).getKey();
            String dtoName = dtoClass.getQualifiedName();
            File file = new File(outputPath + "/" + dtoName.replace(".", "/") + ".java");
            assertTrue(file.exists());
            CtInterface<?> mapper = results.get(name).getValue();
            String mapperName = mapper.getQualifiedName();
            file = new File(outputPath + "/" + mapperName.replace(".", "/") + ".java");
            assertTrue(file.exists());
        }
    }

    @Test
    public void testRPCsimple() {
        List<String> names = List.of("com.example.server.Person", "com.example.server.Address");
        String input_path = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/client-server-simple";
        String packageName = "com.example.server";
//        String outputPath = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/client-server-2";
        String appName = input_path.substring(input_path.lastIndexOf("/") + 1);
        String outputPath = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-generated/" + appName;
        String serverPath = outputPath + "/server";
        String clientPath = outputPath + "/client";

        // generate DTOs for the server and client applications
        generateDTOsForSpring(input_path, outputPath, packageName, names, appName, false);

        // compile, run and test the server applications
        assertTrue(checkTestSuccess(serverPath, clientPath, "http://localhost:8080", "http://localhost:8081"));
    }

    @Test
    public void testRPCrecursive() {
        List<String> names = List.of("com.example.server.Person", "com.example.server.Address");
        String input_path = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/client-server-recursive";
        String packageName = "com.example.server";
//        String outputPath = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/client-server-2";
        String appName = input_path.substring(input_path.lastIndexOf("/") + 1);
        String outputPath = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-generated/" + appName;
        String serverPath = outputPath + "/server";
        String clientPath = outputPath + "/client";

        // generate DTOs for the server and client applications
        generateDTOsForSpring(input_path, outputPath, packageName, names, appName, false);

        // compile, run and test the server applications
        assertTrue(checkTestSuccess(serverPath, clientPath, "http://localhost:8080", "http://localhost:8081"));
    }

    @Test
    public void testRPCbinary() {
        List<String> names = List.of("com.example.server.Person", "com.example.server.Address");
        String input_path = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-repositories/client-server-binary";
        String packageName = "com.example.server";
        String appName = input_path.substring(input_path.lastIndexOf("/") + 1);
        String outputPath = "/Users/khalsel/Documents/projects/decompPlatform/java-analysis-service/data-generated/" + appName;
        String serverPath = outputPath + "/server";
        String clientPath = outputPath + "/client";

        // generate DTOs for the server and client applications
        generateDTOsForSpring(input_path, outputPath, packageName, names, appName, true);

        // compile, run and test the server applications
        assertTrue(checkTestSuccess(serverPath, clientPath, "http://localhost:8080", "http://localhost:8081"));
    }

    private void generateDTOsForSpring(String input_path, String outputPath, String packageName, List<String> names, String appName, boolean includeInClient) {
        String serverOutputPath = outputPath + "/server/src/main/java";
        String clientOutputPath = outputPath + "/client/src/main/java";
        String inputs_path = input_path + "/inputs";
        copyProject(input_path, outputPath);
        setUp(inputs_path, appName);
        // generate DTOs and mappers
        Map<String, Map.Entry<CtClass<?>, CtInterface<?>>> results = new HashMap<>();
        for (String name : names) {
            Map.Entry<CtClass<?>, CtInterface<?>> result = dtoFactory.generateDto(name, packageName);
            results.put(name, result);
        }
        // update the package name of the original classes
        for (String name : names) {
            CtType<?> ctType = context.getCtType(name);
            updateTypePackage(factory, ctType, "com.example.server");
        }
        // add the DTOs and mappers to the launcher filter
        List<String> serverClasses = new ArrayList<>();
        for (String name : names) {
            CtType<?> dtoClass = results.get(name).getKey();
            serverClasses.add(dtoClass.getQualifiedName());
            CtInterface<?> mapper = results.get(name).getValue();
            serverClasses.add(mapper.getQualifiedName());
        }
        // generate the server application dtos and mappers
        launcher.setOutputFilter(serverClasses.toArray(String[]::new));
        launcher.setSourceOutputDirectory(serverOutputPath);
        launcher.getEnvironment().setAutoImports(true);
        launcher.prettyprint();

        // generate the client application
        // update the package name of the original classes
        for (String name : names) {
            CtType<?> ctType = context.getCtType(name);
            updateTypePackage(factory, ctType, "com.example.client");
        }
        // update the package name of the DTOs and mappers
        List<String> clientClasses = new ArrayList<>();
        for (String name : names) {
            CtType<?> dtoClass = results.get(name).getKey();
            updateTypePackage(factory, dtoClass, "com.example.client.dto");
            clientClasses.add(dtoClass.getQualifiedName());
            if (includeInClient) {
                CtInterface<?> mapper = results.get(name).getValue();
                updateTypePackage(factory, mapper, "com.example.client.mapper");
                clientClasses.add(mapper.getQualifiedName());
            }
        }
        launcher.setOutputFilter();
        launcher.setOutputFilter(clientClasses.toArray(String[]::new));
        launcher.setSourceOutputDirectory(clientOutputPath);
        launcher.getEnvironment().setAutoImports(true);
        launcher.prettyprint();
    }
    public static void updateTypePackage(Factory factory, CtType<?> type, String newPackageName) {
        // Store the old package reference
        CtPackage oldPackage = type.getPackage();
        String oldQualifiedName = type.getQualifiedName();

        // Create and set up the new package
        CtPackage newPackage = factory.Package().getOrCreate(newPackageName);
//        CtPackage newPackage = factory.createPackage();
//        newPackage.setSimpleName(newPackageName);

        // Update the type's package
        type.setParent(newPackage);
        newPackage.addType(type);

        // Add the new package to the factory's root package
//        factory.getModel().getRootPackage().addPackage(newPackage);

        // Remove the type from its old package if it existed
        if (oldPackage != null) {
            oldPackage.removeType(type);
        }

        // Update all references to this type in the model
        factory.getModel().getRootPackage().accept(new CtScanner() {
            @Override
            public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
                if (reference.getQualifiedName().equals(oldQualifiedName)) {
                    reference.setPackage(factory.Package().createReference(newPackage));
                }
                super.visitCtTypeReference(reference);
            }
        });
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    public static void copyProject(String inputPath, String outputPath) {
        File input = new File(inputPath);
        File output = new File(outputPath);
        // if exists, delete the output file. If it's a directory, delete the directory even if it's not empty
        if (output.exists()) {
            if (output.isDirectory()) {
                deleteDir(output);
            }
            output.delete();
        }
        if (input.isDirectory()) {
            if (!output.exists()) {
                output.mkdirs();
            }
            for (File file : input.listFiles()) {
                copyProject(file.getAbsolutePath(), outputPath + "/" + file.getName());
            }
        } else {
            try {
                Files.copy(input.toPath(), output.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

// Issues:
// - byte[] field is written as byte => Done
// - the types are written with their qualified names => Done
// - the input to getMapper is not correct => Done
// - the uses value in the Mapper annotation is not correct => Done
// - Some imports need to be added => Done
// - some statements have double semicolons => Done
// - there's a new line after To => Done

// - the Java Spring boot application fails to map the data