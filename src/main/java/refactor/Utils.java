package refactor;

import spoon.reflect.reference.CtTypeReference;

public class Utils {
    public static String getQualifiedNameWithGenerics(CtTypeReference<?> typeReference) {
        if (typeReference == null) {
            return "";
        }
        StringBuilder qualifiedName = new StringBuilder(typeReference.getQualifiedName());
        if (!typeReference.getActualTypeArguments().isEmpty()) {
            qualifiedName.append("<");
            boolean first = true;
            for (CtTypeReference<?> genericType : typeReference.getActualTypeArguments()) {
                if (!first) {
                    qualifiedName.append(", ");
                }
                qualifiedName.append(getQualifiedNameWithGenerics(genericType));
                first = false;
            }
            qualifiedName.append(">");
        }
        return qualifiedName.toString();
    }
}
