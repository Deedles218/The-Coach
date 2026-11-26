package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Allure;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.AndroidDevice;
import lib.Platform;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.android.AndroidBookmarksPageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import lib.ui.android.AndroidPermissionPageObject;
import lib.ui.android.AndroidProgramSelectorPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.openqa.selenium.TimeoutException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Inherit the current reversible scenarios; only Android-specific contracts are overridden. */
public class AndroidBookmarksTests extends BookmarksTests {
    @Override @Before public void setUp() throws Exception {
        requireSupportedPlatform(); // Reject an omitted/wrong platform before opening any device session.
        super.setUp();
    }

    @Override protected void requireSupportedPlatform() {
        Assert.assertTrue("Android Bookmarks requires platform=android", Platform.getInstance().isAndroid());
    }

    @Override protected void prepare(String fixture) {
        if (!Boolean.getBoolean("coach.bookmarks.useCurrentAccount")) {
            super.prepare(fixture);
            return;
        }
        requireSupportedPlatform();
        coach = CoachFlowPageObjectFactory.get(driver);
        bookmarks = BookmarksPageObjectFactory.get(driver);
        coach.activateAppIfPossible();
        new AndroidPermissionPageObject(driver).dismissSystemNotificationPromptIfPresent();
        bookmarks.closeOpenedContentIfPresent();
        if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
        coach.assertAuthorizedDashboardIsDisplayed();
        coach.openToday();
    }

    @Override protected void login(TestData.TestAccount account) {
        try {
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
            verifyProfileAndReturnToToday(account);
        } catch (TimeoutException loginTimeout) {
            recoverWorkbookLogin(account, loginTimeout);
        }
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

    @Override protected void selectFixtureContentDay(String day) {
        if (day == null) return;
        Assert.assertTrue("Fixture content day must be a positive number", day.matches("[1-9][0-9]*"));
        new AndroidProgramSelectorPageObject(driver).selectObservedDay(Integer.parseInt(day));
    }

    @Override protected void verifyTooltipDismissal(String variant) throws Exception {
        Assert.assertFalse("COA-9653 needs a verified fixture login after a fresh Android installation",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        UpdateEvidence.assertAppiumDevice(driver, AndroidDevice.serial());
        Assert.assertTrue("COA-9653 needs an application-management driver", driver instanceof InteractsWithApps);
        Path artifact = apk("android.tooltip.apk");
        Map<String, String> requested = UpdateEvidence.androidApkBuild(artifact);
        Assert.assertEquals("Tooltip layout and measurement must use the same Android build", requested, installedBookmarksBuild());
        String tooltip = required("tooltipText");
        List<String> titles = Arrays.asList(required("lessonTitle"), required("practiceTitle"));
        prepare("TOOLTIP");
        AndroidBookmarksPageObject page = (AndroidBookmarksPageObject) bookmarks;
        AndroidBookmarksPageObject.TooltipLayout layout = page.captureTooltipLayout(tooltip, titles);

        InteractsWithApps apps = (InteractsWithApps) driver;
        String packageName = Platform.getInstance().getAndroidAppPackage();
        apps.terminateApp(packageName);
        Assert.assertTrue("Cannot remove Android app before the tooltip measurement", apps.removeApp(packageName));
        Assert.assertFalse("Android app was still installed before preparing fresh tooltip history", apps.isAppInstalled(packageName));
        apps.installApp(artifact.toString());
        verifyInstalledBookmarksBuild("Tooltip " + variant, requested);
        coach.activateAppIfPossible();
        new AndroidPermissionPageObject(driver).dismissSystemNotificationPromptIfPresent();
        login(TestData.bookmarksAccount("TOOLTIP"));

        page.dismissFreshTooltip(variant, tooltip, layout);
        bookmarks.reopenAndAssertTooltipAbsent(tooltip);
        bookmarks.assertExactCardTitles(titles, null);
    }

    @Override @Test @Issue("COA-9666")
    @DisplayName("COA-9666 Android Bookmarks and tooltip survive update/reinstall")
    public void bookmarksAndTooltipStateAcrossUpdateAndReinstall() throws Exception {
        InstallVariant variant = bookmarkInstallVariant();
        UpdateEvidence.assertAppiumDevice(driver, AndroidDevice.serial());
        Assert.assertTrue("Update/reinstall needs an application-management driver", driver instanceof InteractsWithApps);
        Path newApk = apk("android.update.new");
        Map<String, String> requested = UpdateEvidence.androidApkBuild(newApk);
        Map<String, String> before = installedBookmarksBuild();
        if (variant.updates()) {
            Map<String, String> oldArtifact = UpdateEvidence.androidApkBuild(apk("android.update.old"));
            Assert.assertEquals("Start the Appium session on the prepared old build", oldArtifact, before);
        }
        assertBookmarksInstallBaseline(variant, before, requested);
        String lesson = required("lessonTitle"), tooltip = required("tooltipText");
        prepare(UPDATE);
        assertInstalledBookmarks(lesson, tooltip, false);
        bookmarks.backFromBookmarks();
        InteractsWithApps apps = (InteractsWithApps) driver;
        String packageName = Platform.getInstance().getAndroidAppPackage();
        TestData.TestAccount account = TestData.bookmarksAccount(UPDATE);
        String phase = "Update";
        try {
            if (variant.updates()) {
                apps.terminateApp(packageName);
                Assert.assertTrue("Android replacement installation failed",
                        AndroidDevice.adb("install", "-r", newApk.toString()).contains("Success"));
                verifyInstalledBookmarksBuild(phase, requested);
                coach.activateAppIfPossible();
                new AndroidPermissionPageObject(driver).dismissSystemNotificationPromptIfPresent();
                coach.assertAuthorizedDashboardIsDisplayed();
                verifyProfileAndReturnToToday(account);
                assertInstalledBookmarks(lesson, tooltip, false);
            }
            if (variant.reinstalls()) {
                phase = "Reinstall";
                apps.terminateApp(packageName);
                Assert.assertTrue("Cannot remove app before reinstall", apps.removeApp(packageName));
                Assert.assertFalse("Android app remained installed before reinstall", apps.isAppInstalled(packageName));
                Allure.addAttachment("Android package removal", packageName + " was absent before reinstall");
                apps.installApp(newApk.toString());
                verifyInstalledBookmarksBuild(phase, requested);
                coach.activateAppIfPossible();
                new AndroidPermissionPageObject(driver).dismissSystemNotificationPromptIfPresent();
                coach.ensureLoggedOutOnStartScreen();
                login(account);
                assertInstalledBookmarks(lesson, tooltip, true);
            }
        } catch (Exception | AssertionError failure) {
            captureFailureArtifacts(failure, Description.createTestDescription(getClass(),
                    "androidBookmarks" + phase + "BeforeRecovery"));
            try { if (!apps.isAppInstalled(packageName)) apps.installApp(newApk.toString()); }
            catch (Exception | AssertionError recovery) { failure.addSuppressed(recovery); }
            throw failure;
        }
    }

    private void recoverWorkbookLogin(TestData.TestAccount account, TimeoutException loginTimeout) {
        String packageName = Platform.getInstance().getAndroidAppPackage();
        String workbook = "id:" + packageName + ":id/tvEworkbook";
        String close = "id:" + packageName + ":id/btnClose";
        if (!(driver instanceof InteractsWithApps) || !coach.isElementVisible(workbook) || !coach.isElementVisible(close)) {
            throw loginTimeout;
        }

        InteractsWithApps apps = (InteractsWithApps) driver;
        try {
            driver.navigate().back();
            coach.waitForElementNotVisible(workbook, "Android workbook stayed open after Back", 10);
            apps.terminateApp(packageName);
            apps.activateApp(packageName);
            new AndroidPermissionPageObject(driver).dismissSystemNotificationPromptIfPresent();
            String today = "id:" + packageName + ":id/nav_graph_daily";
            String introduction = "xpath://*[@resource-id='" + packageName + ":id/viewPager']"
                    + "//*[@resource-id='" + packageName + ":id/tvHeader'"
                    + " and starts-with(@text,'Introducing your main program:')]";
            coach.waitForFirstElementPresent(new String[]{today, introduction},
                    "Android app did not resume Today or the main-program introduction", 25);
            if (coach.isElementVisible(introduction)) {
                new AndroidOnboardingPageObject(driver)
                        .closePaywallsAndPopupsUntil(() -> coach.isElementVisible(today));
            }
            coach.assertAuthorizedDashboardIsDisplayed();
            verifyProfileAndReturnToToday(account);
            Allure.addAttachment("Workbook login recovery",
                    "The workbook close timed out. Back and one app relaunch restored the verified Profile.");
        } catch (Exception | AssertionError recoveryFailure) {
            if (recoveryFailure != loginTimeout) loginTimeout.addSuppressed(recoveryFailure);
            throw loginTimeout;
        }
    }

    private void verifyInstalledBookmarksBuild(String phase, Map<String, String> expected) throws Exception {
        Map<String, String> installed = installedBookmarksBuild();
        UpdateEvidence.attachBuild("Android " + phase + " installed", installed.get("version"), installed.get("build"));
        Assert.assertEquals("Android " + phase + " installed an unexpected build", expected, installed);
    }

    private Map<String, String> installedBookmarksBuild() throws Exception {
        return UpdateEvidence.androidInstalledBuild(AndroidDevice.adb("shell", "dumpsys", "package",
                Platform.getInstance().getAndroidAppPackage()));
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
