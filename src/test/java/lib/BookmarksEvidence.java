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

    public static KegelDailyPlanCard readKegelCard(String capturePath, String title, String expectedLinkType) throws IOException {
        Map<?, ?> payload = new Json().toType(new String(Files.readAllBytes(readable(capturePath)),
                StandardCharsets.UTF_8), Map.class);
        Assert.assertTrue("Kegel evidence must contain the Daily Plan program_section",
                payload != null && payload.get("program_section") instanceof Map);
        Map<?, ?> section = (Map<?, ?>) payload.get("program_section");
        Assert.assertTrue("Kegel evidence must contain Daily Plan questions", section.get("questions") instanceof List);
        List<Map<?, ?>> matching = new ArrayList<>();
        for (Object row : (List<?>) section.get("questions")) {
            if (row instanceof Map && title.equals(((Map<?, ?>) row).get("headline"))) {
                matching.add((Map<?, ?>) row);
            }
        }
        Assert.assertEquals("Expected one Daily Plan card named " + title, 1, matching.size());
        Map<?, ?> card = matching.get(0);
        Assert.assertTrue("Kegel card has no typed link: " + title, card.get("link_type") instanceof Map);
        Map<?, ?> links = (Map<?, ?>) card.get("link_type");
        Assert.assertTrue("Expected link_type." + expectedLinkType + " for " + title,
                links.get(expectedLinkType) instanceof Map && !((Map<?, ?>) links.get(expectedLinkType)).isEmpty());
        for (String type : new String[]{"kegel", "kegel_sport", "kegel_workout"}) {
            Assert.assertFalse("Kegel fixture has a second workout type: " + type,
                    !expectedLinkType.equals(type) && links.containsKey(type));
        }

        String program = nonemptyText(section.get("headline"), "Kegel evidence must name the Daily Plan program");
        String programId = nonemptyText(section.get("section_id"), "Kegel fixture must identify its program");
        int catalogDay = positiveNumber(section.get("day_in_program"));
        String id = nonemptyText(card.get("name"), "Kegel evidence must identify the workout card");
        boolean moduleProgram = Boolean.TRUE.equals(section.get("module_program"));
        int day = positiveNumber(section.get(moduleProgram ? "module_current_day" : "day_in_program"));
        String dayLabel = moduleProgram ? "Stage " + day + " of " + positiveNumber(section.get("module_total_days")) : "Day " + day;
        ModuleStage position = moduleProgram ? ModuleStage.parse(nonemptyText(section.get("module_name"),
                "Kegel fixture must identify its module"), dayLabel) : null;
        return new KegelDailyPlanCard(title, id, program, programId, day, catalogDay, dayLabel, expectedLinkType, position);
    }

    public static void assertCurrentKegelCard(org.openqa.selenium.remote.RemoteWebDriver driver,
                                             KegelDailyPlanCard card, long boundary, String email) throws Exception {
        Platform platform = Platform.getInstance();
        String device = platform.isIOS() ? SimulatorTestIdentity.deviceUdid() : AndroidDevice.appPrivateEvidenceSerial();
        UpdateEvidence.assertAppiumDevice(driver, device);
        String app = platform.isIOS() ? platform.getIOSBundleId() : platform.getAndroidAppPackage();
        String capability = platform.isIOS() ? "bundleId" : "appPackage";
        Object selectedApp = driver.getCapabilities().getCapability("appium:" + capability);
        if (selectedApp == null) selectedApp = driver.getCapabilities().getCapability(capability);
        Assert.assertEquals("Kegel evidence must target the app selected by Appium", app, selectedApp);
        if (card.position != null) Assert.assertEquals("Kegel fixture belongs to another UI module/stage",
                card.position, lib.ui.factories.ModulesPageObjectFactory.get(driver).position());
        Map<String, String> build = platform.isIOS() ? UpdateEvidence.readInstalledIOSBuild(driver)
                : UpdateEvidence.androidInstalledBuild(AndroidDevice.adbForSerial(device, "shell", "dumpsys", "package", app));
        String uid = System.getenv("COACH_EXPECTED_ACCOUNT_UID");
        Assert.assertTrue("Kegel evidence requires the approved fixture UID", uid != null && uid.matches("[A-Za-z0-9_-]{20,128}"));
        Path output = Files.createTempFile("coach-kegel-live-", ".json");
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder("python3", "scripts/read_bookmarks_kegel.py",
                    "--platform", platform.isIOS() ? "ios" : "android", "--device", device, "--app", app,
                    "--program", card.programId, "--day", Integer.toString(card.catalogDay), "--card-id", card.id,
                    "--title", card.title, "--link-type", card.linkType, "--day-label", card.dayLabel,
                    "--module-order", Integer.toString(card.position == null ? 0 : card.position.module),
                    "--version", build.get("version"), "--build", build.get("build"), "--boundary", Long.toString(boundary));
            builder.environment().put("COACH_EXPECTED_PROGRAM_UID", uid);
            builder.environment().put("COACH_BOOKMARKS_KEGEL_EXPECTED_EMAIL", email);
            process = builder.redirectErrorStream(true).redirectOutput(output.toFile()).start();
            Assert.assertTrue("Current Kegel evidence timed out", process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS));
            Assert.assertEquals("Current Kegel evidence rejected the fixture/account/build/card", 0, process.exitValue());
            String proof = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
            io.qameta.allure.Allure.addAttachment("Current Kegel catalog corroboration", "application/json", proof, "json");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }

    private static String nonemptyText(Object value, String error) {
        Assert.assertTrue(error, value instanceof String && !((String) value).trim().isEmpty());
        return ((String) value).trim();
    }

    private static int positiveNumber(Object value) {
        Assert.assertTrue("Daily Plan day information must be numeric", value instanceof Number);
        int number = ((Number) value).intValue();
        Assert.assertTrue("Daily Plan day information must be a positive integer", number > 0 && numberEquals(value, number));
        return number;
    }

    public static final class KegelDailyPlanCard {
        public final String title;
        public final String id;
        public final String program;
        public final String programId;
        public final int day;
        public final int catalogDay;
        public final String dayLabel;
        public final String linkType;
        public final ModuleStage position;

        private KegelDailyPlanCard(String title, String id, String program, String programId, int day,
                                  int catalogDay, String dayLabel, String linkType, ModuleStage position) {
            this.title = title;
            this.id = id;
            this.program = program;
            this.programId = programId;
            this.day = day;
            this.catalogDay = catalogDay;
            this.dayLabel = dayLabel;
            this.linkType = linkType;
            this.position = position;
        }
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
