package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.SubscriptionPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.SubscriptionPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic(value = "The Coach StoreKit subscriptions")
public class StoreKitPurchaseTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Paywall"), @Feature(value = "StoreKit Sandbox")})
    @Issue("COA-755")
    @DisplayName("COA-755 Start free trial activates the selected subscription period")
    @Description("Runs the onboarding-to-paywall flow in an explicitly enabled StoreKit sandbox, confirms the purchase, and verifies premium access from Profile.")
    @Step("Start test testStartFreeTrialInStoreKitSandbox")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testStartFreeTrialInStoreKitSandbox() {
        requireIOSPlatform();

        requireStoreKitSandboxAndPurchaseApproval();
        SubscriptionPageObject subscription = openConfiguredPaywall();
        subscription.openSelectedSubscriptionPeriod();
        subscription.startStoreKitPurchase();
        subscription.confirmStoreKitPurchase();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        coachFlow.openProfile();
        subscription.assertPremiumAccessIsDisplayed();
    }

    @Test
    @Features(value = {@Feature(value = "Paywall"), @Feature(value = "Restore Purchases")})
    @Issue("COA-872")
    @DisplayName("COA-872 Restore Purchases returns a StoreKit result")
    @Description("Opens the current subscription paywall in StoreKit sandbox and verifies that Restore Purchases either restores access or shows an explicit sandbox result such as an expired subscription.")
    @Step("Start test testRestorePurchasesInStoreKitSandbox")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testRestorePurchasesInStoreKitSandbox() {
        requireIOSPlatform();

        requireStoreKitSandboxAndPurchaseApproval();
        SubscriptionPageObject subscription = openConfiguredPaywall();
        subscription.restorePurchases();
    }

    private SubscriptionPageObject openConfiguredPaywall() {
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openStartFlow();
        coachFlow.runConfiguredOnboardingSteps(Platform.getInstance().getIOSOnboardingStepLocators());

        SubscriptionPageObject subscription = SubscriptionPageObjectFactory.get(driver);
        Assert.assertNotNull("Subscription page object is not available for current platform", subscription);
        subscription.assertPaywallIsDisplayed();
        return subscription;
    }

    private void requireStoreKitSandboxAndPurchaseApproval() {
        Assert.assertTrue(
                "StoreKit suite requires -Dstorekit.sandbox=true or STOREKIT_SANDBOX=true",
                Platform.getInstance().isStoreKitSandboxEnabled()
        );
        Assert.assertTrue(
                "Purchase/restore actions are disabled by default. Explicitly enable them with -Dstorekit.allowPurchases=true or STOREKIT_ALLOW_PURCHASES=true",
                Platform.getInstance().isStoreKitPurchaseAllowed()
        );
    }
}
