package refactor.processors;

import com.decomp.refactor.ClassDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import refactor.analysis.DTOAnalyzer;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtType;

import java.util.ArrayList;
import java.util.List;

public class DTOProcessor extends AbstractProcessor<CtType<?>> {
    private List<ClassDTO> DTOs;
    private DTOAnalyzer analyzer;
    private Logger logger = LoggerFactory.getLogger(DTOProcessor.class);

    public DTOProcessor() {
        super();
        DTOs = new ArrayList<>();
        analyzer = new DTOAnalyzer();
    }

    @Override
    public void process(CtType<?> ctType) {
        ClassDTO classDTO = analyzer.toDTO(ctType);
        if (classDTO != null) {
            DTOs.add(classDTO);
        }
    }

    public List<ClassDTO> getDTOs() {
        return DTOs;
    }
    public DTOAnalyzer getAnalyzer() {
        return analyzer;
    }
}
