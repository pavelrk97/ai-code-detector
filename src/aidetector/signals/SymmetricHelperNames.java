package aidetector.signals;

import aidetector.core.Finding;
import aidetector.core.Language;
import aidetector.core.SourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SymmetricHelperNames extends AbstractHeuristic {

    private static final Set<String> VERBS = Set.of(
            "get", "set", "is", "has", "handle", "process", "validate", "create",
            "build", "make", "fetch", "load", "save", "update", "delete", "remove",
            "compute", "calculate", "parse", "format", "render", "init", "on");

    public SymmetricHelperNames() {
        super("symmetric_helper_names", "Symmetric helper naming", 1.1, Kind.AI);
    }

    @Override
    public List<Finding> evaluate(SourceFile source) {
        Pattern pattern = namePattern(source.language());
        Map<String, List<Finding>> byPrefix = new LinkedHashMap<>();

        List<String> code = source.codeLines();
        for (int i = 0; i < code.size(); i++) {
            Matcher matcher = pattern.matcher(code.get(i));
            while (matcher.find()) {
                String name = firstGroup(matcher);
                if (name == null) {
                    continue;
                }
                String prefix = verbPrefix(name);
                if (prefix == null) {
                    continue;
                }
                byPrefix.computeIfAbsent(prefix, key -> new ArrayList<>())
                        .add(new Finding(i + 1, source.snippet(i + 1)));
            }
        }

        for (Map.Entry<String, List<Finding>> entry : byPrefix.entrySet()) {
            if (VERBS.contains(entry.getKey()) && entry.getValue().size() >= 3) {
                return Support.cap(entry.getValue(), 5);
            }
        }
        return List.of();
    }

    private static String firstGroup(Matcher matcher) {
        for (int group = 1; group <= matcher.groupCount(); group++) {
            if (matcher.group(group) != null) {
                return matcher.group(group);
            }
        }
        return null;
    }

    private static String verbPrefix(String name) {
        int underscore = name.indexOf('_');
        if (underscore > 0) {
            return name.substring(0, underscore);
        }
        int i = 0;
        while (i < name.length() && Character.isLowerCase(name.charAt(i))) {
            i++;
        }
        return i == 0 ? null : name.substring(0, i);
    }

    private static Pattern namePattern(Language language) {
        return switch (language) {
            case PYTHON -> Pattern.compile("\\bdef\\s+([a-z][A-Za-z0-9_]*)");
            case JAVA -> Pattern.compile(
                    "\\b(?:public|private|protected)\\s+[\\w<>\\[\\],?\\s]+\\s+([a-z][A-Za-z0-9_]*)\\s*\\(");
            default -> Pattern.compile(
                    "\\bfunction\\s+([a-z][A-Za-z0-9_]*)|\\b(?:const|let|var)\\s+([a-z][A-Za-z0-9_]*)\\s*=\\s*(?:async\\s*)?\\(");
        };
    }
}
