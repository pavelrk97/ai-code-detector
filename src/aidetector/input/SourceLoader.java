package aidetector.input;

import aidetector.core.Language;
import aidetector.core.SourceFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class SourceLoader {

    private static final Set<String> SOURCE_EXTENSIONS =
            Set.of("java", "js", "jsx", "mjs", "cjs", "ts", "tsx", "py", "pyw");

    private static final Set<String> SKIP_DIRECTORIES = Set.of(
            ".git", "node_modules", "target", "build", "dist", "out", "vendor",
            ".gradle", ".idea", ".mvn", "bin", "__pycache__", ".venv", "venv");

    public List<SourceFile> load(String target, String languageHint) throws IOException {
        if (target.equals("-")) {
            String content = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            Language language = languageHint != null ? Language.fromHint(languageHint) : Language.OTHER;
            return List.of(SourceFile.of("<stdin>", content, language));
        }
        if (isRemote(target)) {
            try (GitRepository repository = GitRepository.clone(target)) {
                return walk(repository.root());
            }
        }
        Path path = Path.of(target);
        if (!Files.exists(path)) {
            throw new NoSuchFileException(target);
        }
        if (Files.isDirectory(path)) {
            return walk(path);
        }
        Path base = path.getParent() != null ? path.getParent() : path;
        return List.of(read(path, base));
    }

    static boolean isRemote(String target) {
        String lower = target.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("git@") || lower.endsWith(".git");
    }

    private List<SourceFile> walk(Path root) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(root)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isSkipped(root, path))
                    .filter(path -> SOURCE_EXTENSIONS.contains(extension(path)))
                    .sorted()
                    .toList();
        }
        List<SourceFile> sources = new ArrayList<>(files.size());
        for (Path file : files) {
            sources.add(read(file, root));
        }
        return sources;
    }

    private boolean isSkipped(Path root, Path file) {
        for (Path part : root.relativize(file)) {
            if (SKIP_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private SourceFile read(Path file, Path base) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String content = new String(bytes, StandardCharsets.UTF_8);
        String shown = file.startsWith(base) ? base.relativize(file).toString() : file.toString();
        return SourceFile.of(
                shown.replace('\\', '/'),
                content,
                Language.fromFileName(file.getFileName().toString()));
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }
}
