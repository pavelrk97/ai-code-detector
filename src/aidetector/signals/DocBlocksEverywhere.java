package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.Language;
import aidetector.core.SourceFile;

import java.util.List;
import java.util.regex.Pattern;

public final class DocBlocksEverywhere extends AbstractHeuristic {

    public DocBlocksEverywhere() {
        super("doc_on_everything", "Doc blocks on every function", 1.5, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        List<Finding> functions = Support.matchEachLine(source, functionPattern(source.language()));
        if (functions.size() < 2) {
            return List.of();
        }

        int docBlocks = 0;
        List<String> raw = source.rawLines();
        for (SourceFile.Comment comment : source.comments()) {
            if (raw.get(comment.line()).strip().startsWith("/**")) {
                docBlocks++;
            }
        }

        double coverage = (double) docBlocks / functions.size();
        if (coverage >= 0.8) {
            return Support.cap(functions, 5);
        }
        return List.of();
    }

    private static Pattern functionPattern(Language language) {
        return switch (language) {
            case PYTHON -> Pattern.compile("\\bdef\\s+\\w+");
            case JAVA -> Pattern.compile(
                    "\\b(public|private|protected)\\s+[\\w<>\\[\\],?\\s]+\\s+\\w+\\s*\\([^;{]*\\)\\s*\\{");
            default -> Pattern.compile(
                    "\\bfunction\\s+\\w+|\\b\\w+\\s*=\\s*\\([^)]*\\)\\s*=>|\\b\\w+\\s*\\([^)]*\\)\\s*\\{");
        };
    }
}
