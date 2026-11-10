package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.json.Json;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** One independently prepared COA-9222 dataset. Credentials never belong in this file. */
public final class EmailEnforcementFixture {
    public enum Stage {
        N, E1, E2, E3;
        public boolean dismissible() { return this == E1 || this == E2; }
        public Stage next() { return this == E1 ? E2 : this == E2 ? E3 : this; }
    }

    private final Properties values = new Properties();
    private final String device;
    private final boolean ios = Platform.getInstance().isIOS();

    public EmailEnforcementFixture(String method) throws Exception {
        String path = System.getProperty("coach.emailEnforcement.fixture");
        String directory = System.getProperty("coach.emailEnforcement.fixtures");
        if (directory != null) path = Paths.get(directory, method + ".properties").toString();
        if (path == null) throw new IllegalStateException("Supply coach.emailEnforcement.fixture; see docs/ios-email-enforcement.md");
        try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) { values.load(reader); }
        String number = method.replaceFirst("^testCoa([0-9]+).*$", "$1");
        Assert.assertEquals("A prepared dataset cannot be reused by another case", "COA-" + number, required("case"));
        Assert.assertEquals("Fixture belongs to another installation",
                ios ? Platform.getInstance().getIOSBundleId() : Platform.getInstance().getAndroidAppPackage(), appId());
        String platform = ios ? "ios" : "android";
        device = System.getProperty(platform + ".udid", System.getenv(platform.toUpperCase(Locale.ROOT) + "_UDID"));
        Assert.assertTrue("Supply the explicit test device UDID", device != null && !device.trim().isEmpty());
        Assert.assertTrue("Entry must be prepared or new-user", Arrays.asList("prepared", "new-user").contains(required("entry")));
        if (newUser()) Assert.assertEquals("New-user entry must expect the mandatory N form", Stage.N, stage());
        else secret("COACH_EMAIL_ENFORCEMENT_UID");
        Assert.assertFalse("Stage tests require fullReset=false", Platform.getInstance().isCleanInstallConfigured()
                || Boolean.parseBoolean(System.getProperty(platform + ".fullReset", System.getenv(platform.toUpperCase(Locale.ROOT) + "_FULL_RESET"))));
        Assert.assertEquals("Stage tests require noReset=true", "true",
                System.getProperty(platform + ".noReset", Optional.ofNullable(System.getenv(platform.toUpperCase(Locale.ROOT) + "_NO_RESET")).orElse("true")));
        required("form.title");
        required("stage." + stage() + ".message");
        required("daily.content");
        required("app.version");
        required("app.build");
        if ("COA-9696".equals(required("case"))) {
            Assert.assertEquals("Progressive enforcement needs a never-shown E1 fixture", Stage.E1, stage());
            required("dismiss.action");
            required("stage.E2.message");
            required("stage.E3.message");
        }
        if (Arrays.asList("COA-9697", "COA-9698").contains(required("case"))) controlPath();
        if ("COA-9697".equals(required("case"))) {
            Assert.assertTrue("Declare the original legacy A/B flag", Arrays.asList("true", "false").contains(required("legacy.abEnabled")));
            Assert.assertTrue("Declare the applicable old flow", Arrays.asList("daily", "dismissible-form").contains(required("legacy.outcome")));
        }
        if ("COA-9700".equals(required("case"))) required("update.app");
        if (Arrays.asList("COA-9411", "COA-9415", "COA-9702").contains(required("case"))) {
            TestData.validEmailWithoutProgress();
            TestData.existingProgressAccount(); // Existing project secret sources and artifact redaction.
        }
        if ("COA-9424".equals(required("case"))) TestData.existingProgressAccount();
        if (Arrays.asList("COA-9699", "COA-9700", "COA-9701").contains(required("case"))) {
            required("stage." + stage().next() + ".message");
        }
        if ("COA-9701".equals(required("case"))) required("otherTab.content");
        if ("COA-9698".equals(required("case"))) {
            required("stage.E2.message");
            required("stage.E3.message");
            required("daily.unavailable");
        }
        if ("COA-9702".equals(required("case"))) {
            for (String key : new String[]{"form.support", "support.destination", "support.back"}) required(key);
        }
        if ("COA-9411".equals(required("case")) && !stage().dismissible()) {
            required("otp.changeEmail");
            required("email.placeholder");
        }
        if ("COA-9415".equals(required("case"))) required("submission.pending");
        if (Arrays.asList("COA-9696", "COA-9698", "COA-9701").contains(required("case"))
                || ("COA-9699".equals(required("case")) && stage() == Stage.E1)) {
            for (String key : new String[]{"title", "message", "add", "later"}) required("stage.E2.warning." + key);
        }
    }

    public String required(String key) {
        String value = values.getProperty(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Email fixture missing: " + key);
        return value.trim();
    }

    public String value(String key, String fallback) { return values.getProperty(key, fallback).trim(); }
    public Stage stage() { return Stage.valueOf(required("stage")); }
    public boolean newUser() { return "new-user".equals(required("entry")); }
    public String appId() { return required(ios ? "bundleId" : "appPackage"); }

    public Map<String, Object> evidence(String mode) throws Exception { return evidence(mode, null); }

    public Map<String, Object> evidence(String mode, String app) throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList("python3",
                ios ? "scripts/read_ios_email_enforcement.py" : "scripts/read_android_email_enforcement.py",
                "--device", device, ios ? "--bundle" : "--package", appId(), "--mode", mode));
        if (app != null) args.addAll(Arrays.asList("--app", app));
        Map<String, Object> state = new Json().toType(run(args), Map.class);
        Allure.addAttachment("Email enforcement " + mode + " evidence", "application/json", new Json().toJson(state), "json");
        return state;
    }

    public void requireEnabled() throws Exception {
        Map<String, Object> config = evidence("config");
        Assert.assertEquals("COA-9222 is disabled in the active app config", Boolean.TRUE, config.get("enabled"));
        Assert.assertEquals("Fixture belongs to a different app version", required("app.version"), config.get("version"));
        Assert.assertEquals("Fixture belongs to a different app build", required("app.build"), config.get("build"));
        if ("COA-9697".equals(required("case"))) {
            Assert.assertEquals("Legacy A/B flag differs from the reviewed dataset",
                    Boolean.valueOf(required("legacy.abEnabled")), config.get("legacyEnabled"));
        }
    }

    public void control(String action) throws Exception {
        run(Arrays.asList(controlPath(), action, device, appId()));
    }

    private String controlPath() {
        String path = secret("COACH_EMAIL_ENFORCEMENT_CONTROL");
        Assert.assertTrue("The environment-owner control must be executable", Files.isExecutable(Paths.get(path)));
        return path;
    }

    public static String secret(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing secret-backed fixture variable: " + name);
        return value;
    }

    public static void assertPreserved(Map<String, Object> before, Map<String, Object> after) {
        for (String key : new String[]{"identity", "subscriptionActive", "progress"}) {
            Assert.assertNotNull("Missing baseline " + key, before.get(key));
            Assert.assertEquals("Email flow changed " + key, before.get(key), after.get(key));
        }
    }

    public static void assertUnlinked(Map<String, Object> state) {
        Assert.assertEquals("Fixture already has an email", Boolean.FALSE, state.get("emailPresent"));
        Assert.assertEquals("Fixture already has a confirmed email", Boolean.FALSE, state.get("emailVerified"));
    }

    public static void assertNewerBuild(Map<String, Object> before, Map<String, Object> update) {
        Assert.assertEquals("Update belongs to another bundle", before.get("bundleId"), update.get("bundleId"));
        Assert.assertEquals("Update targets another iOS platform", before.get("platform"), update.get("platform"));
        String[] oldParts = before.get("build").toString().split("\\.");
        String[] newParts = update.get("build").toString().split("\\.");
        for (int i = 0; i < Math.max(oldParts.length, newParts.length); i++) {
            int difference = Integer.compare(i < newParts.length ? Integer.parseInt(newParts[i]) : 0,
                    i < oldParts.length ? Integer.parseInt(oldParts[i]) : 0);
            if (difference > 0) return;
            if (difference < 0) break;
        }
        Assert.fail("COA-9700 requires a strictly newer build, not a reinstall of the same artifact");
    }

    private static String run(List<String> args) throws Exception {
        Path output = Files.createTempFile("coach-email-reader-", ".txt");
        Process process = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            Assert.assertTrue("Email fixture command timed out", process.waitFor(90, TimeUnit.SECONDS));
            Assert.assertEquals("Email fixture command failed; check device and fixture prerequisites", 0, process.exitValue());
            return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }
}
