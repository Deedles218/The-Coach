package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.OnboardingGoal;
import lib.ui.ProgramSwitchFeaturePageObject;
import lib.ui.SubscriptionPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import lib.ui.android.AndroidSubscriptionPageObject;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

@Epic("Daily Plan")
@Feature("COA-9549 program switch tooltip")
public class ProgramSwitchFeatureTests extends CoreTestCase {
    @Override
    protected RemoteWebDriver createDriver() throws Exception {
        Platform platform = Platform.getInstance();
        Assert.assertTrue("COA-9549 needs a clean Android install with -Dandroid.app, "
                        + "-Dandroid.noReset=false and -Dandroid.fullReset=true; for iOS use a local app "
                        + "with fullReset, manually prepare a fresh TestFlight installation with "
                        + "-Dcoach.program.freshStartPrepared=true, or a new 0-progress Today profile with "
                        + "-Dcoach.program.preparedToday=true",
                platform.isAndroid() ? platform.isFreshAndroidInstallConfigured()
                        : (platform.isCleanInstallConfigured() && platform.getIOSAppPath() != null)
                            || Boolean.getBoolean("coach.program.freshStartPrepared")
                            || Boolean.getBoolean("coach.program.preparedToday"));
        return super.createDriver();
    }

    private ProgramSwitchFeaturePageObject completeFirstItem() {
        OnboardingGoal goal = OnboardingGoal.fromConfiguredValue(Platform.getInstance().getOnboardingGoal());
        if (Platform.getInstance().isIOS() && Boolean.getBoolean("coach.program.preparedToday")) {
            // A TestFlight installation can be prepared as a new 0-progress
            // account without a local IPA for Appium to reinstall. Dismiss
            // promotional screens that can recur when a new session starts.
            OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
            OnboardingPageObjectFactory.get(driver).waitForToday();
        } else if (Platform.getInstance().isAndroid() && Boolean.getBoolean("coach.program.purchaseFixture")) {
            Assert.assertTrue("Test purchase requires -Dgoogleplay.licenseTester=true",
                    Platform.getInstance().isGooglePlayLicenseTesterEnabled());
            Assert.assertTrue("Test purchase requires -Dpurchase.allow=true",
                    Platform.getInstance().isTestPurchaseAllowed());
            AndroidOnboardingPageObject onboarding = new AndroidOnboardingPageObject(driver);
            onboarding.completeNewUserJourneyToPaywall(goal);
            AndroidSubscriptionPageObject paywall = new AndroidSubscriptionPageObject(driver);
            paywall.assertPaywallIsDisplayed();
            paywall.selectPlan(SubscriptionPageObject.PlanPosition.TOP);
            paywall.startPurchaseAndWaitForStore();
            paywall.confirmTestPurchase();
            paywall.assertPurchaseCompleted();
            onboarding.closePaywallsAndPopups();
            onboarding.waitForToday();
        } else {
            OnboardingPageObjectFactory.get(driver).completeNewUserJourney(goal);
        }
        ProgramSwitchFeaturePageObject flow = new ProgramSwitchFeaturePageObject(driver);
        flow.openToday();
        flow.assertFirstTooltipAbsent();
        flow.completeDailyPlanItem(System.getProperty("coach.program.itemTitle"));
        flow.waitForFirstTooltip();
        return flow;
    }

    @Test @Issue("COA-9549")
    public void testFirstTooltipCancelConfirmAndNoRepeat() {
        ProgramSwitchFeaturePageObject flow = completeFirstItem();
        String target = System.getProperty("coach.program.target");
        Assert.assertNotNull("Configure -Dcoach.program.target to a different available program", target);
        String original = flow.activeProgram();
        Assert.assertNotEquals("Destination must differ from active program", original, target);
        flow.dismissFirstTooltip();

        flow.openSelector();
        flow.selectProgramForFirstTime(target);
        flow.waitForConfirmation();
        flow.cancelConfirmation();
        Assert.assertEquals("Cancel changed the active program", original, flow.activeProgram());

        flow.openSelector();
        flow.selectProgramForFirstTime(target);
        flow.waitForConfirmation();
        flow.confirmSwitch();
        flow.waitForProgram(target);
        flow.waitForSecondTooltip();
        flow.dismissSecondTooltip();

        flow.openSelector();
        flow.selectProgramForFirstTime(original);
        flow.assertConfirmationAbsent();
        flow.waitForProgram(original);
        flow.assertTooltipsAbsent();
        closeAndReopenCoachApplication();
        flow.openToday();
        flow.assertTooltipsAbsent();
    }

    @Test @Issue("COA-9549")
    public void testFirstTooltipForFreshAnonymousAccountAfterReinstall() {
        Platform platform = Platform.getInstance();
        Assert.assertNotNull("Automated reinstall requires -Dios.app or -Dandroid.app; "
                + "TestFlight-only installs need a manual reinstall protocol", platform.isAndroid()
                ? platform.getAndroidAppPath() : platform.getIOSAppPath());
        ProgramSwitchFeaturePageObject flow = completeFirstItem();
        flow.dismissFirstTooltip();
        Assert.assertTrue("Reinstall requires an Appium mobile driver", driver instanceof InteractsWithApps);
        String appId = platform.isAndroid() ? platform.getAndroidAppPackage() : platform.getIOSBundleId();
        String appPath = platform.isAndroid() ? platform.getAndroidAppPath() : platform.getIOSAppPath();
        InteractsWithApps apps = (InteractsWithApps) driver;
        Assert.assertTrue("Could not uninstall feature build", apps.removeApp(appId));
        apps.installApp(appPath);
        apps.activateApp(appId);
        completeFirstItem();
    }
}
