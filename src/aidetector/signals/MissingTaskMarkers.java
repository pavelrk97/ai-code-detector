package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.List;
import java.util.regex.Pattern;

public final class MissingTaskMarkers extends AbstractHeuristic {

    private static final Pattern MARKER = Pattern.compile("\\b(TODO|FIXME|XXX|HACK)\\b");

    public MissingTaskMarkers() {
        super("no_todo_fixme", "No TODO/FIXME left behind", 0.2, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        int meaningful = source.nonEmptyLineCount();
        if (meaningful < 25) {
            return List.of();
        }
        for (SourceFile.Comment comment : source.comments()) {
            if (MARKER.matcher(comment.text()).find()) {
                return List.of();
            }
        }
        return Support.aggregate("no TODO/FIXME/XXX/HACK markers across " + meaningful + " lines");
    }
}
