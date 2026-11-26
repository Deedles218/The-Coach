package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.json.Json;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** UID-verified checks; production opt-in permits fixture reads only. */
public final class ProgramSelectionApi {
    private ProgramSelectionApi() { }

    public static void assertRejectedTransition(String operation) throws Exception {
        Assert.assertTrue("Unsupported program selection check", operation.matches(
                "secondary-includes-main|activate-unadded|complete-without-main"));
        runCheck(operation);
    }

    public static void assertFreshOnboardingProfile() throws Exception {
        runCheck("new-onboarding-profile");
    }

    public static void assertFreshSecondaryDraft(String mainId) throws Exception {
        Assert.assertTrue("Set coach.programSelection.mainId for the prepared draft", mainId != null
                && mainId.matches("[a-z0-9_]{1,80}"));
        runCheck("secondary-onboarding-profile", "--program", mainId);
    }

    /** Call before any UI action that can save a draft, activate or complete content. */
    public static void assertDedicatedProfile(String caseKey) throws Exception {
        Assert.assertEquals("Bind a task-owned QA profile to this case with COACH_PROGRAM_SELECTION_CASE",
                caseKey, System.getenv("COACH_PROGRAM_SELECTION_CASE"));
        if (Platform.getInstance().isIOS()) {
            String uid = System.getenv("COACH_PROGRAM_SELECTION_UID");
            Assert.assertTrue("A dedicated QA UID binding is required", uid != null
                    && uid.matches("[A-Za-z0-9_-]{20,128}"));
        } else {
            Assert.assertTrue("Android observations require a reviewed progress or cross-platform fixture",
                    Arrays.asList("COA-9794", "COA-9795").contains(caseKey));
            Assert.assertNotNull("Bind Android to the iOS backend identity", System.getenv("COACH_PROGRAM_SELECTION_BACKEND_HASH"));
        }
        read(); // The session reader verifies the device and API identities against the binding.
    }

    public static ProgramSelectionState read() throws Exception {
        return new ProgramSelectionState((Map<String, Object>) runCheck("observe").get("original"));
    }

    public static ProgramSelectionState activateFirstProgram(String id) throws Exception {
        Assert.assertTrue("Invalid program_id", id != null && id.matches("[a-z0-9_]{1,80}"));
        assertDedicatedProfile("COA-9797");
        return new ProgramSelectionState((Map<String, Object>) runCheck("first-activation", "--program", id).get("after"));
    }

    public static Map<String, Object> recommendation() throws Exception {
        return (Map<String, Object>) runCheck("observe", "--recommendation").get("recommendation");
    }

    public static Map<String, Object> detail(String programId) throws Exception {
        Assert.assertTrue("Invalid program_id", programId != null && programId.matches("[a-z0-9_]{1,80}"));
        return (Map<String, Object>) runCheck("observe", "--program", programId).get("detail");
    }

    public static List<Map<String, Object>> catalog() throws Exception {
        return (List<Map<String, Object>>) runCheck("observe").get("catalog");
    }

    public static Map<String, Object> exchange(String route, long notBefore) throws Exception {
        Assert.assertTrue("Unsupported selection exchange", Arrays.asList("main_journey/", "secondary_programs/",
                "onboarding/complete/", "active_program/").contains(route)
                || route != null && route.matches("[a-z0-9_]{1,80}/add_and_activate/"));
        return (Map<String, Object>) runCheck("observe", "--exchange", route,
                "--not-before-ms", String.valueOf(notBefore)).get("exchange");
    }

    public static int contentStatus(String observedRoute) throws Exception {
        Assert.assertTrue("Supply the observed lesson/practice endpoint", observedRoute != null
                && observedRoute.matches("/api/v1/coachprogram/(lessons|exercises|actions)/[A-Za-z0-9_-]{1,128}/?"));
        Map<String, Object> data = (Map<String, Object>) runCheck("observe", "--content-route", observedRoute).get("content_access");
        return ((Number) data.get("http_status")).intValue();
    }

    private static Map<String, Object> runCheck(String operation, String... options) throws Exception {
        Platform platform = Platform.getInstance();
        Assert.assertTrue("Program selection evidence needs a mobile platform", platform.isIOS() || platform.isAndroid());
        Assert.assertTrue("Android supports read-only observations", platform.isIOS() || "observe".equals(operation));
        String bundle = platform.isIOS() ? platform.getIOSBundleId() : platform.getAndroidAppPackage();
        boolean production = "com.vamapps.The-Coach".equals(bundle)
                || "com.vamapps.The-Coach-for-her".equals(bundle);
        Assert.assertTrue("API checks require preprod or an explicitly approved production fixture read",
                platform.isAndroid() || bundle != null && (bundle.startsWith("com.vamapps.preprod.")
                        || production && "new-onboarding-profile".equals(operation)
                        && Boolean.getBoolean("coach.programSelection.allowProduction")));
        String prefix = platform.isIOS() ? "ios" : "android";
        String device = System.getProperty(prefix + ".udid", System.getenv(prefix.toUpperCase(java.util.Locale.ROOT) + "_UDID"));
        Assert.assertNotNull("API checks require an explicit device", device);
        Files.createDirectories(Paths.get("target"));
        Path folder = Files.createTempDirectory(Paths.get("target"), "program-selection-api-");
        Path result = folder.resolve("result.json"), log = folder.resolve("check.log");
        List<String> command = new ArrayList<>(Arrays.asList("python3", "scripts/check_program_selection_api.py", operation,
                "--device", device, "--bundle", bundle, "--output", result.toString()));
        command.addAll(Arrays.asList("--platform", prefix));
        if (production) command.add("--allow-production-read");
        command.addAll(Arrays.asList(options));
        Process check = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            Assert.assertTrue("Program selection check timed out; see " + folder,
                    check.waitFor(150, TimeUnit.SECONDS));
            Assert.assertEquals("Program selection check failed; see " + folder, 0, check.exitValue());
            return new Json().toType(new String(Files.readAllBytes(result), StandardCharsets.UTF_8), Map.class);
        } finally {
            if (check.isAlive()) check.destroyForcibly();
            if (Files.exists(result)) {
                try (InputStream data = Files.newInputStream(result)) {
                    Allure.addAttachment("Program selection profile evidence", "application/json", data, "json");
                }
            }
        }
    }
}
