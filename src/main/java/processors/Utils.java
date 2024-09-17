package processors;

import com.decomp.analysis.CodeSpan;
import com.decomp.analysis.Position;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtRecord;


public class Utils {
    private static final Map<String, String> DECLARATION_KEYWORDS;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("class", "class");
        map.put("interface", "interface");
        map.put("enum", "enum");
        map.put("annotation", "@interface");
        map.put("record", "record");
        DECLARATION_KEYWORDS = Collections.unmodifiableMap(map);
    }

    public static String checkType(CtType<?> ctType) throws Exception {
        if (ctType instanceof CtEnum) {
            return "enum";
        }
        if (ctType instanceof CtInterface) {
            return "interface";
        }
        if (ctType instanceof CtAnnotationType) {
            return "annotation";
        }
        if (ctType instanceof CtRecord) {
            return "record";
        }
        if (ctType instanceof CtClass) {
            return "class";
        }
        // If none of the above types match, return "unknown"
        throw new Exception("Unknown type");
        // return "unknown";
    }

    static public CodeSpan buildCodeSpan(CtElement element) {
        SourcePosition position = element.getPosition();
        if (position.isValidPosition()) {
            Position.Builder startPosition = Position.newBuilder();
            Position.Builder endPosition = Position.newBuilder();
            CodeSpan.Builder objectSpan = CodeSpan.newBuilder();
            int startIndex = position.getSourceStart();
            int endIndex = position.getSourceEnd();
            position = element.getFactory().createSourcePosition(
                    position.getCompilationUnit(),
                    startIndex,
                    endIndex,
                    position.getCompilationUnit().getLineSeparatorPositions()
            );
            int startLine = position.getLine();
            int startColumn = position.getColumn();
            int endLine = position.getEndLine();
            int endColumn = position.getEndColumn();
//            if (element instanceof CtModifiable) {
//                SourcePosition firstModifierPosition = getFirstModifierPosition((CtModifiable) element, position);
//                if (firstModifierPosition!=null && shouldUpdatePosition(firstModifierPosition, startLine, startColumn)) {
//                    System.out.println("Updating position with modifier from " + startLine + ":" + startColumn + " to " + firstModifierPosition.getLine() + ":" + firstModifierPosition.getColumn());
//                    startLine = firstModifierPosition.getLine();
//                    startColumn = firstModifierPosition.getColumn();
//                }
//            }
            // if method
//            if (element instanceof CtMethod) {
//                // Include return type
//                CtMethod<?> method = (CtMethod<?>) element;
//                CtTypeReference<?> returnType = method.getType();
//                if (returnType != null && returnType.getPosition().isValidPosition()) {
//                    if (shouldUpdatePosition(returnType.getPosition(), startLine, startColumn)) {
//                        startLine = returnType.getPosition().getLine();
//                        startColumn = returnType.getPosition().getColumn();
//                    }
//                }
//            } else if (element instanceof CtType) {
//                System.out.println("Is Type");
//                SourcePosition keywordPosition = getKeyWordPosition((CtType<?>) element, position);
//                if (keywordPosition!=null && shouldUpdatePosition(keywordPosition, startLine, startColumn)) {
//                    System.out.println("Updating position with keyword from " + startLine + ":" + startColumn + " to " + keywordPosition.getLine() + ":" + keywordPosition.getColumn());
//                    startLine = keywordPosition.getLine();
//                    startColumn = keywordPosition.getColumn();
//                }
//                SourcePosition firstModifierPosition = getFirstModifierPosition((CtType<?>) element, position);
//                if (firstModifierPosition!=null && shouldUpdatePosition(firstModifierPosition, startLine, startColumn)) {
//                    System.out.println("Updating position with modifier from " + startLine + ":" + startColumn + " to " + firstModifierPosition.getLine() + ":" + firstModifierPosition.getColumn());
//                    startLine = firstModifierPosition.getLine();
//                    startColumn = firstModifierPosition.getColumn();
//                }
//            }
            // Check for annotations
//            List<CtAnnotation<?>> annotations = element.getAnnotations();
//            for (CtAnnotation<?> annotation : annotations) {
//                SourcePosition annotationPosition = annotation.getPosition();
//                if (annotationPosition.isValidPosition()) {
//                    startLine = Math.min(startLine, annotationPosition.getLine());
//                    if (startLine == annotationPosition.getLine()) {
//                        startColumn = Math.min(startColumn, annotationPosition.getColumn());
//                    }
//                }
//            }
            // Build the CodeSpan
            startPosition.setLine(startLine);
            startPosition.setColumn(startColumn);
            endPosition.setLine(endLine);
            endPosition.setColumn(endColumn);
            objectSpan.setStart(startPosition.build());
            objectSpan.setEnd(endPosition.build());
            return objectSpan.build();
        }
        return null;
    }

    private static boolean shouldUpdatePosition(SourcePosition pos, int startLine, int startColumn) {
        return (pos.getLine() < startLine || (pos.getLine() == startLine && pos.getColumn() < startColumn));
    }


    public static int getLastMatch(String modifierString, String relevantCode) {
        String modifierStr = modifierString.toLowerCase() + "\\b";
        Pattern pattern = Pattern.compile(modifierStr);
        Matcher matcher = pattern.matcher(relevantCode.toLowerCase());
        int index = -1;
        while (matcher.find()) {
            index = matcher.start();
        }
        return index;
    }

    private static SourcePosition getKeyWordPosition(CtType<?> element, SourcePosition elementPosition) {
        try {
            String keyword = checkType(element);
            String sourceCode = elementPosition.getCompilationUnit().getOriginalSourceCode();
            int startIndex = elementPosition.getSourceStart();
            int endIndex = elementPosition.getSourceEnd();
            String relevantCode = sourceCode.substring(Math.max(0, startIndex - 100), endIndex);

            int index = getLastMatch(keyword, relevantCode);
            if (index != -1) {
                int absoluteIndex = Math.max(0, startIndex - 100) + index;
                return element.getFactory().createSourcePosition(
                        elementPosition.getCompilationUnit(),
                        absoluteIndex,
                        absoluteIndex + keyword.length(),
                        elementPosition.getCompilationUnit().getLineSeparatorPositions()
                );
            }
            System.out.println("Keyword not found. Skipping");
            return null;

        } catch (Exception e) {
            System.out.println("Unknown type. Skipping");
            return null;
        }
    }

    private static SourcePosition getFirstModifierPosition(CtModifiable modifiable, SourcePosition elementPosition) {
        String sourceCode = elementPosition.getCompilationUnit().getOriginalSourceCode();
        int startIndex = elementPosition.getSourceStart();
        int endIndex = elementPosition.getSourceEnd();
        String relevantCode = sourceCode.substring(Math.max(0, startIndex - 100), endIndex);
        SourcePosition firstModifierPosition = null;
        int earliestIndex = Integer.MAX_VALUE;

        for (ModifierKind modifier : modifiable.getModifiers()) {
            int index = getLastMatch(modifier.toString(), relevantCode);
            if (index != -1 && index < earliestIndex) {
                earliestIndex = index;
                int absoluteIndex = Math.max(0, startIndex - 100) + index;
                firstModifierPosition = modifiable.getFactory().createSourcePosition(
                        elementPosition.getCompilationUnit(),
                        absoluteIndex,
                        absoluteIndex + modifier.toString().length(),
                        elementPosition.getCompilationUnit().getLineSeparatorPositions()
                );
            }
        }
        return firstModifierPosition;
    }
}
