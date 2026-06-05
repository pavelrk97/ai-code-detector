package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.List;
import java.util.regex.Pattern;

public final class ExcessiveTryCatch extends AbstractHeuristic {

    private static final Pattern BARE_HANDLER = Pattern.compile(
            "except\\s*:|except\\s+Exception\\s*:|catch\\s*\\(\\s*(Exception|Throwable|RuntimeException|Error)\\b");

    private static final Pattern TRY = Pattern.compile("\\btry\\b");

    public ExcessiveTryCatch() {
        super("excessive_try_except", "Reflexive try/catch wrapping", 1.6, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        List<Finding> bareHandlers = Support.matchEachLine(source, BARE_HANDLER);
        int singleStatementTries = countSingleStatementTries(source);

        if (bareHandlers.size() >= 2) {
            return Support.cap(bareHandlers, 5);
        }
        if (singleStatementTries >= 2) {
            return Support.aggregate(singleStatementTries + " try blocks guarding a single statement");
        }
        return List.of();
    }

    private static int countSingleStatementTries(SourceFile source) {
        List<String> code = source.codeLines();
        int count = 0;
        for (int i = 0; i < code.size(); i++) {
            if (!TRY.matcher(code.get(i)).find()) {
                continue;
            }
            int statements = 0;
            for (int j = i + 1; j < code.size(); j++) {
                String body = code.get(j).strip();
                if (body.startsWith("}") || body.contains("catch") || body.startsWith("except") || body.contains("finally")) {
                    break;
                }
                String stripped = body.replace("{", "").replace("}", "").strip();
                if (!stripped.isEmpty()) {
                    statements++;
                }
                if (statements > 1) {
                    break;
                }
            }
            if (statements == 1) {
                count++;
            }
        }
        return count;
    }
}
