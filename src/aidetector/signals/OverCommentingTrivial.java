package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.List;

public final class OverCommentingTrivial extends AbstractHeuristic {

    public OverCommentingTrivial() {
        super("over_commenting_trivial_ops", "Comments narrating trivial steps", 2.0, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        int meaningful = source.nonEmptyLineCount();
        if (meaningful == 0) {
            return List.of();
        }
        double commentRatio = (double) source.commentLineCount() / meaningful;

        List<Finding> pairs = new ArrayList<>();
        List<String> raw = source.rawLines();
        for (SourceFile.Comment comment : source.comments()) {
            int next = comment.line() + 1;
            while (next < raw.size() && raw.get(next).isBlank()) {
                next++;
            }
            if (next < raw.size() && !source.isCommentLine(next)) {
                String statement = raw.get(next).strip();
                if (!statement.isEmpty() && statement.length() < 40) {
                    pairs.add(new Finding(next + 1, source.snippet(next + 1)));
                }
            }
        }

        if (commentRatio > 0.5) {
            List<Finding> evidence = new ArrayList<>();
            for (SourceFile.Comment comment : source.comments()) {
                evidence.add(new Finding(comment.line() + 1, source.snippet(comment.line() + 1)));
                if (evidence.size() >= 5) {
                    break;
                }
            }
            return evidence.isEmpty()
                    ? Support.aggregate(Math.round(commentRatio * 100) + "% of lines carry a comment")
                    : evidence;
        }
        if (pairs.size() >= 4) {
            return Support.cap(pairs, 5);
        }
        return List.of();
    }
}
