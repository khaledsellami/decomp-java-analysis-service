package processors;

import com.decomp.analysis.CodeSpan;
import com.decomp.analysis.Position;
import spoon.reflect.cu.SourcePosition;

public class Utils {
    static public CodeSpan buildCodeSpan(SourcePosition position){
        if (position.isValidPosition()) {
            Position.Builder startPosition = Position.newBuilder();
            Position.Builder endPosition = Position.newBuilder();
            CodeSpan.Builder objectSpan = CodeSpan.newBuilder();
            startPosition.setLine(position.getLine());
            startPosition.setColumn(position.getColumn());
            endPosition.setLine(position.getEndLine());
            endPosition.setColumn(position.getEndColumn());
            objectSpan.setStart(startPosition.build());
            objectSpan.setEnd(endPosition.build());
            return objectSpan.build();
        }
        return null;
    }
}
