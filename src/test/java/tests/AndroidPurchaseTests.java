package tests;
import lib.*;
import lib.ui.*;
import lib.ui.android.*;
import io.qameta.allure.*;
import org.junit.*;

/** Google Play counterparts of COA-755 / COA-872. Requires a license-test device. */
public class AndroidPurchaseTests extends AndroidTestCase {
    private AndroidSubscriptionPageObject paywall() {
        Assert.assertTrue("Configure a Google Play license tester",Platform.getInstance().isGooglePlayLicenseTesterEnabled());
        Assert.assertTrue("Explicit test-store transactions must be enabled",Platform.getInstance().isTestPurchaseAllowed());
        Assert.assertTrue("Use a fresh isolated installation for purchase tests",Platform.getInstance().isFreshAndroidInstallConfigured());
        new AndroidOnboardingPageObject(driver).completeNewUserJourneyToPaywall(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
        AndroidSubscriptionPageObject page=new AndroidSubscriptionPageObject(driver);page.assertPaywallIsDisplayed();return page;
    }
    @Test @Issue("COA-755") public void testTrialActivatesSelectedSubscription() throws Exception {
        AndroidSubscriptionPageObject page=paywall();page.selectPlan(SubscriptionPageObject.PlanPosition.TOP);
        AndroidBillingEvidence entitlement=new AndroidBillingEvidence();
        page.startPurchaseAndWaitForStore();
        page.waitForElementVisible("xpath://*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'free trial')]", "Selected Google Play offer has no free trial", 10);
        page.confirmTestPurchase();page.assertPurchaseCompleted();
        entitlement.assertActivated();
        new AndroidOnboardingPageObject(driver).closePaywallsAndPopups();
        new AndroidCoachFlowPageObject(driver).assertAuthorizedDashboardIsDisplayed();
    }
    @Test @Issue("COA-872") public void testRestorePurchasesReturnsExplicitResult() {
        AndroidSubscriptionPageObject page=paywall();page.assertRestoreButtonIsAvailable();page.tapRestoreAndAssertResult();
    }
}
