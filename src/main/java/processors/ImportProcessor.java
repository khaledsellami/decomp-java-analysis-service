package processors;

import com.decomp.analysis.CodeSpan;
import com.decomp.analysis.ImportKind;
import com.decomp.analysis.ImportUsage;
import com.decomp.analysis.Import_;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.groups.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static processors.Utils.buildCodeSpan;

public class ImportProcessor extends AbstractProcessor<CtType> {
    private Map<String, Set<Import_>> imports;
    private Logger logger = LoggerFactory.getLogger(TypeProcessor.class);

    public ImportProcessor() {
        super();
        imports = new HashMap<>();
//        System.out.println("ImportProcessor created");
    }

    public Map<String, Set<Import_>> getImports() {
        return imports;
    }

    public List<Import_> getImportsAsList() {
        List<Import_> importList = new ArrayList<>();
        for (Set<Import_> importSet : imports.values()) {
            importList.addAll(importSet);
        }
        return importList;
    }

    public Pair<ImportKind, Triple<String, String, String>> resolveImport(String importStatement) {
        ImportKind importKind;
        String importedItem;
        String packageName = "";
        String className = "";
        String memberName = "";
        Pattern staticImportPattern = Pattern.compile("^import\\s+static\\s+([\\w\\.]+)\\.([\\w]+)(?:\\.\\*)?;$");
        Pattern singleTypeImportPattern = Pattern.compile("^import\\s+([\\w\\.]+)(?:\\.\\*)?;$");
        Matcher staticMatcher = staticImportPattern.matcher(importStatement);
        Matcher singleTypeMatcher = singleTypeImportPattern.matcher(importStatement);
        if (staticMatcher.matches()) {
            importKind = importStatement.contains(".*") ? ImportKind.ALL_STATIC_MEMBERS : ImportKind.FIELD_OR_METHOD;
            switch (importKind) {
                case ALL_STATIC_MEMBERS:
                    packageName = staticMatcher.group(1);
                    className = staticMatcher.group(2);
                    importedItem = staticMatcher.group(1) + "." + className;
                    break;
                case FIELD_OR_METHOD:
                    packageName = staticMatcher.group(1).substring(0, staticMatcher.group(1).lastIndexOf('.'));
                    className = staticMatcher.group(1).substring(staticMatcher.group(1).lastIndexOf('.') + 1);
                    memberName = staticMatcher.group(2);
                    importedItem = staticMatcher.group(1) + "." + className + "." + memberName;
                    break;
                default:
                    importedItem = staticMatcher.group(1) + staticMatcher.group(2);
            }
        } else if (singleTypeMatcher.matches()) {
            importKind = importStatement.contains(".*") ? ImportKind.ALL_TYPES : ImportKind.TYPE;
            switch (importKind) {
                case ALL_TYPES:
                    packageName = singleTypeMatcher.group(1);
                    break;
                case TYPE:
                    packageName = singleTypeMatcher.group(1).substring(0, singleTypeMatcher.group(1).lastIndexOf('.'));
                    className = singleTypeMatcher.group(1).substring(singleTypeMatcher.group(1).lastIndexOf('.') + 1);
                    break;
            }
            importedItem = singleTypeMatcher.group(1);
        } else {
            logger.error("Unrecognized import statement: " + importStatement);
            importKind = ImportKind.UNRECOGNIZED;
            importedItem = "";
        }
        return Pair.of(importKind, Triple.of(packageName, className, memberName));
    }


    public String spanToString(CodeSpan span) {
        return span.getStart().getLine() + ":" + span.getStart().getColumn() + " - " + span.getEnd().getLine() + ":" + span.getEnd().getColumn();
    }

    public static String getSnippetFromSpan(CodeSpan span, String filePath) {
        String sample;
        try {
            sample = FileCodeSampleExtractor.getCodeSample(filePath, span.getStart().getLine(), span.getStart().getColumn(), span.getEnd().getLine(), span.getEnd().getColumn());
        }
        catch (IOException e) {
            e.printStackTrace();
            sample = "Error reading file";
        }
        return sample;
    }

    private static boolean isEncompassed(CodeSpan span1, CodeSpan span2) {
        return (span2.getStart().getLine() < span1.getStart().getLine() ||
                (span2.getStart().getLine() == span1.getStart().getLine() &&
                        span2.getStart().getColumn() <= span1.getStart().getColumn())) &&
                (span2.getEnd().getLine() > span1.getEnd().getLine() ||
                        (span2.getEnd().getLine() == span1.getEnd().getLine() &&
                                span2.getEnd().getColumn() >= span1.getEnd().getColumn()));
    }

    private static boolean isEqual(CodeSpan span1, CodeSpan span2) {
        return span1.getStart().getLine() == span2.getStart().getLine() &&
                span1.getStart().getColumn() == span2.getStart().getColumn() &&
                span1.getEnd().getLine() == span2.getEnd().getLine() &&
                span1.getEnd().getColumn() == span2.getEnd().getColumn();
    }


    public static List<ImportUsage> filterSpans(List<ImportUsage> spans) {
        List<ImportUsage> result = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            boolean isSmallest = true;
            for (int j = 0; j < spans.size(); j++) {
                if (i != j && isEncompassed(spans.get(i).getSpan(), spans.get(j).getSpan()) && (!(isEqual(spans.get(i).getSpan(), spans.get(j).getSpan()) && i<=j))) {
                    isSmallest = false;
                    break;
                }
            }
            if (isSmallest) {
                result.add(spans.get(i));
            }
        }
        return result;
    }


    public void showImport(Import_ import_, List<ImportUsage> refUsages, CtImport ctImport) {
        String filePath = ctImport.getPosition().getFile().getPath();

        System.out.println("Import string: " + import_.getImportString());
        System.out.println("Import kind: " + import_.getImportKind());
        System.out.println("Package name: " + import_.getImportedPackage());
        System.out.println("Class name: " + import_.getImportedClass());
        System.out.println("Member name: " + import_.getImportedFieldOrMethod());
        System.out.println("Package is local: " + import_.getIsLocal());
        System.out.println("Span: " + spanToString(import_.getSpan()));
        System.out.println("Usages: " + import_.getUsagesCount());
        for (ImportUsage usage : import_.getUsagesList()) {
            if (usage.hasSpan()) {
                String spanText = getSnippetFromSpan(usage.getSpan(), filePath);
                System.out.println("  Span: " + spanToString(usage.getSpan()) + " // " + spanText);
            }
            else {
                System.out.println("  Span: null");
            }
        }
        System.out.println("Ref Usages: " + refUsages.size());
        for (ImportUsage usage : refUsages) {
            if (usage.hasSpan()) {
                String spanText = getSnippetFromSpan(usage.getSpan(), filePath);
                System.out.println("  Span: " + spanToString(usage.getSpan()) + " // " + spanText);
            }
            else {
                System.out.println("  Span: null");
            }
        }
        System.out.println(" ");

    }

    public List<ImportUsage> findImportUsages(CtImport ctImport, String importedItem) {
        List<ImportUsage> usagePositions = new ArrayList<>();
        CtType<?> ctType = ctImport.getPosition().getCompilationUnit().getMainType();
        String className = ctType.getQualifiedName();
        List<CtTypeAccess<?>> typeAccesses = ctType.getElements(new TypeFilter<>(CtTypeAccess.class));
        for (CtTypeAccess<?> typeAccess : typeAccesses) {
            CtTypeReference<?> typeRef = typeAccess.getAccessedType();
            if (typeRef.getQualifiedName().equals(importedItem)) {
                ImportUsage.Builder usage = ImportUsage.newBuilder();
                usage.setClassName(className);
                CodeSpan span = buildCodeSpan(typeAccess);
                if (span != null) {
                    usage.setSpan(span);
                }
                // Find the enclosing method, if any
                CtMethod<?> enclosingMethod = typeAccess.getParent(CtMethod.class);
                if (enclosingMethod != null) {
                    usage.setMethodName(enclosingMethod.getSimpleName());
                }
                usagePositions.add(usage.build());
            }
        }
        return usagePositions;
    }

    public Import_ createImport(CtImport ctImport){
        String importString = ctImport.prettyprint();
        CodeSpan span = buildCodeSpan(ctImport);
        ImportKind importKind;
        String packageName;
        String className;
        String memberName;
        // Resolve the import statement to find its kind and imported item details
        Pair<ImportKind, Triple<String, String, String>> resolvedImport = resolveImport(importString);
        importKind = resolvedImport.getLeft();
        packageName = resolvedImport.getRight().getLeft();
        className = resolvedImport.getRight().getMiddle();
        memberName = resolvedImport.getRight().getRight();
        // verify if the package and class are local
        boolean isLocal = (className.isEmpty() && findPackage(packageName)) || (!className.isEmpty() && findClass(packageName + "." + className));
        // Create the Import_ object
        Import_.Builder import_ = Import_.newBuilder();
        import_.setImportString(importString);
        import_.setImportKind(importKind);
        import_.setImportedPackage(packageName);
        import_.setImportedClass(className);
        import_.setImportedFieldOrMethod(memberName);
        import_.setIsLocal(isLocal);
        import_.setSpan(span);
        // Find usages of the import statement
        List<ImportUsage> importUsages;
        List<ImportUsage> refImportUsages;
        if (importKind.equals(ImportKind.ALL_TYPES)) {
            importUsages = new ArrayList<>();
            refImportUsages = new ArrayList<>();
        } else {
            String classFullName = packageName + "." + className;
//            importUsages = findImportUsages(ctImport, classFullName);
            ImportUsageProcessor importUsageProcessor = new ImportUsageProcessor(ctImport, classFullName);
            importUsageProcessor.findUsages();
            importUsages = importUsageProcessor.getUsagePositions();
            refImportUsages = importUsageProcessor.getRefUsagePositions();
        }
        import_.addAllUsages(importUsages);
        refImportUsages = filterSpans(refImportUsages);
        import_.addAllAllReferences(refImportUsages);
//        showImport(import_.build(), refImportUsages, ctImport);
        return import_.build();
    }

    public ImportKind fromCtImportKind(CtImportKind ctImportKind) {
        switch (ctImportKind) {
            case ALL_STATIC_MEMBERS:
                return ImportKind.ALL_STATIC_MEMBERS;
            case FIELD:
            case METHOD:
                return ImportKind.FIELD_OR_METHOD;
            case TYPE:
                return ImportKind.TYPE;
            case ALL_TYPES:
                return ImportKind.ALL_TYPES;
            default:
                return ImportKind.UNRECOGNIZED;
        }
    }

    public boolean findPackage(String packageName) {
        CtModel model = getFactory().getModel();
        CtPackage ctPackage = model.getRootPackage().getPackage(packageName);
        return ctPackage != null;
    }

    public boolean findClass(String className) {
        CtModel model = getFactory().getModel();
        CtType<?> ctType = model.getAllTypes().stream()
                .filter(type -> type.getQualifiedName().equals(className))
                .findFirst()
                .orElse(null);
        return ctType != null;
    }

    public void parseImports(CtType<?> ctType, File file){
        String filePath = file.getPath();
        Set<Import_> importSet = imports.getOrDefault(filePath, new HashSet<>());
        for (CtImport ctImport : ctType.getFactory().CompilationUnit().getOrCreate(ctType).getImports()) {
            Import_ import_ = createImport(ctImport);
            importSet.add(import_);
//            System.out.println("Import: " + ctImport.prettyprint());
//            CtImportKind ctImportKind = ctImport.getImportKind();
//            CtReference ctReference = ctImport.getReference();
//            ImportKind importKind;
//            String ImportString = ctImport.prettyprint();
//            CodeSpan span = buildCodeSpan(ctImport);
//            String importReference;
//            String importName;
//
//            resolveImport(ctImport.prettyprint());
//            System.out.println(" ");
//            if (!ctImportKind.equals(CtImportKind.UNRESOLVED)) {
//                importReference = ctReference.toString();
//                importName = ctReference.getSimpleName();
//            }
//            else {
//                Pair<ImportKind, String> resolvedImport = resolveImport(ctImport.prettyprint());
//
//                importReference = "null";
//                importName = "null";
//            }
//                System.out.println("Import kind: " + importString);
//                System.out.println("Import reference: " + importReference);
//                System.out.println("Import name: " + importName);
//                System.out.println(" ");
//            String importReference = ctImport.getReference().toString();
//            String importName = ctImport.getReference().getSimpleName();
//            String importString = ctImport.getImportKind()
//            if (importSet.stream().filter(i -> i.getImportString().equals(ctImport.getReference().getSimpleName())).count() == 0) {
//                Import_.Builder import_ = Import_.newBuilder();
//                import_.setImportedItem(ctImport.getReference().getSimpleName());
//                importSet.add(import_.build());
//            }
//            Import_.Builder import_ = Import_.newBuilder();
//            import_.setImportedItem(ctType.getQualifiedName());

        }
        imports.put(filePath, importSet);
    }

    @Override
    public void process(CtType ctType) {
        System.out.println("Name: " + ctType.getQualifiedName());
        CtPackage ctPackage = ctType.getPackage();
        if (ctPackage != null) {
            System.out.println("Package: " + ctPackage.getQualifiedName());
        }
        File file = ctType.getPosition().getFile();
        parseImports(ctType, file);

    }

//    @Override
//    public void process(CtImport ctImport) {
//        System.out.println("Import: " + ctImport);
//        String importedItem = ctImport.getReference().getSimpleName();
//        String fileName = ctImport.getPosition().getFile().getName();
//
//        // Store the import statement
//        importUsageMap.putIfAbsent(fileName, new HashSet<>());
//        importUsageMap.get(fileName).add(importedItem);
//
//        // Find usage of the imported item
//        CtType<?> parentType = ctImport.getParent(CtType.class);
//        if (parentType != null) {
//            System.out.println("Parent type: " + parentType.getSimpleName());
//            List<CtReference> references = parentType.getElements(new TypeFilter<>(CtReference.class));
//            for (CtReference reference : references) {
//                if (reference.getSimpleName().equals(importedItem)) {
//                    String usage = String.format("%s used at line %d", importedItem, reference.getPosition().getLine());
//                    System.out.println(usage);
//                    importUsageMap.get(fileName).add(usage);
//                }
//            }
//        }
//        else {
//            System.out.println("Parent type not found for import: " + ctImport);
//        }
//    }
//
//    @Override
//    public void processingDone() {
//        // Print the results
//        for (Map.Entry<String, Set<String>> entry : importUsageMap.entrySet()) {
//            System.out.println("File: " + entry.getKey());
//            for (String importInfo : entry.getValue()) {
//                System.out.println("  " + importInfo);
//            }
//            System.out.println();
//        }
//    }
}