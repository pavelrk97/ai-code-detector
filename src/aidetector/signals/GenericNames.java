package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.List;
import java.util.regex.Pattern;

public final class GenericNames extends AbstractHeuristic {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("(?<![.\\w])(data|temp|tmp|obj|arr|foo|bar|baz|retval)\\b");

    public GenericNames() {
        super("generic_names", "Generic placeholder identifiers", 1.5, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        List<Finding> hits = Support.matchEachLine(source, PLACEHOLDER);
        if (hits.size() < 3) {
            return List.of();
        }
        return Support.cap(hits, 5);
    }
}
