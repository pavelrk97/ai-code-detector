package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefensiveNullChecks extends AbstractHeuristic {

    private static final Pattern[] GUARDS = {
            Pattern.compile("\\b(\\w+)\\s*(?:==|!=)\\s*null\\b"),
            Pattern.compile("\\b(\\w+)\\s*(?:===|!==)\\s*(?:null|undefined)\\b"),
            Pattern.compile("\\b(\\w+)\\s+is\\s+(?:not\\s+)?None\\b"),
            Pattern.compile("\\bObjects\\.requireNonNull\\s*\\(\\s*(\\w+)")
    };

    public DefensiveNullChecks() {
        super("defensive_null_checks", "Defensive null guarding everywhere", 1.3, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        Set<String> guardedVariables = new LinkedHashSet<>();
        List<Finding> hits = new ArrayList<>();
        int total = 0;

        List<String> code = source.codeLines();
        for (int i = 0; i < code.size(); i++) {
            boolean lineHit = false;
            for (Pattern guard : GUARDS) {
                Matcher matcher = guard.matcher(code.get(i));
                while (matcher.find()) {
                    total++;
                    lineHit = true;
                    if (matcher.group(1) != null) {
                        guardedVariables.add(matcher.group(1));
                    }
                }
            }
            if (lineHit) {
                hits.add(new Finding(i + 1, source.snippet(i + 1)));
            }
        }

        int meaningful = Math.max(1, source.nonEmptyLineCount());
        double density = (double) total / meaningful;
        if (guardedVariables.size() >= 3 || (total >= 5 && density >= 0.08)) {
            return Support.cap(hits, 5);
        }
        return List.of();
    }
}
