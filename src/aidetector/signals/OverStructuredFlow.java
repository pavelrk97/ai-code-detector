package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.List;
import java.util.regex.Pattern;

public final class OverStructuredFlow extends AbstractHeuristic {

    private static final Pattern CONTROL = Pattern.compile("\\b(if|for|while|switch)\\b");

    public OverStructuredFlow() {
        super("over_structured", "Dense, textbook control flow", 1.0, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        List<Finding> hits = Support.matchEachLine(source, CONTROL);
        if (hits.size() <= 5) {
            return List.of();
        }
        return Support.cap(hits, 5);
    }
}
