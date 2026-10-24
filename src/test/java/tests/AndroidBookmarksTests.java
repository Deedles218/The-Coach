package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.AndroidDevice;
import lib.Platform;
import lib.TestData;
import lib.ui.android.AndroidBookmarksPageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.android.AndroidProgramSelectorPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Inherit the current reversible scenarios; only Android-specific contracts are overridden. */
public class AndroidBookmarksTests extends BookmarksTests {
    @Override @Before public void setUp() throws Exception {
        requireSupportedPlatform(); // Reject an omitted/wrong platform before opening any device session.
        super.setUp();
    }

    @Override protected void requireSupportedPlatform() {
        Assert.assertTrue("Android Bookmarks requires platform=android", Platform.getInstance().isAndroid());
    }

    @Override protected void login(TestData.TestAccount account) {
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        verifyProfileAndReturnToToday(account);
    }

    /** Shared by login and upgrade; never repairs a lost upgraded session with another login. */
    protected void verifyProfileAndReturnToToday(TestData.TestAccount account) {
        coach.openProfile();
        ((AndroidCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(account.getEmail());
        coach.returnToMainScreenFromProfile();
        coach.openToday();
    }

    @Override protected void selectFixtureProgram(String program) {
        if (program == null) return;
        AndroidProgramSelectorPageObject selector = new AndroidProgramSelectorPageObject(driver);
        if (!program.equalsIgnoreCase(selector.getActiveProgramName())) {
            selector.openProgramSelector();
            selector.selectExactProgram(program);
        }
    }

    @Override @Test @Issue("COA-9666")
    @DisplayName("COA-9666 Android Bookmarks and tooltip survive update/reinstall")
    public void bookmarksAndTooltipStateAcrossUpdateAndReinstall() throws Exception {
        Path oldApk = apk("android.update.old"), newApk = apk("android.update.new");
        Assert.assertFalse("Update needs different APK artifacts", Arrays.equals(Files.readAllBytes(oldApk), Files.readAllBytes(newApk)));
        AndroidDevice.serial(); // Never uninstall on an unspecified/personal device.
        prepare(UPDATE);
        String lesson = required("lessonTitle"), tooltip = required("tooltipText");
        bookmarks.openFromToday(); bookmarks.assertCard(lesson); bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.backFromBookmarks();
        Assert.assertTrue("Update needs an application-management driver", driver instanceof InteractsWithApps);
        InteractsWithApps apps = (InteractsWithApps) driver;
        String packageName = Platform.getInstance().getAndroidAppPackage();
        try {
            apps.terminateApp(packageName);
            Assert.assertTrue("Cannot remove app before the old-build baseline", apps.removeApp(packageName));
            apps.installApp(oldApk.toString());
            coach.activateAppIfPossible();
            login(TestData.bookmarksAccount(UPDATE));
            bookmarks.openFromToday(); bookmarks.assertCard(lesson); bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.backFromBookmarks();
            long oldCode = AndroidDevice.versionCode();
            apps.terminateApp(packageName);
            Assert.assertTrue("Android replacement installation failed", AndroidDevice.adb("install", "-r", newApk.toString()).contains("Success"));
            coach.activateAppIfPossible();
            coach.assertAuthorizedDashboardIsDisplayed();
            Assert.assertTrue("Update must increase installed versionCode", AndroidDevice.versionCode() > oldCode);
            // Verify identity without logging in again: a lost session cannot be repaired into a pass.
            verifyProfileAndReturnToToday(TestData.bookmarksAccount(UPDATE));
            bookmarks.openFromToday(); bookmarks.assertCard(lesson); bookmarks.assertTooltipAbsent(tooltip);
            apps.terminateApp(packageName);
            Assert.assertTrue("Cannot remove app before reinstall", apps.removeApp(packageName));
            apps.installApp(newApk.toString());
            coach.activateAppIfPossible(); login(TestData.bookmarksAccount(UPDATE));
            bookmarks.openFromToday(); bookmarks.assertCard(lesson); bookmarks.assertTooltipVisible(tooltip);
        } catch (Exception | AssertionError failure) {
            captureFailureArtifacts(failure, org.junit.runner.Description.createTestDescription(getClass(), "androidBookmarksUpdateBeforeRecovery"));
            try { AndroidDevice.adb("install", "-r", newApk.toString()); }
            catch (Exception | AssertionError recovery) { failure.addSuppressed(recovery); }
            throw failure;
        }
    }

    @Override @Test @Issue("COA-9667")
    @DisplayName("Android candidate: unsubscribed account sees Bookmarks entry paywall")
    public void availableLessonCanBeBookmarkedWithoutSubscription() {
        // Keep the local Android expectation separate; never replace the current iOS base-lesson test.
        prepare(NO_SUBSCRIPTION);
        bookmarks.tapEntryFromToday();
        ((AndroidBookmarksPageObject) bookmarks).assertSubscriptionPaywallIsDisplayed();
    }

    private static Path apk(String property) {
        String value = System.getProperty(property);
        Assert.assertTrue("Configure -D" + property, value != null && !value.trim().isEmpty());
        Path path = Paths.get(value).toAbsolutePath();
        Assert.assertTrue("APK is missing", Files.isRegularFile(path));
        return path;
    }
}
