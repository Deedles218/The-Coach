package tests;

import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.MobileCaseFixture;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import lib.ui.ios.IOSProgramPurchasePageObject;
import lib.ui.ios.IOSProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/** COA-9787: one independent, UID-bound Paywall profile per purchase outcome. */
public class ProgramPaywallEntryTests extends CoreTestCase {
    private IOSProgramPurchasePageObject purchase;
    private IOSProgramSelectionPageObject selection;

    @Before @Override public void setUp() throws Exception {
        requireIOSPlatform();
        Assert.assertTrue("Attach only to an installed preprod build with preserved QA data",
                Platform.getInstance().isTestFlightAttachConfigured()
                        && Platform.getInstance().getIOSBundleId().startsWith("com.vamapps.preprod."));
        PreparedMobileProfile.assertPreservationAllowed();
        Assert.assertTrue("Use the explicitly authorized test store",
                Platform.getInstance().isStoreKitSandboxEnabled() && Platform.getInstance().isTestPurchaseAllowed());
        super.setUp();
        purchase = new IOSProgramPurchasePageObject(driver);
        selection = new IOSProgramSelectionPageObject(driver);
    }

    @Override protected void cleanupSessionForNextTest() {
        // Never accept an offer or finish program selection in cleanup.
    }

    private MobileCaseFixture prepare() throws Exception {
        MobileCaseFixture f = MobileCaseFixture.load("COA-9787");
        ProgramSelectionApi.assertDedicatedProfile("COA-9787");
        ProgramSelectionState before = ProgramSelectionApi.read();
        before.assertRoles(null, Collections.emptyList());
        Assert.assertEquals("Fixture already has paid access", false, before.raw().get("subscription_active"));
        for (Object count : ((Map<?, ?>) before.raw().get("achievements")).values())
            Assert.assertEquals("Fixture already completed content", 0, ((Number) count).intValue());
        purchase.waitForElementVisible(f.selector("paywall.ready"), "Prepared initial Paywall is absent", 20);
        assertSelectionNotEntered();
        return f;
    }

    @Test @Issue("COA-9787")
    public void cancelledPurchaseKeepsTheCurrentPaywallAndUnchangedProgramState() throws Exception {
        MobileCaseFixture f = prepare();
        ProgramSelectionState before = ProgramSelectionApi.read();
        purchase.startPurchaseAndWaitForStore();
        purchase.cancelTestPurchase();
        purchase.waitForElementVisible(f.selector("paywall.ready"), "Cancel did not return to the current Paywall", 20);
        assertSelectionNotEntered();
        ProgramSelectionApi.read().assertExactlyEquals(before);
    }

    @Test @Issue("COA-9787")
    public void successfulPurchaseEntersRecommendationBeforeAnyDailyPlan() throws Exception {
        MobileCaseFixture f = prepare();
        String mainId = f.required("main.id");
        purchase.startPurchaseAndWaitForStore();
        purchase.confirmTestPurchase();
        purchase.waitForRecommendationAfterSuccess(f.selector("paywall.ready"));
        Assert.assertFalse("Today appeared before program selection", selection.isElementVisible("id:Today"));
        Assert.assertFalse("Summary appeared before program selection", selection.isElementVisible("id:Your plan is ready"));
        ProgramSelectionApi.assertFreshOnboardingProfile();
        selection.assertRecommendation(ProgramSelectionApi.recommendation(), mainId);
        // The Summary -> Today -> cold-launch section is shared with COA-9792,
        // on its independently prepared Summary fixture; never auto-finish it here.
    }

    private void assertSelectionNotEntered() {
        assertOnboardingNotRepeated();
        Assert.assertFalse("Purchase outcome opened Secondary", selection.isElementVisible("id:What else would you like to work on?"));
        Assert.assertFalse("Purchase outcome opened Daily Plan", selection.isElementVisible("id:Today"));
    }

    private void assertOnboardingNotRepeated() {
        Assert.assertFalse("Recommendation is displayed unexpectedly",
                selection.isElementVisible("id:" + IOSProgramSelectionPageObject.RECOMMENDATION_TITLE));
        Assert.assertFalse("Summary is displayed unexpectedly", selection.isElementVisible("id:Your plan is ready"));
    }
}
