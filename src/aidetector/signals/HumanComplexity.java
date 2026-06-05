package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HumanComplexity extends AbstractHeuristic {

    private static final Set<String> CONSTRUCTS = Set.of(
            "if", "else", "for", "while", "switch", "case", "do", "try", "catch",
            "finally", "throw", "return", "break", "continue", "async", "await",
            "yield", "goto", "when");

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public HumanComplexity() {
        super("human_complexity", "Varied, human-style complexity", 1.0, Kind.HUMAN);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        Set<String> seen = new HashSet<>();
        for (String line : source.codeLines()) {
            Matcher matcher = WORD.matcher(line);
            while (matcher.find()) {
                String token = matcher.group();
                if (CONSTRUCTS.contains(token)) {
                    seen.add(token);
                }
            }
        }
        if (source.nonEmptyLineCount() > 20 && seen.size() > 10) {
            return Support.aggregate(seen.size() + " distinct control constructs across " + source.nonEmptyLineCount() + " lines");
        }
        return List.of();
    }
}
