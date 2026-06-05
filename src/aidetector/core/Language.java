package aidetector.core;

public enum Language {
    JAVA("//", "/*", "*/", "\"'", false),
    JAVASCRIPT("//", "/*", "*/", "\"'`", false),
    TYPESCRIPT("//", "/*", "*/", "\"'`", false),
    PYTHON("#", null, null, "\"'", true),
    OTHER("//", "/*", "*/", "\"'", false);

    private final String lineComment;
    private final String blockOpen;
    private final String blockClose;
    private final String quotes;
    private final boolean tripleQuoted;

    Language(String lineComment, String blockOpen, String blockClose, String quotes, boolean tripleQuoted) {
        this.lineComment = lineComment;
        this.blockOpen = blockOpen;
        this.blockClose = blockClose;
        this.quotes = quotes;
        this.tripleQuoted = tripleQuoted;
    }

    public String lineComment() {
        return lineComment;
    }

    public String blockOpen() {
        return blockOpen;
    }

    public String blockClose() {
        return blockClose;
    }

    public boolean isQuote(char c) {
        return quotes.indexOf(c) >= 0;
    }

    public boolean tripleQuoted() {
        return tripleQuoted;
    }

    public static Language fromFileName(String name) {
        String lower = name.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot + 1) : "";
        return switch (ext) {
            case "java" -> JAVA;
            case "js", "jsx", "mjs", "cjs" -> JAVASCRIPT;
            case "ts", "tsx" -> TYPESCRIPT;
            case "py", "pyw" -> PYTHON;
            default -> OTHER;
        };
    }

    public static Language fromHint(String hint) {
        if (hint == null) {
            return OTHER;
        }
        return switch (hint.toLowerCase()) {
            case "java" -> JAVA;
            case "js", "javascript" -> JAVASCRIPT;
            case "ts", "typescript" -> TYPESCRIPT;
            case "py", "python" -> PYTHON;
            default -> OTHER;
        };
    }
}
