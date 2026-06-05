package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConversationalPreamble extends AbstractHeuristic {

    private static final Pattern PREAMBLE = Pattern.compile(
            "^(here'?s|here is|below is|below,|sure[,!.]|certainly|of course|let me|"
                    + "i'?ll |i have |i've |as an ai|as requested|note that this|"
                    + "this (code|function|implementation|snippet))",
            Pattern.CASE_INSENSITIVE);

    public ConversationalPreamble() {
        super("preamble_strings", "Conversational preamble left in code", 1.6, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        Map<Integer, Finding> byLine = new LinkedHashMap<>();

        for (SourceFile.Comment comment : source.comments()) {
            if (PREAMBLE.matcher(comment.text().strip()).find()) {
                int line = comment.line() + 1;
                byLine.putIfAbsent(line, new Finding(line, source.snippet(line)));
            }
        }

        List<String> raw = source.rawLines();
        int lookahead = Math.min(4, raw.size());
        for (int i = 0; i < lookahead; i++) {
            String text = raw.get(i).strip();
            if (!text.isEmpty() && PREAMBLE.matcher(text).find()) {
                byLine.putIfAbsent(i + 1, new Finding(i + 1, source.snippet(i + 1)));
            }
        }

        if (byLine.isEmpty()) {
            return List.of();
        }
        return Support.cap(new ArrayList<>(byLine.values()), 5);
    }
}
