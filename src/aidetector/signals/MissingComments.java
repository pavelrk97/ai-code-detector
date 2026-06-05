package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.List;

public final class MissingComments extends AbstractHeuristic {

    public MissingComments() {
        super("lack_of_comments", "No comments across substantial code", 0.6, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        int meaningful = source.nonEmptyLineCount();
        if (meaningful >= 25 && source.commentLineCount() == 0) {
            return Support.aggregate("not a single comment across " + meaningful + " lines of code");
        }
        return List.of();
    }
}
