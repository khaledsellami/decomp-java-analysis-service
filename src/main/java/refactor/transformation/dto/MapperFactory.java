package refactor.transformation.dto;

import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;

import java.util.HashMap;
import java.util.Map;

public class MapperFactory {
    private final Factory factory;
    private final Map<String, MapperGenerator> processedMappers;
    public MapperFactory(Factory factory) {
        this.factory = factory;
        processedMappers = new HashMap<>();
    }

    public MapperGenerator getMapper(String name) {
        return processedMappers.getOrDefault(name, null);
    }

    public MapperGenerator getOrGenerateMapper(CtType<?> originalType, CtType<?> dtoType) {
        String dtoName = dtoType.getQualifiedName();
        if (processedMappers.containsKey(dtoName)) {
            return processedMappers.get(dtoName);
        }
        MapperGenerator mapperGenerator = new MapperGenerator(factory, originalType, dtoType);
        processedMappers.put(dtoName, mapperGenerator);
        return mapperGenerator;
    }

}
