package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Feature;
import lib.EmailEnforcementFixture;
import lib.Platform;
import lib.RestoringTestAction;
import lib.RestoringTestAction.CheckedAction;
import lib.TestData;
import lib.TestFlightHandoff;
import lib.ui.ios.IOSEmailReleasePageObject;
import lib.ui.ios.iOSOnboardingPageObject;
import lib.ui.ios.iOSPartnerRecipientOnboardingPageObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.runner.Description;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Feature("iOS email release regression")
public class IOSEmailReleaseRegression extends EmailReleaseRegression {
    private static final String TESTFLIGHT_BUNDLE = "com.apple.TestFlight";
    private String bundle;
    private String device;
    private String goal;
    private String language = "en";
    private Map<String, Object> target;
    private Path testFlightDirectory;
    private IOSEmailReleasePageObject screen;
    private iOSOnboardingPageObject onboarding;

    @Before
    @Override public void setUp() throws Exception {
        Assume.assumeTrue(unsupportedPlatformMessage(), isPlatformSupported());
        bundle = Platform.getInstance().getIOSBundleId();
        device = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertTrue("Select a dedicated iOS test device explicitly",
                device != null && device.equals(System.getProperty("coach.emailRelease.dedicatedDevice")));
        Assert.assertEquals("Keep this installation under suite control", "logout", TestData.testIsolationMode());
        Assert.assertNull("Use an already installed iOS app for the initial Appium session", Platform.getInstance().getIOSAppPath());
        Assert.assertTrue("Set ios.noReset=true and ios.fullReset=false",
                capability("ios.noReset", "IOS_NO_RESET", true) && !capability("ios.fullReset", "IOS_FULL_RESET", false));
        validateLanguage("en|es");
        goal = System.getProperty("coach.emailRelease.iosGoal",
                bundle.endsWith("-for-her") ? "Cultivate desire" : "Beat premature ejaculation");
        String handoff = System.getProperty("coach.ios.testFlightHandoff", "").trim();
        if (handoff.isEmpty()) {
            target = artifact("coach.emailRelease.updateApp");
            Assert.assertEquals("Install the target iOS build before the run", target, evidence("build", null));
        } else {
            String build = System.getProperty("coach.run.appBuild", "");
            Assert.assertTrue("Set coach.run.appBuild to the target TestFlight build", build.matches("[0-9]+"));
            testFlightDirectory = Files.createTempDirectory(Files.createDirectories(Paths.get(handoff)),
                    testName.getMethodName() + "-");
        }
        super.setUp();
        Object actual = driver.getCapabilities().getCapability("appium:udid");
        if (actual == null) actual = driver.getCapabilities().getCapability("udid");
        Assert.assertEquals("Appium must use the selected test device", device, actual);
        Object sessionBundle = driver.getCapabilities().getCapability("appium:bundleId");
        if (sessionBundle == null) sessionBundle = driver.getCapabilities().getCapability("bundleId");
        Assert.assertEquals("Appium must attach to the selected session bundle",
                testFlightDirectory == null ? bundle : TESTFLIGHT_BUNDLE, sessionBundle);
        onboarding = bundle.endsWith("-for-her")
                ? new iOSPartnerRecipientOnboardingPageObject(driver) : new iOSOnboardingPageObject(driver);
        if (testFlightDirectory == null) {
            resetInstallation(appPath("coach.emailRelease.updateApp"), target);
        } else {
            handoff("prepare-clean-rc");
            target = evidence("build", null);
            Assert.assertEquals("Unexpected target TestFlight build", System.getProperty("coach.run.appBuild"),
                    target.get("build"));
            String version = System.getProperty("coach.run.appVersion");
            if (version != null) Assert.assertEquals("Unexpected target TestFlight version", version, target.get("version"));
            launchInEnglish();
        }
    }

    @Override protected boolean isPlatformSupported() { return Platform.getInstance().isIOS(); }

    @Override protected IOSEmailReleasePageObject screen() { return screen; }

    @Override protected int defaultFirstRequestLaunch() { return 4; }

    @Override protected void startNewUserOnboarding() {
        screen.startFromPurchaseLogin();
        finishOnboarding(true);
    }

    @Override protected void finishOnboarding(boolean requirePaywall) {
        onboarding.selectGoal(goal);
        onboarding.completeQuestionnaire();
        int closed = screen.closeOffersBeforeEmailOrDaily();
        if (requirePaywall) Assert.assertTrue("Onboarding did not pass through a paywall", closed > 0);
    }

    @Override protected void withPreviousRelease(CheckedAction scenario) throws Exception {
        RestoringTestAction.run(() -> {
            try {
                if (testFlightDirectory == null) {
                    Map<String, Object> previous = artifact("coach.emailRelease.previousApp");
                    EmailEnforcementFixture.assertNewerBuild(previous, target);
                    resetInstallation(appPath("coach.emailRelease.previousApp"), previous);
                } else {
                    handoff("install-previous");
                    EmailEnforcementFixture.assertNewerBuild(evidence("build", null), target);
                    launchInEnglish();
                }
                startNewUserOnboarding();
                screen.waitForDaily();
                screen.assertNoRequest();
                scenario.run();
            } catch (Exception | AssertionError failure) {
                captureFailureArtifacts(failure, Description.createTestDescription(getClass(),
                        testName.getMethodName() + "BeforeRestore"));
                throw failure;
            }
        }, () -> {
            if (!apps().isAppInstalled(bundle) || !target.equals(evidence("build", null))) {
                if (testFlightDirectory == null) {
                    installUpdate();
                } else {
                    handoff("ensure-rc");
                    Assert.assertEquals("Unexpected iOS build after restoration", target, evidence("build", null));
                }
            }
        });
    }

    @Override protected void installUpdate() throws Exception {
        if (apps().isAppInstalled(bundle)) apps().terminateApp(bundle);
        if (testFlightDirectory == null) apps().installApp(appPath("coach.emailRelease.updateApp"));
        else handoff("install-rc");
        Assert.assertEquals("Unexpected iOS build after in-place update", target, evidence("build", null));
    }

    @Override protected void switchLanguage(String language) {
        useLanguage(language);
        coldLaunch();
    }

    @Override protected void returnToWelcomeForLogin() throws Exception {
        // Reinstalling iOS alone can leave the authenticated session in Keychain.
        screen.ensureLoggedOutOnStartScreen();
        if (testFlightDirectory == null) {
            resetInstallation(appPath("coach.emailRelease.updateApp"), target);
        } else {
            handoff("reinstall-rc");
            Assert.assertEquals("Unexpected iOS build after reinstall", target, evidence("build", null));
            launchInEnglish();
        }
    }

    @Override protected void coldLaunch() {
        apps().terminateApp(bundle);
        launchWithLanguage();
    }

    private void resetInstallation(String path, Map<String, Object> expectedBuild) throws Exception {
        apps().terminateApp(bundle);
        Assert.assertTrue("Could not remove the selected test installation", apps().removeApp(bundle));
        apps().installApp(path);
        Assert.assertEquals("Unexpected iOS build after reinstall", expectedBuild, evidence("build", null));
        launchInEnglish();
    }

    private void handoff(String phase) {
        TestFlightHandoff.perform(driver, phase, testFlightDirectory);
    }

    private void launchInEnglish() {
        useLanguage("en");
        launchWithLanguage();
    }

    private void useLanguage(String language) {
        this.language = language;
        screen = new IOSEmailReleasePageObject(driver, language, onboarding);
    }

    private void launchWithLanguage() {
        Map<String, Object> options = new HashMap<>();
        options.put("bundleId", bundle);
        String locale = "es".equals(language) ? "es_ES" : "en_US";
        options.put("arguments", Arrays.asList("-AppleLanguages", "(" + language + ")",
                "-NSLanguages", "(" + language + ")", "-AppleLocale", locale));
        driver.executeScript("mobile: launchApp", options);
    }

    private InteractsWithApps apps() { return (InteractsWithApps) driver; }

    private Map<String, Object> artifact(String property) throws Exception {
        return evidence("artifact", appPath(property));
    }

    private Map<String, Object> evidence(String mode, String path) throws Exception {
        return EmailEnforcementFixture.iosEvidence(device, bundle, mode, path);
    }

    private static String appPath(String property) {
        String value = System.getProperty(property, "").trim();
        Assert.assertFalse("Supply -D" + property, value.isEmpty());
        Path path = Paths.get(value).toAbsolutePath().normalize();
        Assert.assertTrue("App artifact is missing for " + property, Files.exists(path));
        return path.toString();
    }

    private static boolean capability(String property, String environment, boolean fallback) {
        String value = System.getProperty(property, System.getenv(environment));
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
