package refactor.analysis;

import com.decomp.analysis.Class_;
import com.decomp.refactor.ClassDTO;
import spoon.reflect.declaration.CtType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisContext {
    private final Map<String, Class_> classes;
    private final Map<String, CtType<?>> ctTypes;
    private final Map<String, ClassDTO> DTOs;

    public AnalysisContext(List<Class_> classes, List<CtType<?>> ctTypes, List<ClassDTO> DTOs) {
        this.classes = new HashMap<>();
        this.ctTypes = new HashMap<>();
        this.DTOs = new HashMap<>();

        for (Class_ class_ : classes) {
            this.classes.put(class_.getFullName(), class_);
        }

        for (CtType<?> ctType : ctTypes) {
            this.ctTypes.put(ctType.getQualifiedName(), ctType);
        }

        for (ClassDTO DTO : DTOs) {
            this.DTOs.put(DTO.getFullName(), DTO);
        }
    }

    public Class_ getClass(String name) {
        return classes.getOrDefault(name, null);
    }

    public CtType<?> getCtType(String name) {
        return ctTypes.getOrDefault(name, null);
    }

    public ClassDTO getDTO(String name) {
        return DTOs.getOrDefault(name, null);
    }

    public Map<String, Class_> getClasses() {
        return classes;
    }

    public Map<String, CtType<?>> getCtTypes() {
        return ctTypes;
    }

    public Map<String, ClassDTO> getDTOs() {
        return DTOs;
    }
}
