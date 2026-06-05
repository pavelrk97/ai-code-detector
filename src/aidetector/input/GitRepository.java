package aidetector.input;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class GitRepository implements AutoCloseable {

    private final Path workspace;
    private final Path checkout;

    private GitRepository(Path workspace, Path checkout) {
        this.workspace = workspace;
        this.checkout = checkout;
    }

    public Path root() {
        return checkout;
    }

    public static GitRepository clone(String url) throws IOException {
        String normalized = normalize(url);
        Path workspace = Files.createTempDirectory("ai-detector-");
        Path checkout = workspace.resolve("repository");

        ProcessBuilder builder = new ProcessBuilder(
                "git", "clone", "--depth", "1", "--quiet", normalized, checkout.toString());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            String log;
            try (InputStream in = process.getInputStream()) {
                log = new String(in.readAllBytes());
            }
            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                deleteTree(workspace);
                throw new IOException("git clone timed out for " + normalized);
            }
            if (process.exitValue() != 0) {
                deleteTree(workspace);
                throw new IOException("git clone failed for " + normalized
                        + (log.isBlank() ? "" : "\n" + log.strip()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deleteTree(workspace);
            throw new IOException("interrupted while cloning " + normalized, e);
        }
        return new GitRepository(workspace, checkout);
    }

    static String normalize(String url) {
        String trimmed = url.trim();
        int query = trimmed.indexOf('?');
        if (query >= 0) {
            trimmed = trimmed.substring(0, query);
        }
        int fragment = trimmed.indexOf('#');
        if (fragment >= 0) {
            trimmed = trimmed.substring(0, fragment);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Override
    public void close() {
        deleteTree(workspace);
    }

    private static void deleteTree(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(GitRepository::forceDelete);
        } catch (IOException ignored) {
            directory.toFile().deleteOnExit();
        }
    }

    private static void forceDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException first) {
            path.toFile().setWritable(true, false);
            try {
                Files.deleteIfExists(path);
            } catch (IOException second) {
                path.toFile().deleteOnExit();
            }
        }
    }
}
