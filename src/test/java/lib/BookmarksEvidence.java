package lib;

import org.junit.Assert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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

    public static void assertBookmarkAnalytics(String path, long checkpoint, String lessonId, int slide) {
        awaitRecords(path, checkpoint, rows -> hasEvent(rows, "BookmarksOpened", null, 0)
                && hasEvent(rows, "BookmarkSaved", lessonId, slide)
                && hasEvent(rows, "BookmarkDeleted", lessonId, slide),
                "Fresh BookmarksOpened/BookmarkSaved/BookmarkDeleted events with the expected lesson and slide");
    }

    public static void assertBookmarkRequest(String path, long checkpoint, String lessonId,
                                             String programId, int day, int slide) {
        awaitRecords(path, checkpoint, rows -> rows.stream().anyMatch(row -> {
            Object body = row.get("body");
            if (!(body instanceof Map)) return false;
            Map<?, ?> fields = (Map<?, ?>) body;
            return "POST".equals(row.get("method"))
                    && ("/api/v1/coachprogram/lessons/" + lessonId + "/bookmark").equals(row.get("path"))
                    && programId.equals(fields.get("program_id"))
                    && numberEquals(fields.get("program_day"), day)
                    && numberEquals(fields.get("slide_number"), slide);
        }), "Fresh bookmark POST with the expected lesson, program, original day and slide");
    }

    private static boolean hasEvent(List<Map<String, Object>> rows, String event, String lesson, int slide) {
        return rows.stream().anyMatch(row -> {
            if (!event.equals(row.get("event")) && !event.equals(row.get("event_type"))) return false;
            if (lesson == null) return true;
            Object properties = row.containsKey("properties") ? row.get("properties") : row.get("event_properties");
            if (!(properties instanceof Map)) return false;
            Map<?, ?> fields = (Map<?, ?>) properties;
            return lesson.equals(fields.get("lesson_id")) && numberEquals(fields.get("slide_number"), slide);
        });
    }

    private static boolean numberEquals(Object value, int expected) {
        return value instanceof Number && ((Number) value).doubleValue() == expected;
    }

    private static void awaitRecords(String pathValue, long checkpoint,
                                     Predicate<List<Map<String, Object>>> expected, String description) {
        Path path = readable(pathValue);
        long seconds = Long.getLong("coach.bookmarks.evidenceTimeoutSeconds", 10L);
        try {
            new FluentWait<>(path).withTimeout(Duration.ofSeconds(seconds)).pollingEvery(Duration.ofMillis(100))
                    .withMessage(description).until(file -> expected.test(records(file, checkpoint)));
        } catch (TimeoutException timeout) {
            throw new AssertionError(description + " were not observed after the action checkpoint", timeout);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> records(Path path, long checkpoint) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            Assert.assertTrue("Evidence file was truncated or the checkpoint is invalid",
                    checkpoint >= 0 && checkpoint <= bytes.length);
            String appended = new String(bytes, (int) checkpoint, bytes.length - (int) checkpoint, StandardCharsets.UTF_8);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (String line : appended.split("\\R")) {
                if (line.trim().isEmpty()) continue;
                if (!line.trim().endsWith("}")) continue; // The collector may still be writing its last line.
                try {
                    rows.add(new Json().toType(line, Map.class));
                } catch (RuntimeException invalidJson) {
                    throw new AssertionError("Bookmarks evidence must contain valid JSON objects");
                }
            }
            return rows;
        } catch (IOException failure) {
            throw new AssertionError("Cannot read fresh Bookmarks evidence", failure);
        }
    }

    private static Path readable(String pathValue) {
        Path path = Paths.get(pathValue).toAbsolutePath().normalize();
        Assert.assertTrue("Evidence file does not exist: " + path, Files.isRegularFile(path));
        Assert.assertTrue("Evidence file is not readable: " + path, Files.isReadable(path));
        return path;
    }
}
