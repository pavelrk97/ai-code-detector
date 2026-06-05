package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Support {

    private Support() {
    }

    static List<Finding> matchEachLine(SourceFile source, Pattern pattern) {
        List<Finding> found = new ArrayList<>();
        List<String> code = source.codeLines();
        for (int i = 0; i < code.size(); i++) {
            if (pattern.matcher(code.get(i)).find()) {
                found.add(new Finding(i + 1, source.snippet(i + 1)));
            }
        }
        return found;
    }

    static int countOccurrences(SourceFile source, Pattern pattern) {
        int total = 0;
        for (String line : source.codeLines()) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                total++;
            }
        }
        return total;
    }

    static List<Finding> cap(List<Finding> findings, int max) {
        if (findings.size() <= max) {
            return findings;
        }
        return new ArrayList<>(findings.subList(0, max));
    }

    static List<Finding> aggregate(String message) {
        return List.of(Finding.aggregate(message));
    }
}
