package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RepetitiveLines extends AbstractHeuristic {

    public RepetitiveLines() {
        super("repetitive_lines", "Repeated, copy-pasted lines", 1.4, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Integer> firstSeen = new HashMap<>();
        int considered = 0;

        List<String> code = source.codeLines();
        for (int i = 0; i < code.size(); i++) {
            String normalized = code.get(i).strip();
            if (normalized.length() < 6 || isPunctuationOnly(normalized) || isStructuralNoise(normalized)) {
                continue;
            }
            considered++;
            counts.merge(normalized, 1, Integer::sum);
            firstSeen.putIfAbsent(normalized, i + 1);
        }
        if (considered == 0) {
            return List.of();
        }

        int duplicateExtra = 0;
        int peak = 0;
        for (int occurrences : counts.values()) {
            duplicateExtra += occurrences - 1;
            peak = Math.max(peak, occurrences);
        }
        double ratio = (double) duplicateExtra / considered;
        if (ratio <= 0.2 && peak < 4) {
            return List.of();
        }

        List<Finding> evidence = new ArrayList<>();
        counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> evidence.add(new Finding(
                        firstSeen.get(entry.getKey()),
                        SourceFile.shorten(entry.getKey()) + "  (x" + entry.getValue() + ")")));
        return evidence;
    }

    private static boolean isPunctuationOnly(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStructuralNoise(String text) {
        if (text.startsWith("<") || text.contains("</")) {
            return true;
        }
        if (text.startsWith("@") || text.startsWith(".")) {
            return true;
        }
        if (text.startsWith("import ") || text.startsWith("package ")) {
            return true;
        }
        return text.equals("});") || text.equals("})") || text.equals("));") || text.equals("})),");
    }
}
