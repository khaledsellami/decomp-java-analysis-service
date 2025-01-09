package refactor;

import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

public class ProjectMetadata {
    public static final String CLASSNAME_PREFIX = "";
    public static final String DTO_SUFFIX = "DTO";
    public static final String MAPPER_SUFFIX = "Mapper";
    public static final String MapStructPackage = "org.mapstruct";

    public static CtTypeReference<?> getByteArrayType(Factory factory) {
        // Spoon's pretty printer does not print byte[] correctly so we'll use List<Byte> instead
//        CtTypeReference<?> byteArrayType = factory.Type().createArrayReference(factory.Type().BYTE_PRIMITIVE);
//        return byteArrayType;
        CtTypeReference<?> byteType = factory.Type().createReference(Byte.class);
        CtTypeReference<?> listType = factory.Type().createReference(List.class);
        listType.addActualTypeArgument(byteType);
        return listType;
    }
}
