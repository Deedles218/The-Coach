package tests;

import io.appium.java_client.InteractsWithApps;
import lib.CoreTestCase;
import lib.EmailEnforcementFixture.Stage;
import lib.Platform;
import lib.ui.android.AndroidEmailEnforcementPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.support.ui.Quotes;

import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** Explicitly selected fixture builder; excluded from the default *Tests discovery and feature suite. */
public class AndroidEmailEnforcementPreparation extends CoreTestCase {
    @Test public void prepareDedicatedAnonymousInstallation() throws Exception {
        Assert.assertTrue("Android preparation only", Platform.getInstance().isAndroid());
        Stage stage = Stage.valueOf(System.getProperty("coach.emailEnforcement.prepareStage"));
        String packageName = Platform.getInstance().getAndroidAppPackage();
        String device = System.getProperty("android.udid");
        Path directory = Paths.get(System.getProperty("coach.emailEnforcement.output"));
        Files.createDirectories(directory);
        AndroidOnboardingPageObject onboarding = new AndroidOnboardingPageObject(driver);
        onboarding.closePaywallsAndPopupsUntil(() ->
                onboarding.isElementVisible("id:" + packageName + ":id/btnSignInAnonymous"));
        onboarding.completeNewUserJourneyToPaywall(Platform.getInstance().getOnboardingGoal());
        // Preparation only uses navigation, before a measured EmailEnforcementFixture exists.
        AndroidEmailEnforcementPageObject screen = new AndroidEmailEnforcementPageObject(driver, null);
        screen.reachDailyWithoutDismissingRequest();
        Path configOutput = directory.resolve("active-config.json");
        Process configReader = new ProcessBuilder("python3", "scripts/read_android_email_enforcement.py",
                "--device", device, "--package", packageName, "--mode", "config")
                .redirectErrorStream(true).redirectOutput(configOutput.toFile()).start();
        try {
            Assert.assertTrue("Config reader timed out", configReader.waitFor(30, TimeUnit.SECONDS));
            Assert.assertEquals("Active Android configuration is unavailable", 0, configReader.exitValue());
            java.util.Map config = new org.openqa.selenium.json.Json().toType(
                    new String(Files.readAllBytes(configOutput), java.nio.charset.StandardCharsets.UTF_8), java.util.Map.class);
            Assert.assertEquals("PRECONDITION: Android email flag is OFF in activated template "
                    + config.get("templateVersion"), Boolean.TRUE, config.get("enabled"));
        } finally {
            if (configReader.isAlive()) configReader.destroyForcibly();
        }
        String title = "xpath://*[@text=" + Quotes.escape(AndroidEmailEnforcementPageObject.FORM_TITLE) + "]";
        screen.waitForElementVisible(title, "Fresh anonymous N entry did not appear", 30);
        if (stage == Stage.N) return;
        ((InteractsWithApps) driver).terminateApp(packageName);
        Path output = directory.resolve("seed-command.json");
        Process process = new ProcessBuilder("python3", "scripts/prepare_android_email_enforcement.py",
                "--device", device, "--package", packageName, "--action", "seed-existing",
                "--evidence", directory.resolve("seed.json").toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            Assert.assertTrue("Seed command timed out", process.waitFor(60, TimeUnit.SECONDS));
            Assert.assertEquals("Seed failed; see seed-command.json", 0, process.exitValue());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
        // E1/E2/E3 are reached through real cold launches, never counter writes.
        int launches = stage == Stage.E1 ? 1 : stage == Stage.E2 ? 2 : 3;
        for (int i = 0; i < launches; i++) {
            ((InteractsWithApps) driver).activateApp(packageName);
            screen.reachDailyWithoutDismissingRequest();
            screen.waitForElementVisible(title, "Seeded request did not appear", 30);
            if (i + 1 < launches) ((InteractsWithApps) driver).terminateApp(packageName);
        }
    }

    @Override protected void cleanupSessionForNextTest() { /* Retain the prepared installation. */ }
}
