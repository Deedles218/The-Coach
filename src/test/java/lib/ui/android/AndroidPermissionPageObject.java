package lib.ui.android;

import lib.AndroidDevice;
import lib.ui.MainPageObject;
import lib.ui.OnboardingGoal;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Android 13+ notification authorization, including its actual OS grant state. */
public final class AndroidPermissionPageObject extends MainPageObject {
    private static final String P="com.vamapps.thecoach:id/";
    private static final String ALLOW="id:com.android.permissioncontroller:id/permission_allow_button";
    private static final String DENY="id:com.android.permissioncontroller:id/permission_deny_button";
    private static final String PROMPT="xpath://*[@resource-id='"+P+"tvHeader' and contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'notification')]";
    public AndroidPermissionPageObject(RemoteWebDriver driver) {super(driver);}
    public void completeFreshJourneyToPermission() {
        waitForFirstElementPresent(new String[]{ALLOW,PROMPT,"id:"+P+"btnSignInAnonymous"},"Fresh install did not reach permission or Welcome",30);
        if(isElementVisible(ALLOW)||isElementVisible(PROMPT)) return;
        AndroidOnboardingPageObject onboarding=new AndroidOnboardingPageObject(driver);
        onboarding.completeNewUserJourneyToPaywall(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
        for(int n=0;n<12;n++) {
            if(isElementVisible(PROMPT)||isElementVisible(ALLOW))return;
            if(isElementVisible("id:"+P+"btnClose")) {
                waitForElementAndClick("id:"+P+"btnClose","Cannot close optional paywall before permission prompt",10);
                waitForElementNotVisible("id:"+P+"btnClose","Paywall did not close before permission prompt",12);
            } else if(isElementVisible("id:"+P+"btnGotIt")) {
                waitForElementAndClick("id:"+P+"btnGotIt","Cannot advance onboarding introduction",10);
            } else {
                waitForFirstElementPresent(new String[]{PROMPT,ALLOW,"id:"+P+"btnClose","id:"+P+"btnGotIt"},"Onboarding did not reach notification authorization",20);
            }
        }
        Assert.fail("Notification prompt was never reached");
    }
    public void allowAndVerifySystemPermission() throws Exception {
        Assert.assertFalse("Notification permission must initially be denied",notificationGranted());
        if(isElementVisible(PROMPT))waitForElementAndClick("id:"+P+"btnMain","Cannot allow in-app notification prompt",10);
        waitForElementVisible(ALLOW,"Android notification dialog did not open",15);
        waitForElementVisible(DENY,"Android notification deny control absent",10);
        String message=waitForElementVisible("id:com.android.permissioncontroller:id/permission_message","System permission explanation absent",10).getText();
        Assert.assertTrue("Wrong Android permission dialog",message.toLowerCase(java.util.Locale.ROOT).contains("notification"));
        waitForElementAndClick(ALLOW,"Cannot grant Android notification permission",10);
        waitForElementNotVisible(ALLOW,"System permission dialog did not close",10);
        Assert.assertTrue("OS did not persist POST_NOTIFICATIONS grant",notificationGranted());
    }
    public void finishOnboardingToToday() {
        waitForFirstElementPresent(new String[]{"id:"+P+"btnSignInAnonymous","id:"+P+"nav_graph_daily","id:"+P+"btnClose","id:"+P+"btnGotIt","id:"+P+"ivClose"},"App did not resume after the Android permission dialog",20);
        AndroidOnboardingPageObject onboarding=new AndroidOnboardingPageObject(driver);
        if(isElementVisible("id:"+P+"btnSignInAnonymous")) onboarding.completeNewUserJourneyToPaywall(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
        onboarding.closePaywallsAndPopups();onboarding.waitForToday();
        new AndroidDailyPlanPageObject(driver).openTodayTab();
    }
    public boolean notificationGranted() throws Exception {
        return AndroidDevice.adb("shell","dumpsys","package","com.vamapps.thecoach")
                .matches("(?s).*android\\.permission\\.POST_NOTIFICATIONS: granted=true.*");
    }
}
