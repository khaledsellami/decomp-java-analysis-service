package processors;

import com.decomp.analysis.CodeSpan;
import com.decomp.analysis.ImportUsage;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

import static processors.Utils.buildCodeSpan;

public class ImportUsageProcessor {
    private final CtImport ctImport;
    private final String targetImport;
    private final List<ImportUsage> usagePositions;
    private final List<ImportUsage> refUsagePositions;
    public ImportUsageProcessor(CtImport ctImport, String targetImport) {
        this.ctImport = ctImport;
        this.targetImport = targetImport;
        this.usagePositions = new java.util.ArrayList<>();
        this.refUsagePositions = new java.util.ArrayList<>();
    }

    public List<ImportUsage> getUsagePositions() {
        return usagePositions;
    }

    public List<ImportUsage> getRefUsagePositions() {
        return refUsagePositions;
    }

    public void findUsages() {
        CtType<?> ctType = ctImport.getPosition().getCompilationUnit().getMainType();
        String className = ctType.getQualifiedName();

        // Process all relevant elements
        processTypeReferences(ctType, className);
        processFields(ctType, className);
        processMethods(ctType, className);
        processLocalVariables(ctType, className);
        processExceptions(ctType, className);
        processCasting(ctType, className);
//            processInvocations(ctType, qualifiedName);
    }

    public static CtElement findFirstParentWithValidPosition(CtTypeReference<?> typeRef) {
        CtElement parent = typeRef;
        while (parent != null) {
            SourcePosition position = parent.getPosition();
            if (position != null && position.isValidPosition()) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null; // No valid parent found
    }

    private void processTypeReferences(CtType<?> ctType, String className) {
        List<CtTypeAccess<?>> typeAccesses = ctType.getElements(new TypeFilter<>(CtTypeAccess.class));
        for (CtTypeAccess<?> typeAccess : typeAccesses) {
            CtTypeReference<?> typeRef = typeAccess.getAccessedType();
            if (typeRef.getQualifiedName().equals(targetImport)) {
                addUsage(typeAccess, className);
            }
        }
        List<CtTypeReference<?>> typeReferences = ctType.getElements(new TypeFilter<>(CtTypeReference.class));
        for (CtTypeReference<?> typeRef : typeReferences) {
            if (typeRef.getQualifiedName().equals(targetImport)) {
                CtElement parent = findFirstParentWithValidPosition(typeRef);
                addRefUsage(parent, className);
            }
        }
//        List<CtTypeReference<?>> typeReferences = type.getElements(new TypeFilter<>(CtTypeReference.class));
//        for (CtTypeReference<?> typeRef : typeReferences) {
//            if (typeRef.getQualifiedName().equals(qualifiedName)) {
//                addUsage(typeRef, "Type Reference");
//            }
//        }
    }

    private void processFields(CtType<?> ctType, String className) {
        List<CtField<?>> fields = ctType.getElements(new TypeFilter<>(CtField.class));
        for (CtField<?> field : fields) {
            if (field.getType().getQualifiedName().equals(targetImport)) {
                addUsage(field.getType(), className);
            }
        }
    }

    private void processMethods(CtType<?> ctType, String className) {
        List<CtMethod<?>> methods = ctType.getElements(new TypeFilter<>(CtMethod.class));
        for (CtMethod<?> method : methods) {
            // Check return type
            if (method.getType().getQualifiedName().equals(targetImport)) {
                addUsage(method.getType(), className);
            }
            // Check parameter types
            for (CtParameter<?> param : method.getParameters()) {
                if (param.getType().getQualifiedName().equals(targetImport)) {
                    addUsage(param.getType(), className);
                }
            }
        }
    }

    private void processLocalVariables(CtType<?> ctType, String className) {
        List<CtLocalVariable<?>> localVars = ctType.getElements(new TypeFilter<>(CtLocalVariable.class));
        for (CtLocalVariable<?> localVar : localVars) {
            if (localVar.getType().getQualifiedName().equals(targetImport)) {
                addUsage(localVar.getType(), className);
            }
        }
    }

    private void processExceptions(CtType<?> ctType, String className) {
        // Check for thrown exceptions
        List<CtMethod<?>> methods = ctType.getElements(new TypeFilter<>(CtMethod.class));
        for (CtMethod<?> method : methods) {
            for (CtTypeReference<? extends Throwable> thrownType : method.getThrownTypes()) {
                if (thrownType.getQualifiedName().equals(targetImport)) {
                    addUsage(thrownType, className);
                }
            }
        }

        // Check for caught exceptions
        List<CtCatch> catchBlocks = ctType.getElements(new TypeFilter<>(CtCatch.class));
        for (CtCatch catchBlock : catchBlocks) {
            if (catchBlock.getParameter().getType().getQualifiedName().equals(targetImport)) {
                addUsage(catchBlock.getParameter().getType(), className);
            }
        }
    }

    private void processCasting(CtType<?> ctType, String className) {
        // Get all expressions in the given CtType
        List<CtExpression<?>> expressions = ctType.getElements(new TypeFilter<>(CtExpression.class));

        // Iterate over each expression and check if it's a cast operation
        for (CtExpression<?> expression : expressions) {
            if (expression.getRoleInParent() == CtRole.CAST) {
                CtTypeReference<?> targetType = expression.getType();
                System.out.println("Cast operation to type: " + targetType.getQualifiedName());
                if (targetType.getQualifiedName().equals(targetImport)) {
                    addUsage(targetType, className);
                }
            }
        }
    }

//    private void processInvocations(CtType<?> ctType, String className) {
//        List<CtInvocation<?>> invocations = ctType.getElements(new TypeFilter<>(CtInvocation.class));
//        for (CtInvocation<?> invocation : invocations) {
//            CtExpression<?> target = invocation.getTarget();
//            if (target instanceof CtTypeAccess) {
//                CtTypeAccess<?> typeAccess = (CtTypeAccess<?>) target;
//                if (typeAccess.getAccessedType().getQualifiedName().equals(targetImport)) {
//                    addUsage(invocation, className);
//                }
//            } else if (target instanceof CtFieldRead) {
//                CtFieldRead<?> fieldRead = (CtFieldRead<?>) target;
//                if (fieldRead.getType().getQualifiedName().equals(targetImport)) {
//                    addUsage(invocation, className);
//                }
//            }
//        }
//    }

    private void addUsage(CtElement element, String className) {
        ImportUsage.Builder usage = ImportUsage.newBuilder();
        usage.setClassName(className);
        CodeSpan span = buildCodeSpan(element);
        if (span != null) {
            usage.setSpan(span);
        }
        // Find the enclosing method, if any
        CtMethod<?> enclosingMethod = element.getParent(CtMethod.class);
        if (enclosingMethod != null) {
            usage.setMethodName(enclosingMethod.getSimpleName());
        }
        usagePositions.add(usage.build());
    }

    private void addRefUsage(CtElement element, String className) {
        ImportUsage.Builder usage = ImportUsage.newBuilder();
        usage.setClassName(className);
        CodeSpan span = buildCodeSpan(element);
        if (span != null) {
            usage.setSpan(span);
        }
        // Find the enclosing method, if any
        CtMethod<?> enclosingMethod = element.getParent(CtMethod.class);
        if (enclosingMethod != null) {
            usage.setMethodName(enclosingMethod.getSimpleName());
        }
        refUsagePositions.add(usage.build());
    }
}
