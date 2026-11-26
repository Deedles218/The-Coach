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
        for (String role : new String[]{"baseline", "content", "tooltip", "recurring", "switcha", "switchb", "update",
                "nosubscription", "activesubscription", "activetrial"}) {
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

    @Test
    public void selectedCompletionAndKegelRolesAreRedacted() {
        for (String selector : new String[]{"completion", "kegel", "available", "savedSlide"}) {
            String option = "coach.bookmarks." + selector + "AccountRole";
            String prefix = "coach.bookmarks.freshfixture";
            String oldRole = System.getProperty(option);
            String oldEmail = System.getProperty(prefix + ".email");
            String oldOtp = System.getProperty(prefix + ".otp");
            try {
                System.setProperty(option, "Fresh-Fixture");
                System.setProperty(prefix + ".email", "fresh@example.invalid");
                System.setProperty(prefix + ".otp", "fresh-fixture-code");
                Assert.assertEquals("fresh@example.invalid", TestData.bookmarksAccount("Fresh-Fixture").getEmail());
                Assert.assertEquals("<redacted-email> <redacted-otp>",
                        TestData.sanitizeSensitiveData("fresh@example.invalid fresh-fixture-code"));
                Assert.assertEquals("<redacted-email> <redacted-otp>",
                        TestData.sanitizeSensitiveData("FRESH@EXAMPLE.INVALID fresh-fixture-code"));
            } finally {
                restore(option, oldRole);
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
    public void savedSlideEnvironmentRoleRedactsItsCredentialsInASeparateJvm() throws Exception {
        Path output = Files.createTempFile("bookmarks-role-redaction-", ".txt");
        Process process = null;
        try {
            String javaExecutable = java.nio.file.Paths.get(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder command = new ProcessBuilder(javaExecutable, "-cp",
                    System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    RoleRedactionProbe.class.getName());
            command.environment().put("COACH_BOOKMARKS_SAVED_SLIDE_ACCOUNT_ROLE", "ExternalFixture");
            command.environment().put("COACH_BOOKMARKS_EXTERNALFIXTURE_EMAIL", "external@example.invalid");
            command.environment().put("COACH_BOOKMARKS_EXTERNALFIXTURE_OTP", "external-fixture-code");
            process = command.redirectErrorStream(true).redirectOutput(output.toFile()).start();
            Assert.assertTrue("Role redaction probe timed out", process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
            Assert.assertEquals("Environment-selected saved-slide credentials were not protected", 0, process.exitValue());
            Assert.assertEquals("protected", new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }

    public static class RoleRedactionProbe {
        public static void main(String[] args) {
            String sanitized = TestData.sanitizeSensitiveData("EXTERNAL@EXAMPLE.INVALID external-fixture-code");
            if (!"<redacted-email> <redacted-otp>".equals(sanitized)) {
                throw new AssertionError("Environment-selected role credentials were not redacted");
            }
            System.out.print("protected");
        }
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

    @Test
    public void capturedKegelCardTypesAndProgramPositionsAreVerified() throws Exception {
        Path capture = Files.createTempFile("bookmarks-kegel", ".json");
        try {
            writeKegelCapture(capture, "\"kegel\":{\"workout_id\":\"classic\"}");
            BookmarksEvidence.KegelDailyPlanCard classic = BookmarksEvidence.readKegelCard(capture.toString(), "Workout", "kegel");
            Assert.assertEquals("workout-card", classic.id);
            Assert.assertEquals("Kegel Challenge", classic.program);
            Assert.assertEquals("kegel_challenge", classic.programId);
            Assert.assertEquals(2, classic.day);
            Assert.assertEquals(4, classic.catalogDay);
            Assert.assertEquals("Stage 2 of 9", classic.dayLabel);
            Assert.assertEquals(new lib.ModuleStage(2, 2, 9), classic.position);

            writeKegelCapture(capture, "\"kegel_sport\":{\"workout_id\":\"resistance\"}");
            BookmarksEvidence.readKegelCard(capture.toString(), "Workout", "kegel_sport");
            assertIncorrectKegelCapture(capture, "kegel", "Expected link_type.kegel");
        } finally {
            Files.deleteIfExists(capture);
        }
    }

    @Test
    public void modernOrConflictingWorkoutCannotSatisfyLegacyKegelFixture() throws Exception {
        Path capture = Files.createTempFile("bookmarks-kegel-ambiguous", ".json");
        try {
            writeKegelCapture(capture, "\"kegel_workout\":{\"workout_id\":\"modern\"}");
            assertIncorrectKegelCapture(capture, "kegel", "Expected link_type.kegel");
            writeKegelCapture(capture, "\"kegel\":{\"workout_id\":\"classic\"},\"kegel_workout\":{\"workout_id\":\"modern\"}");
            assertIncorrectKegelCapture(capture, "kegel", "second workout type");
        } finally {
            Files.deleteIfExists(capture);
        }
    }

    private void writeKegelCapture(Path capture, String links) throws Exception {
        String body = "{\"program_section\":{\"headline\":\"Kegel Challenge\",\"section_id\":\"kegel_challenge\",\"day_in_program\":4,\"module_program\":true,"
                + "\"module_current_day\":2,\"module_total_days\":9,\"module_name\":\"Module 2: Workouts\",\"questions\":["
                + "{\"headline\":\"Workout\",\"name\":\"workout-card\",\"link_type\":{" + links + "}}]}}";
        Files.write(capture, body.getBytes(StandardCharsets.UTF_8));
    }

    private void assertIncorrectKegelCapture(Path capture, String type, String message) throws Exception {
        try {
            BookmarksEvidence.readKegelCard(capture.toString(), "Workout", type);
            Assert.fail("Incorrect workout type was accepted");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.toString(), expected.getMessage().contains(message));
        }
    }
}
