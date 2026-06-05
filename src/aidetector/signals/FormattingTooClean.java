package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.List;

public final class FormattingTooClean extends AbstractHeuristic {

    public FormattingTooClean() {
        super("formatting_too_clean", "Formatting is suspiciously uniform", 0.3, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        boolean usesTabs = false;
        boolean usesSpaces = false;
        boolean trailingWhitespace = false;
        List<Integer> indents = new ArrayList<>();
        int considered = 0;

        for (String line : source.rawLines()) {
            if (line.isBlank()) {
                continue;
            }
            considered++;
            if (line.length() != line.stripTrailing().length()) {
                trailingWhitespace = true;
            }
            int spaces = 0;
            int index = 0;
            while (index < line.length() && (line.charAt(index) == ' ' || line.charAt(index) == '\t')) {
                if (line.charAt(index) == '\t') {
                    usesTabs = true;
                } else {
                    usesSpaces = true;
                    spaces++;
                }
                index++;
            }
            if (spaces > 0) {
                indents.add(spaces);
            }
        }

        if (considered < 12 || indents.size() < 4 || trailingWhitespace || (usesTabs && usesSpaces)) {
            return List.of();
        }

        int step = 0;
        for (int width : indents) {
            step = gcd(step, width);
        }
        if (step == 2 || step == 4) {
            return Support.aggregate("uniform " + step + "-space indentation with no trailing whitespace over " + considered + " lines");
        }
        return List.of();
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
}
