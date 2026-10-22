package tests;

import lib.BookmarksEvidence;
import lib.TestData;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class BookmarksEvidenceUnitTests {
    @Test
    public void oldEventsCannotSatisfyCurrentAction() throws Exception {
        Path log = Files.createTempFile("bookmarks-evidence", ".log");
        try {
            Files.write(log, "BookmarkSaved".getBytes(StandardCharsets.UTF_8));
            long checkpoint = BookmarksEvidence.checkpoint(log.toString());
            try {
                BookmarksEvidence.assertAppendedContains(log.toString(), checkpoint, "BookmarkSaved");
                Assert.fail("Stale event was accepted");
            } catch (AssertionError expected) {
                Assert.assertTrue(expected.getMessage().contains("Fresh evidence does not contain"));
            }
            Files.write(log, "\nBookmarkSaved lesson-42".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            BookmarksEvidence.assertAppendedContains(log.toString(), checkpoint, "BookmarkSaved", "lesson-42");
        } finally {
            Files.deleteIfExists(log);
        }
    }

    @Test
    public void truncatedEvidenceIsRejected() throws Exception {
        Path log = Files.createTempFile("bookmarks-evidence", ".log");
        try {
            Files.write(log, "old captured request".getBytes(StandardCharsets.UTF_8));
            long checkpoint = BookmarksEvidence.checkpoint(log.toString());
            Files.write(log, new byte[0]);
            try {
                BookmarksEvidence.assertAppendedContains(log.toString(), checkpoint, "POST");
                Assert.fail("Truncated capture was accepted");
            } catch (AssertionError expected) {
                Assert.assertTrue(expected.getMessage().contains("was truncated"));
            }
        } finally {
            Files.deleteIfExists(log);
        }
    }

    @Test
    public void everyBookmarksRoleIsRedactedAndUsesItsOwnCredentials() {
        for (String role : new String[]{"baseline", "content", "tooltip", "recurring", "switcha", "switchb", "update", "nosubscription"}) {
            String prefix = "coach.bookmarks." + role;
            String oldEmail = System.getProperty(prefix + ".email");
            String oldOtp = System.getProperty(prefix + ".otp");
            try {
                System.setProperty(prefix + ".email", role + "@example.invalid");
                System.setProperty(prefix + ".otp", "fixture-code-" + role);
                Assert.assertEquals(role + "@example.invalid", TestData.bookmarksAccount(role).getEmail());
                Assert.assertEquals("<redacted-email> <redacted-otp>", TestData.sanitizeSensitiveData(
                        role + "@example.invalid fixture-code-" + role));
            } finally {
                restore(prefix + ".email", oldEmail);
                restore(prefix + ".otp", oldOtp);
            }
        }
    }

    private void restore(String property, String value) {
        if (value == null) System.clearProperty(property);
        else System.setProperty(property, value);
    }

    @Test
    public void timestampDigitsCannotSatisfyWrongSlideOrSeparateEventProperties() throws Exception {
        Path log = Files.createTempFile("bookmarks-structured", ".jsonl");
        String property = "coach.bookmarks.evidenceTimeoutSeconds";
        String previous = System.getProperty(property);
        try {
            System.setProperty(property, "0");
            String records = "{\"event\":\"BookmarksOpened\",\"timestamp\":20261022}\n"
                    + "{\"event\":\"BookmarkSaved\",\"properties\":{\"lesson_id\":\"lesson-a\",\"slide_number\":1}}\n"
                    + "{\"event\":\"BookmarkDeleted\",\"properties\":{\"lesson_id\":\"lesson-a\",\"slide_number\":1}}\n";
            Files.write(log, records.getBytes(StandardCharsets.UTF_8));
            try {
                BookmarksEvidence.assertBookmarkAnalytics(log.toString(), 0, "lesson-a", 2);
                Assert.fail("Timestamp digits satisfied the expected slide");
            } catch (AssertionError expected) {
                Assert.assertTrue(expected.getMessage().contains("were not observed"));
            }
            BookmarksEvidence.assertBookmarkAnalytics(log.toString(), 0, "lesson-a", 1);
        } finally {
            restore(property, previous);
            Files.deleteIfExists(log);
        }
    }

    @Test
    public void bookmarkRequestFieldsMustBelongToOneFreshRequest() throws Exception {
        Path log = Files.createTempFile("bookmarks-request", ".jsonl");
        String property = "coach.bookmarks.evidenceTimeoutSeconds";
        String previous = System.getProperty(property);
        try {
            System.setProperty(property, "0");
            Files.write(log, ("{\"method\":\"POST\",\"path\":\"/api/v1/coachprogram/lessons/lesson-a/bookmark\","
                    + "\"body\":{\"program_id\":\"program-a\",\"program_day\":1,\"slide_number\":2}}\n")
                    .getBytes(StandardCharsets.UTF_8));
            long checkpoint = BookmarksEvidence.checkpoint(log.toString());
            Files.write(log, ("{\"method\":\"POST\",\"path\":\"/api/v1/coachprogram/lessons/lesson-b/bookmark\","
                    + "\"body\":{\"program_id\":\"program-a\",\"program_day\":1,\"slide_number\":2}}\n")
                    .getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            try {
                BookmarksEvidence.assertBookmarkRequest(log.toString(), checkpoint, "lesson-a", "program-a", 1, 2);
                Assert.fail("An old matching request satisfied the current action");
            } catch (AssertionError expected) {
                Assert.assertTrue(expected.getMessage().contains("were not observed"));
            }
            Files.write(log, ("{\"method\":\"POST\",\"path\":\"/api/v1/coachprogram/lessons/lesson-a/bookmark\","
                    + "\"body\":{\"program_id\":\"program-a\",\"program_day\":1,\"slide_number\":2}}\n")
                    .getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            BookmarksEvidence.assertBookmarkRequest(log.toString(), checkpoint, "lesson-a", "program-a", 1, 2);
        } finally {
            restore(property, previous);
            Files.deleteIfExists(log);
        }
    }
}
