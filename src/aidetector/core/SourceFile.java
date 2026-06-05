package aidetector.core;

import java.util.ArrayList;
import java.util.List;

public final class SourceFile {

    public record Comment(int line, String text) {
    }

    private enum Scan {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING
    }

    private final String path;
    private final Language language;
    private final List<String> rawLines;
    private final List<String> codeLines;
    private final boolean[] commentLines;
    private final List<Comment> comments;

    private SourceFile(String path, Language language, List<String> rawLines,
                       List<String> codeLines, boolean[] commentLines, List<Comment> comments) {
        this.path = path;
        this.language = language;
        this.rawLines = rawLines;
        this.codeLines = codeLines;
        this.commentLines = commentLines;
        this.comments = comments;
    }

    public String path() {
        return path;
    }

    public Language language() {
        return language;
    }

    public List<String> rawLines() {
        return rawLines;
    }

    public List<String> codeLines() {
        return codeLines;
    }

    public List<Comment> comments() {
        return comments;
    }

    public int lineCount() {
        return rawLines.size();
    }

    public boolean isCommentLine(int index) {
        return index >= 0 && index < commentLines.length && commentLines[index];
    }

    public int commentLineCount() {
        int total = 0;
        for (boolean flagged : commentLines) {
            if (flagged) {
                total++;
            }
        }
        return total;
    }

    public int nonEmptyLineCount() {
        int total = 0;
        for (String line : rawLines) {
            if (!line.isBlank()) {
                total++;
            }
        }
        return total;
    }

    public String snippet(int oneBasedLine) {
        int index = oneBasedLine - 1;
        if (index < 0 || index >= rawLines.size()) {
            return "";
        }
        return shorten(rawLines.get(index).strip());
    }

    public static String shorten(String text) {
        if (text.length() > 80) {
            return text.substring(0, 77) + "...";
        }
        return text;
    }

    public static SourceFile of(String path, String content, Language language) {
        List<String> raw = splitLines(content);
        int total = raw.size();

        StringBuilder[] code = new StringBuilder[total];
        for (int i = 0; i < total; i++) {
            code[i] = new StringBuilder();
        }
        boolean[] commentMark = new boolean[total];
        List<Comment> comments = new ArrayList<>();

        Scan state = Scan.CODE;
        int line = 0;
        char quote = 0;
        boolean triple = false;
        StringBuilder buffer = new StringBuilder();
        int bufferLine = 0;

        String lineComment = language.lineComment();
        String blockOpen = language.blockOpen();
        String blockClose = language.blockClose();

        int length = content.length();
        int i = 0;
        while (i < length) {
            char c = content.charAt(i);

            if (c == '\r') {
                i++;
                continue;
            }
            if (c == '\n') {
                if (state == Scan.LINE_COMMENT) {
                    comments.add(new Comment(bufferLine, buffer.toString().strip()));
                    buffer.setLength(0);
                    state = Scan.CODE;
                }
                line = Math.min(line + 1, total - 1);
                i++;
                continue;
            }

            switch (state) {
                case CODE -> {
                    if (lineComment != null && content.startsWith(lineComment, i)) {
                        state = Scan.LINE_COMMENT;
                        bufferLine = line;
                        commentMark[line] = true;
                        i += lineComment.length();
                    } else if (blockOpen != null && content.startsWith(blockOpen, i)) {
                        state = Scan.BLOCK_COMMENT;
                        bufferLine = line;
                        commentMark[line] = true;
                        i += blockOpen.length();
                    } else if (language.isQuote(c)) {
                        quote = c;
                        triple = language.tripleQuoted() && content.startsWith(tripled(c), i);
                        state = Scan.STRING;
                        i += triple ? 3 : 1;
                    } else {
                        code[line].append(c);
                        i++;
                    }
                }
                case LINE_COMMENT -> {
                    buffer.append(c);
                    i++;
                }
                case BLOCK_COMMENT -> {
                    commentMark[line] = true;
                    if (blockClose != null && content.startsWith(blockClose, i)) {
                        comments.add(new Comment(bufferLine, buffer.toString().strip()));
                        buffer.setLength(0);
                        state = Scan.CODE;
                        i += blockClose.length();
                    } else {
                        buffer.append(c);
                        i++;
                    }
                }
                case STRING -> {
                    if (!triple && c == '\\') {
                        if (i + 1 < length && content.charAt(i + 1) == '\n') {
                            line = Math.min(line + 1, total - 1);
                        }
                        i += 2;
                    } else if (triple && content.startsWith(tripled(quote), i)) {
                        state = Scan.CODE;
                        i += 3;
                    } else if (!triple && c == quote) {
                        state = Scan.CODE;
                        i++;
                    } else {
                        i++;
                    }
                }
            }
        }
        if (state == Scan.LINE_COMMENT || state == Scan.BLOCK_COMMENT) {
            comments.add(new Comment(bufferLine, buffer.toString().strip()));
        }

        List<String> codeLines = new ArrayList<>(total);
        for (StringBuilder builder : code) {
            codeLines.add(builder.toString());
        }
        return new SourceFile(path, language, List.copyOf(raw), List.copyOf(codeLines), commentMark, List.copyOf(comments));
    }

    private static String tripled(char quote) {
        return String.valueOf(quote).repeat(3);
    }

    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines.add(stripCarriage(content.substring(start, i)));
                start = i + 1;
            }
        }
        lines.add(stripCarriage(content.substring(start)));
        return lines;
    }

    private static String stripCarriage(String line) {
        if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
            return line.substring(0, line.length() - 1);
        }
        return line;
    }
}
