package tests;

import lib.*;
import lib.ui.android.*;
import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.*;
import org.junit.*;
import java.nio.file.*;

@Epic("Android release")
public class AndroidUpdateTests extends AndroidTestCase {
    @Test @Issue("COA-7914")
    public void testUpdatePreservesAuthorizationAndProgress() throws Exception {
        Path oldApk = apk("android.update.old"), newApk = apk("android.update.new");
        Assert.assertFalse("Update needs different APK artifacts", java.util.Arrays.equals(Files.readAllBytes(oldApk), Files.readAllBytes(newApk)));
        AndroidDevice.serial(); // Do not remove an installation on an unspecified/personal device.
        String expectedUid = System.getenv("COACH_EXPECTED_ACCOUNT_UID");
        Assert.assertNotNull("Update needs a verified test-account UID", expectedUid);
        InteractsWithApps apps = (InteractsWithApps)driver;
        AndroidCoachFlowPageObject coach = new AndroidCoachFlowPageObject(driver);
        AndroidDailyPlanPageObject today = new AndroidDailyPlanPageObject(driver);
        String packageName = Platform.getInstance().getAndroidAppPackage();
        Assert.assertEquals("Only the approved male test app is supported", "com.vamapps.thecoach", packageName);
        try {
            // A production APK cannot always be downgraded in place. Establish the
            // old baseline with a genuine installation, then upgrade without removal.
            Assert.assertTrue("Could not remove the newer test installation", apps.removeApp(packageName));
            apps.installApp(oldApk.toString());
            coach.activateAppIfPossible();
            coach.waitForFirstElementPresent(new String[]{"id:com.android.permissioncontroller:id/permission_allow_button", "id:com.vamapps.thecoach:id/btnSignInAnonymous"}, "Old app did not reach Welcome", 30);
            if(coach.isElementVisible("id:com.android.permissioncontroller:id/permission_allow_button"))
                coach.waitForElementAndClick("id:com.android.permissioncontroller:id/permission_allow_button", "Cannot grant old-install notification permission", 10);
            coach.waitForStartScreen();
            TestData.TestAccount account = TestData.existingProgressAccount();
            coach.authenticateForFixtureSetup(account.getEmail(), account.getOtp());
            if (coach.isElementVisible("id:com.vamapps.thecoach:id/tvAnswerNum")) {
                AndroidOnboardingPageObject onboarding = new AndroidOnboardingPageObject(driver);
                onboarding.selectGoal(lib.ui.OnboardingGoal.BEAT_PREMATURE_EJACULATION);
                onboarding.completeQuestionnaire();
                // Establish a normal cold-launch baseline before measuring upgrade preservation.
                apps.terminateApp(packageName);apps.activateApp(packageName);
                coach.waitForFirstElementPresent(new String[]{"id:com.vamapps.thecoach:id/nav_graph_daily", "id:com.vamapps.thecoach:id/ivClose", "id:com.vamapps.thecoach:id/btnGotIt", "id:com.vamapps.thecoach:id/tvEworkbook"},"Old baseline did not reach Today or launch promo",30);
                onboarding.closePaywallsAndPopups();onboarding.waitForToday();
            }
            Assert.assertEquals("Wrong account before upgrade", expectedUid, AndroidModuleEvidence.currentUid());
            today.openTodayTab();
            String program = today.getActiveProgramName(), day = today.getCurrentDayLabel(), progress = today.getProgramProgressValue();
            String oldVersion = AndroidDevice.version();
            long oldCode=AndroidDevice.versionCode();
            AndroidDevice.adb("shell", "am", "force-stop", packageName);
            String result = AndroidDevice.adb("install", "-r", newApk.toString());
            Assert.assertTrue("Replacement installation failed", result.contains("Success"));
            coach.activateAppIfPossible();
            coach.waitForFirstElementPresent(new String[]{"id:com.vamapps.thecoach:id/nav_graph_daily", "id:com.vamapps.thecoach:id/ivClose"}, "Updated app did not reach Today or launch promo", 30);
            new AndroidOnboardingPageObject(driver).closePaywallsAndPopups();
            coach.assertAuthorizedDashboardIsDisplayed();
            Assert.assertEquals("Upgrade lost/replaced account identity", expectedUid, AndroidModuleEvidence.currentUid());
            today.openTodayTab();
            Assert.assertEquals("Upgrade changed selected program", program, today.getActiveProgramName());
            Assert.assertEquals("Upgrade changed viewed day", day, today.getCurrentDayLabel());
            Assert.assertEquals("Upgrade changed progress", progress, today.getProgramProgressValue());
            Assert.assertTrue("Update must increase versionCode",AndroidDevice.versionCode()>oldCode);
            Assert.assertNotEquals("The installed version did not change", oldVersion, AndroidDevice.version());
            Allure.addAttachment("Verified Android upgrade", oldVersion + " -> " + AndroidDevice.version()
                    + "; " + program + "; " + day + "; " + progress + "; UID preserved");
        } catch (Exception | AssertionError failure) {
            captureFailureArtifacts(failure, org.junit.runner.Description.createTestDescription(getClass(), "androidUpgradeBeforeRecovery"));
            try { AndroidDevice.adb("install", "-r", newApk.toString()); }
            catch (Exception | AssertionError recovery) { failure.addSuppressed(recovery); }
            throw failure;
        }
    }
    private Path apk(String key) {
        String value = System.getProperty(key);
        Assert.assertNotNull("Provide -D" + key + "=/absolute/path.apk", value);
        Path path = Paths.get(value);
        Assert.assertTrue("APK missing: " + path, Files.isRegularFile(path));
        return path;
    }
}
