package processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.Method_;
import com.decomp.analysis.Invocation_;
import com.decomp.analysis.Import_;
import com.decomp.refactor.ClassDTO;
import com.decomp.refactor.extension.MethodAPITypes;

import java.util.ArrayList;
import java.util.List;

public class ProcessedContainers {
    public List<Class_> classes;
    public List<Method_> methods;
    public List<Invocation_> invocations;
    public List<Import_> imports;
    public List<ClassDTO> DTOs;
    public List<MethodAPITypes> methodAPITypes;

    public ProcessedContainers() {
        classes = new ArrayList<>();
        methods = new ArrayList<>();
        invocations = new ArrayList<>();
        imports = new ArrayList<>();
        DTOs = new ArrayList<>();
        methodAPITypes = new ArrayList<>();
    }

    public ProcessedContainers(List<Class_> classes, List<Method_> methods, List<Invocation_> invocations, List<Import_> imports,
                               List<ClassDTO> DTOs, List<MethodAPITypes> methodAPITypes) {
        this.classes = classes;
        this.methods = methods;
        this.invocations = invocations;
        this.imports = imports;
        this.DTOs = DTOs;
        this.methodAPITypes = methodAPITypes;
    }

    public List<Class_> getClasses() {
        return classes;
    }

    public List<Method_> getMethods() {
        return methods;
    }

    public List<Invocation_> getInvocations() {
        return invocations;
    }

    public List<Import_> getImports() {
        return imports;
    }
    public List<ClassDTO> getDTOs() {
        return DTOs;
    }
    public List<MethodAPITypes> getMethodAPITypes() {
        return methodAPITypes;
    }

}
