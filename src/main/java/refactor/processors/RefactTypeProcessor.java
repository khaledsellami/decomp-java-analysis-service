package refactor.processors;

import com.decomp.analysis.Class_;
import com.decomp.analysis.Method_;
import processors.TypeProcessor;
import spoon.reflect.declaration.*;

import java.util.ArrayList;
import java.util.List;

public class RefactTypeProcessor extends TypeProcessor {
    protected final List<CtType<?>> types;

    public RefactTypeProcessor() {
        super();
        this.types = new ArrayList<>();
    }

    public RefactTypeProcessor(ArrayList<Class_> objects, ArrayList<Method_> methods, String appName) {
        super(objects, methods, appName);
        this.types = new ArrayList<>();
    }

    public RefactTypeProcessor(ArrayList<Class_> objects, ArrayList<Method_> methods, String appName, String serviceName) {
        super(objects, methods, appName, serviceName);
        this.types = new ArrayList<>();
    }

    @Override
    public void process(CtType ctType) {
        super.process(ctType);
        types.add(ctType);
    }

    public List<CtType<?>> getTypes() {
        return types;
    }
}
