package tests;
import lib.*;
import lib.ui.android.*;
import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.*;
import org.junit.*;

public class AndroidPushPermissionTests extends AndroidTestCase {
    private void requireFixture() throws Exception {
        AndroidDevice.serial();
        Assert.assertTrue("Notification test requires Android 13+",Integer.parseInt(AndroidDevice.adb("shell","getprop","ro.build.version.sdk").trim())>=33);
        Assert.assertTrue("Use a clean Android install and an APK",Platform.getInstance().isFreshAndroidInstallConfigured());
        Assert.assertEquals("Disable automatic permissions for push tests","false",System.getProperty("android.autoGrantPermissions"));
    }
    private void verifyJourney() throws Exception {
        AndroidPermissionPageObject page=new AndroidPermissionPageObject(driver);
        page.completeFreshJourneyToPermission();page.allowAndVerifySystemPermission();
    }
    @Test @Issue("COA-7918")
    public void testCleanInstallRequestsNotificationPermission() throws Exception {requireFixture();verifyJourney();}
    @Test @Issue("COA-7914")
    public void testCleanInstallOnboardingAndNotificationPermission() throws Exception {
        requireFixture();verifyJourney();
        new AndroidPermissionPageObject(driver).finishOnboardingToToday();
    }
    @Test @Issue("COA-7918")
    public void testReinstallRequestsNotificationPermissionAgain() throws Exception {
        requireFixture();verifyJourney();
        InteractsWithApps apps=(InteractsWithApps)driver;
        Assert.assertTrue("Could not remove test app",apps.removeApp(Platform.getInstance().getAndroidAppPackage()));
        apps.installApp(Platform.getInstance().getAndroidAppPath());
        new AndroidCoachFlowPageObject(driver).activateAppIfPossible();
        Assert.assertFalse("Reinstall retained notification grant",new AndroidPermissionPageObject(driver).notificationGranted());
        verifyJourney();
    }
}
