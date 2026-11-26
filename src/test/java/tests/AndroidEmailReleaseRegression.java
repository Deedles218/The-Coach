package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Feature;
import lib.AndroidDevice;
import lib.Platform;
import lib.RestoringTestAction;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.android.AndroidEmailEnforcementPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.runner.Description;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/** Each method starts with its own anonymous account on an explicit disposable Android installation. */
@Feature("COA-9619 Android email release regression")
public class AndroidEmailReleaseRegression extends EmailReleaseRegression {
    private static final String HIM = "com.vamapps.thecoach";
    private static final String HER = "com.vamapps.thecoach.forher";
    private static final String ACTIVITY = "com.vamapps.thecoach.MainActivity";
    private String app;
    private String serial;
    private String goal;
    private String previousLocale;
    private AndroidEmailEnforcementPageObject screen;
    private AndroidOnboardingPageObject onboarding;

    @Before
    @Override
    public void setUp() throws Exception {
        Assume.assumeTrue(unsupportedPlatformMessage(), isPlatformSupported());
        app = Platform.getInstance().getAndroidAppPackage();
        serial = AndroidDevice.emailRegressionSerial();
        Assert.assertTrue("Use the Him or Her release package", HIM.equals(app) || HER.equals(app));
        Assert.assertEquals("Keep the test installation during Appium teardown", "logout", TestData.testIsolationMode());
        Assert.assertNull("Run the release scenario with a preinstalled APK", Platform.getInstance().getAndroidAppPath());
        Assert.assertFalse("The release scenario manages its own installation lifecycle",
                Boolean.parseBoolean(System.getProperty("android.fullReset", System.getenv("ANDROID_FULL_RESET")))
                        || !Boolean.parseBoolean(System.getProperty("android.noReset",
                                System.getenv("ANDROID_NO_RESET") == null ? "true" : System.getenv("ANDROID_NO_RESET"))));
        goal = HER.equals(app) ? "Cultivate desire" : "Beat premature ejaculation";
        validateLanguage(HER.equals(app) ? "en|es|fr" : "en|es|fr|de|it");
        Map<String, String> installed = installedBuild();
        Assert.assertEquals("Install the release APK requested by the regression scenario",
                requiredProperty("coach.emailRelease.version"), installed.get("version"));
        UpdateEvidence.attachBuild("Android email regression", installed.get("version"), installed.get("build"));
        previousLocale = currentAppLocale();
        try {
            // The release onboarding helpers use the verified English questionnaire.
            clearTestInstallation();
            setAppLocale("en-US");
            launch();
            super.setUp();
            onboarding = new AndroidOnboardingPageObject(driver);
            useScreen("en");
        } catch (Exception | AssertionError failure) {
            restoreAppLocale(failure);
            throw failure;
        }
    }

    @Override protected boolean isPlatformSupported() { return Platform.getInstance().isAndroid(); }

    @Override protected AndroidEmailEnforcementPageObject screen() { return screen; }

    @Override protected int defaultFirstRequestLaunch() { return 3; }

    @Override protected void startNewUserOnboarding() {
        screen.startFromPurchaseLogin();
        finishOnboarding(true);
    }

    @Override protected void installUpdate() throws Exception {
        installInPlace("coach.emailRelease.updateApk", "coach.emailRelease.version");
    }

    @Override protected void switchLanguage(String language) throws Exception {
        setAppLocale("es".equals(language) ? "es-ES" : language);
        useScreen(language);
        coldLaunch();
    }

    @Override protected void returnToWelcomeForLogin() throws Exception {
        clearTestInstallation();
        setAppLocale("en-US");
        launch();
    }

    private void preparePreviousRelease() throws Exception {
        installInPlace("coach.emailRelease.previousApk", "coach.emailRelease.previousVersion");
        clearTestInstallation();
        setAppLocale("en-US");
        launch();
        screen.startFromPurchaseLogin();
        finishOnboarding(true);
        screen.waitForDaily();
        screen.assertNoRequest();
    }

    @Override protected void withPreviousRelease(RestoringTestAction.CheckedAction scenario) throws Exception {
        Assert.assertNotEquals("Use distinct previous and target versions",
                requiredProperty("coach.emailRelease.version"), requiredProperty("coach.emailRelease.previousVersion"));
        Assert.assertNotEquals("Use distinct previous and target APKs",
                apk("coach.emailRelease.previousApk"), apk("coach.emailRelease.updateApk"));
        RestoringTestAction.run(() -> {
            try {
                preparePreviousRelease();
                scenario.run();
            } catch (Exception | AssertionError failure) {
                captureFailureArtifacts(failure, Description.createTestDescription(getClass(),
                        testName.getMethodName() + "BeforeRestore"));
                throw failure;
            }
        }, () -> {
            if (!requiredProperty("coach.emailRelease.version").equals(installedVersion())) {
                installInPlace("coach.emailRelease.updateApk", "coach.emailRelease.version");
            }
        });
    }

    @Override protected void finishOnboarding(boolean requirePaywall) {
        onboarding.selectGoal(goal);
        String reader = System.getProperty("coach.emailRelease.questionnaireReader", "").trim();
        onboarding.completeReleaseQuestionnaire(reader.isEmpty() ? null : Paths.get(reader).toAbsolutePath());
        int closed = screen.closeOffersBeforeEmailOrDaily();
        if (requirePaywall) Assert.assertTrue("The regression did not pass through a paywall", closed > 0);
    }

    private void useScreen(String language) { screen = AndroidEmailEnforcementPageObject.forRelease(driver, language); }

    private void clearTestInstallation() throws Exception {
        Assert.assertEquals("Could not reset the test application", "Success",
                adb("shell", "pm", "clear", app).trim());
    }

    private void launch() throws Exception {
        String result = adb("shell", "am", "start", "-n", app + "/" + ACTIVITY);
        Assert.assertFalse("Could not launch the selected Coach application", result.contains("Error"));
    }

    private void installInPlace(String apkProperty, String versionProperty) throws Exception {
        ((InteractsWithApps) driver).terminateApp(app);
        Path file = apk(apkProperty);
        String result = adb("install", "-r", file.toString());
        Assert.assertTrue("The Android replacement installation failed", result.contains("Success"));
        Assert.assertEquals("Unexpected Android APK version after installation",
                requiredProperty(versionProperty), installedVersion());
    }

    private String installedVersion() throws Exception {
        return installedBuild().get("version");
    }

    private Map<String, String> installedBuild() throws Exception {
        return UpdateEvidence.androidInstalledBuild(adb("shell", "dumpsys", "package", app));
    }

    private String adb(String... arguments) throws Exception {
        return AndroidDevice.adbForEmailRegression(serial, arguments);
    }

    private static Path apk(String property) {
        Path path = Paths.get(requiredProperty(property)).toAbsolutePath().normalize();
        Assert.assertTrue("APK missing for " + property, Files.isRegularFile(path));
        return path;
    }

    private static String requiredProperty(String key) {
        String value = System.getProperty(key, "").trim();
        Assert.assertFalse("Supply -D" + key, value.isEmpty());
        return value;
    }

    private String currentAppLocale() throws Exception {
        String output = adb("shell", "cmd", "locale", "get-app-locales", app).trim();
        int begin = output.lastIndexOf('[');
        Assert.assertTrue("Android did not report the app locale", begin >= 0 && output.endsWith("]"));
        return output.substring(begin + 1, output.length() - 1).replace(" ", "");
    }

    private void setAppLocale(String locale) throws Exception {
        if (locale.equals(currentAppLocale())) return;
        if (locale.isEmpty()) adb("shell", "cmd", "locale", "set-app-locales", app);
        else adb("shell", "cmd", "locale", "set-app-locales", app, "--locales", locale);
        Assert.assertEquals("Android app locale was not applied", locale, currentAppLocale());
    }

    protected void restoreAppLocale(Throwable failure) throws Exception {
        if (previousLocale == null) return;
        try {
            setAppLocale(previousLocale);
        } catch (Exception | AssertionError cleanup) {
            if (failure == null) throw cleanup;
            failure.addSuppressed(cleanup);
        } finally {
            previousLocale = null;
        }
    }

    @Override public void tearDown() {
        try {
            RestoringTestAction.run(super::tearDown, () -> restoreAppLocale(null));
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Exception cleanup) {
            throw new IllegalStateException("Could not restore the Android app locale", cleanup);
        }
    }

}
