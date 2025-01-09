import com.decomp.analysis.*;
import com.decomp.refactor.ClassDTO;
import com.decomp.refactor.ClassDTOContainer;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processors.ASTParser;
import processors.DistributedASTParser;
import processors.ProcessedContainers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class DataLoader {

    // TODO change to a more suitable storage approach (mongodb for example)
    private static final String defaultOutputPath = "./data/static_analysis/";
    private String outputPath = "./data/static_analysis/";
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private static final String classFileName = "typeData.json";
    private static final String methodFileName = "methodData.json";
    private static final String invocationFileName = "invocationData.json";
    private static final String importFileName = "importData.json";
    private static final String DTOFileName = "dtoData.json";

    private void saveRefact(List<ClassDTO> DTOs, String appName) throws IOException {
        logger.info("Converting import DTO data to JSON");
        ClassDTOContainer.Builder dtoContainer = ClassDTOContainer.newBuilder().addAllDtos(DTOs);
        String jsonDTOs = JsonFormat.printer().includingDefaultValueFields().print(dtoContainer);
        try {
            String savePath = Paths.get(outputPath, appName, DTOFileName).toString();
            logger.info("Saving DTO data in " + savePath);
            File file = new File(savePath);
            file.getParentFile().mkdirs();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            out.println(jsonDTOs);
            out.close();
        }
        catch (IOException e){
            logger.error("Failed to save JSON data");
            throw e;
        }
    }

    private void save(List<Class_> classes, List<Method_> methods, List<Invocation_> invocations, List<Import_> imports,
                      String appName) throws IOException{
        logger.info("Converting class data to JSON");
        ClassContainer.Builder classContainer = ClassContainer.newBuilder().addAllClasses(classes);
        String jsonClasses = JsonFormat.printer().includingDefaultValueFields().print(classContainer);
//        StringBuilder jsonClassesBuilder = new StringBuilder("[\n");
//        int i = 0;
//        for (Class_ class_: classes){
//            if (i!=0)
//                jsonClassesBuilder.append(",\n");
//            jsonClassesBuilder.append(JsonFormat.printer().print(class_.toBuilder()));
//            i++;
//        }
//        jsonClassesBuilder.append(']');
//        String jsonClasses = jsonClassesBuilder.toString();
        logger.info("Converting method data to JSON");
        MethodContainer.Builder methodContainer = MethodContainer.newBuilder().addAllMethods(methods);
        String jsonMethods = JsonFormat.printer().includingDefaultValueFields().print(methodContainer);
//        StringBuilder jsonMethodsBuilder = new StringBuilder("[\n");
//        i = 0;
//        for (Method_ method_: methods){
//            if (i!=0)
//                jsonMethodsBuilder.append(",\n");
//            jsonMethodsBuilder.append(JsonFormat.printer().print(method_.toBuilder()));
//            i++;
//        }
//        jsonMethodsBuilder.append(']');
//        String jsonMethods = jsonMethodsBuilder.toString();
        logger.info("Converting failed invocation match data to JSON");
        InvocationContainer.Builder invocationContainer = InvocationContainer.newBuilder().addAllInvocations(
                invocations);
        String jsonInvocations = JsonFormat.printer().includingDefaultValueFields().print(invocationContainer);
//        StringBuilder jsonInvocationsBuilder = new StringBuilder("[\n");
//        i = 0;
//        for (Invocation_ invocation_: invocations){
//            if (i!=0)
//                jsonInvocationsBuilder.append(",\n");
//            jsonInvocationsBuilder.append(JsonFormat.printer().print(invocation_.toBuilder()));
//            i++;
//        }
//        jsonInvocationsBuilder.append(']');
//        String jsonInvocations = jsonInvocationsBuilder.toString();
        logger.info("Converting import data to JSON");
        ImportContainer.Builder importContainer = ImportContainer.newBuilder().addAllImports(imports);
        String jsonImports = JsonFormat.printer().includingDefaultValueFields().print(importContainer);
        try {
            String savePath = Paths.get(outputPath, appName, classFileName).toString();
            logger.info("Saving type data in " + savePath);
            File file = new File(savePath);
            file.getParentFile().mkdirs();
            file.createNewFile();
            PrintWriter out = new PrintWriter(file);
            out.println(jsonClasses);
            out.close();
            savePath = Paths.get(outputPath, appName, methodFileName).toString();
            logger.info("Saving method data in " + savePath);
            file = new File(savePath);
            file.getParentFile().mkdirs();
            file.createNewFile();
            out = new PrintWriter(file);
            out.println(jsonMethods);
            out.close();
            savePath = Paths.get(outputPath, appName, invocationFileName).toString();
            logger.info("Saving invocation data in " + savePath);
            file = new File(savePath);
            file.getParentFile().mkdirs();
            file.createNewFile();
            out = new PrintWriter(file);
            out.println(jsonInvocations);
            out.close();
            savePath = Paths.get(outputPath, appName, importFileName).toString();
            logger.info("Saving import data in " + savePath);
            file = new File(savePath);
            file.getParentFile().mkdirs();
            file.createNewFile();
            out = new PrintWriter(file);
            out.println(jsonImports);
            out.close();
        }
        catch (IOException e){
            logger.error("Failed to save JSON data");
            throw e;
        }
    }


    public boolean exists(String appName){
        boolean itExists = true;
        List<String> files = Arrays.asList(classFileName, methodFileName, invocationFileName);
        int i = 0;
        while (itExists&&(i<files.size())){
            String fileName = files.get(i);
            String savePath = Paths.get(outputPath, appName, fileName).toString();
            File file = new File(savePath);
            itExists = itExists&&file.exists();
            i++;
        }
        return itExists;
    }

    public boolean analyze(String appName, String appPath) throws IOException
    {
        return analyze(appName, appPath, true, false);
    }

    public boolean analyze(String appName, String appPath, boolean ignoreTest) throws IOException
    {
        return analyze(appName, appPath, ignoreTest, false);
    }

    public boolean analyze(String appName, String appPath, boolean ignoreTest, boolean isDistributed) throws IOException
    {
        if (exists(appName)){
            logger.info("Application " + appName + " exists! Exiting process.");
            return false;
        }
        logger.info("Application " + appName + " not found! Starting analysis.");
        ASTParser astParser;
        if (isDistributed)
            astParser = new DistributedASTParser(appPath, appName, ignoreTest);
        else
            astParser = new ASTParser(appPath, appName, ignoreTest);
        ProcessedContainers analysisResults = astParser.analyze();
//        List<Class_> classes = analysisResults.getLeft();
//        List<Method_> methods = analysisResults.getMiddle();
//        List<Invocation_> invocations = analysisResults.getRight();
        List<Class_> classes = analysisResults.getClasses();
        List<Method_> methods = analysisResults.getMethods();
        List<Invocation_> invocations = analysisResults.getInvocations();
        List<Import_> imports = analysisResults.getImports();
        List<ClassDTO> DTOs = analysisResults.getDTOs();
        logger.info("Saving data for Application " + appName + " !");
        save(classes, methods, invocations, imports, appName);
        saveRefact(DTOs, appName);
        return true;
    }

    public List<Class_> getClasses(String appName) throws IOException {
        if (!exists(appName))
            return null;
        logger.info("Loading class data for Application " + appName + " !");
        String savePath = Paths.get(outputPath, appName, classFileName).toString();
        String dataString = FileUtils.readFileToString(new File(savePath), StandardCharsets.UTF_8);
        ClassContainer.Builder classContainer = ClassContainer.newBuilder();
        JsonFormat.parser().merge(dataString, classContainer);
        return classContainer.getClassesList();
    }

    public List<Method_> getMethods(String appName) throws IOException {
        if (!exists(appName))
            return null;
        logger.info("Loading method data for Application " + appName + " !");
        String savePath = Paths.get(outputPath, appName, methodFileName).toString();
        String dataString = FileUtils.readFileToString(new File(savePath), StandardCharsets.UTF_8);
        MethodContainer.Builder methodContainer = MethodContainer.newBuilder();
        JsonFormat.parser().merge(dataString, methodContainer);
        return methodContainer.getMethodsList();
    }

    public List<Invocation_> getInvocations(String appName) throws IOException {
        if (!exists(appName))
            return null;
        logger.info("Loading invocation data for Application " + appName + " !");
        String savePath = Paths.get(outputPath, appName, invocationFileName).toString();
        String dataString = FileUtils.readFileToString(new File(savePath), StandardCharsets.UTF_8);
        InvocationContainer.Builder invocationContainer = InvocationContainer.newBuilder();
        JsonFormat.parser().merge(dataString, invocationContainer);
        return invocationContainer.getInvocationsList();
    }
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public void restoreDefaultOutputPath(){
        this.outputPath = defaultOutputPath;
    }
}
