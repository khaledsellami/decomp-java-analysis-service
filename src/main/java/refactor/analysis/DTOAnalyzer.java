package refactor.analysis;

import com.decomp.refactor.Field;
import com.decomp.refactor.FieldType;
import com.decomp.refactor.TypeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtType;

import com.decomp.refactor.ClassDTO;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;

public class DTOAnalyzer {
    Map<String, FieldType> parsedTypes;
    private static final Set<String> STANDARD_LIBRARY_PREFIXES = Set.of(
            "java.", "javax.", "jdk.", "sun."
    );
    private Logger logger = LoggerFactory.getLogger(DTOAnalyzer.class);
    public DTOAnalyzer() {
        this.parsedTypes = new HashMap<>();
    }

    public FieldType getFieldType(CtTypeReference<?> ctTypeReference) {
        return parsedTypes.getOrDefault(ctTypeReference.getQualifiedName(), parseFieldType(ctTypeReference));
    }

    private TypeSource getTypeSource(CtTypeReference<?> ctTypeReference) {
        // Check if the type is a primitive type (int, boolean, etc.)
        if (ctTypeReference.isPrimitive())
            return TypeSource.PRIMITIVE;
        // check if the type is from the standard libraries
        String qualifiedName = ctTypeReference.getQualifiedName();
        if (qualifiedName != null) {
            for (String prefix : STANDARD_LIBRARY_PREFIXES) {
                if (qualifiedName.startsWith(prefix)) {
                    return TypeSource.STANDARD;
                }
            }
        }
        // check if the type is local (from the source code of the same application
        if (ctTypeReference.getDeclaration() != null) {
            return TypeSource.LOCAL;
        }
        // otherwise it is considered a third-party library
        return TypeSource.LIBRARY;
    }

    private boolean isSerializable(CtTypeReference<?> ctTypeReference, TypeSource typeSource, List<FieldType> genericTypes) {
        if (typeSource == TypeSource.PRIMITIVE) {
            return true;
        }
        if (ctTypeReference.isSubtypeOf(
                ctTypeReference.getFactory().Type().createReference(java.io.Serializable.class)))
            return true;
        // Handle special cases for generics (e.g., List<T>) or arrays (e.g., T[])
        if (ctTypeReference.isSubtypeOf(ctTypeReference.getFactory().Type().createReference(java.util.Collection.class))|ctTypeReference.isArray()) {
            // Check generic type arguments recursively
            return genericTypes.stream().allMatch(DTOAnalyzer::isFieldSerializable);
        }
        return false;
    }

    private static boolean isFieldSerializable(FieldType fieldType) {
        return fieldType.getIsSerializable();
    }

    private List<FieldType> parseGenericTypes(CtTypeReference<?> ctTypeReference) {
        List<FieldType> genericTypes = new ArrayList<>();
        if (ctTypeReference.isArray()){
            CtArrayTypeReference<?> arrayType = (CtArrayTypeReference<?>) ctTypeReference;
            CtTypeReference<?> componentType = arrayType.getComponentType();
            genericTypes.add(getFieldType(componentType));
        }
        else {
            for (CtTypeReference<?> genericType : ctTypeReference.getActualTypeArguments()) {
                genericTypes.add(getFieldType(genericType));
            }
        }
        return genericTypes;
    }

    private FieldType parseFieldType(CtTypeReference<?> ctTypeReference) {
        String typeName = ctTypeReference.getQualifiedName();
        if (parsedTypes.containsKey(typeName)) {
            return parsedTypes.get(typeName);
        }
        String simpleName = ctTypeReference.getSimpleName();
        TypeSource typeSource = getTypeSource(ctTypeReference);
        List<FieldType> genericTypes = parseGenericTypes(ctTypeReference);
        boolean isSerializable = isSerializable(ctTypeReference, typeSource, genericTypes);
        FieldType.Builder builder = FieldType.newBuilder();
        builder.setFullName(typeName);
        builder.setSimpleName(simpleName);
        builder.setTypeSource(typeSource);
        builder.setIsSerializable(isSerializable);
        builder.addAllGenericTypes(genericTypes);
        FieldType fieldType = builder.build();
        parsedTypes.put(typeName, fieldType);
        return fieldType;
    }

    private Field toField(CtFieldReference ctField) {
        String name = ctField.getSimpleName();
        String parentName = ctField.getDeclaringType().getQualifiedName();
        FieldType fieldType = getFieldType(ctField.getType());
        Field.Builder builder = Field.newBuilder();
        builder.setVariableName(name);
        builder.setParentName(parentName);
        builder.setType(fieldType);
        return builder.build();
    }

    public ClassDTO toDTO(CtType<?> ctType) {
        String fullName = ctType.getQualifiedName();
        List<Field> fields = new ArrayList<>();
        for (Object f: ctType.getAllFields()) {
            CtFieldReference ctField = (CtFieldReference) f;
            fields.add(toField(ctField));
        }
        ClassDTO.Builder builder = ClassDTO.newBuilder();
        builder.setFullName(fullName);
        builder.addAllFields(fields);
        return builder.build();
    }
}
