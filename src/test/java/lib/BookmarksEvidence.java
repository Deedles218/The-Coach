package lib;

import org.junit.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class BookmarksEvidence {
    private BookmarksEvidence() {
    }

    public static long checkpoint(String pathValue) {
        Path path = readable(pathValue);
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new AssertionError("Cannot read evidence size: " + path, e);
        }
    }

    public static void assertAppendedContains(String pathValue, long checkpoint, String... fragments) {
        Path path = readable(pathValue);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new AssertionError("Cannot read evidence: " + path, e);
        }
        Assert.assertTrue("Evidence file was truncated while the test was running: " + path,
                checkpoint <= bytes.length);
        String appended = new String(bytes, (int) checkpoint, bytes.length - (int) checkpoint,
                StandardCharsets.UTF_8);
        String sanitized = TestData.sanitizeSensitiveData(appended);
        for (String fragment : fragments) {
            Assert.assertTrue("Fresh evidence does not contain '" + fragment + "'. Appended evidence: " + sanitized,
                    appended.contains(fragment));
        }
    }

    private static Path readable(String pathValue) {
        Path path = Paths.get(pathValue).toAbsolutePath().normalize();
        Assert.assertTrue("Evidence file does not exist: " + path, Files.isRegularFile(path));
        Assert.assertTrue("Evidence file is not readable: " + path, Files.isReadable(path));
        return path;
    }
}
