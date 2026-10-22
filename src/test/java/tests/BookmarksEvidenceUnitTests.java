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
}
