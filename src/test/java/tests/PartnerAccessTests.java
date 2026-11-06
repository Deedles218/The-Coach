package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.PartnerRecipientEvidence;
import lib.ui.ios.iOSPartnerAccessPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Epic("The Coach")
@Feature("Partner access")
@Issue("COA-9324")
public class PartnerAccessTests extends CoreTestCase {
    @Override
    @Before
    public void setUp() throws Exception {
        Assert.assertTrue("COA-9324 targets the iOS male production TestFlight build",
                Platform.getInstance().isIOS()
                        && "com.vamapps.The-Coach".equals(Platform.getInstance().getIOSBundleId()));
        Assert.assertTrue("Attach to TestFlight with ios.noReset=true, ios.fullReset=false and no ios.app",
                Platform.getInstance().isTestFlightAttachConfigured());
        Assert.assertTrue("Each method needs a fresh TestFlight install and zero offer counters; "
                        + "confirm preparation with -Dpartner.freshInstall=true. Logout alone is insufficient.",
                Boolean.getBoolean("partner.freshInstall"));
        super.setUp();
    }

    @Test
    @DisplayName("COA-9324 Partner offer appears every fifth opening, at most three times")
    @Description("Creates a new buyer, checks all openings 1 through 21, dismisses offers on 5, 10 and 15, "
            + "and verifies no fourth offer on opening 20 or the following opening. Counters persist through cold restarts.")
    @Severity(SeverityLevel.CRITICAL)
    public void testOfferEveryFifthOpeningStopsAfterThreeDisplays() {
        iOSPartnerAccessPageObject partner = new iOSPartnerAccessPageObject(driver);
        partner.completeBuyerOnboarding(Platform.getInstance().getOnboardingGoal());
        int shown = 0;
        for (int opening = 1; opening <= 21; opening++) {
            if (opening > 1) closeAndReopenCoachApplication();
            boolean expected = opening == 5 || opening == 10 || opening == 15;
            partner.assertOfferOnOpening(opening, expected);
            if (expected) {
                shown++;
                partner.dismissOffer();
                partner.assertOfferOnOpening(opening, false);
            }
        }
        Assert.assertEquals("Unexpected total number of partner offer displays", 3, shown);
    }

    @Test
    @Issue("COA-9687")
    @DisplayName("COA-9687 Cancelled partner purchase does not expose an invitation")
    @Description("Opens the offer on the fifth opening, checks the one-time partner product, cancels twice, "
            + "and verifies no invitation on Today or after cold restart. This is the UI cancellation contract; "
            + "license absence on the backend requires separate API evidence.")
    @Severity(SeverityLevel.CRITICAL)
    public void testCancelledPurchaseDoesNotExposeInvite() {
        requireSandbox(false);
        iOSPartnerAccessPageObject partner = openFifthOpeningOffer();
        partner.openPartnerPaywall();
        for (int attempt = 0; attempt < 2; attempt++) {
            partner.startPartnerStorePurchase();
            partner.cancelStorePurchase();
        }
        partner.closePartnerPaywall();
        partner.assertNoInvite();
        closeAndReopenCoachApplication();
        partner.waitForLaunchSurface();
        partner.dismissOffer();
        partner.assertNoInvite();
    }

    @Test
    @Issue("COA-9685")
    @io.qameta.allure.Issues({@Issue("COA-9688")})
    @DisplayName("COA-9685 Partner purchase, native share and activation unlock For Her content")
    @Description("Proves an unpaid recipient baseline, buys the one-time partner product in Sandbox, "
            + "shares twice with the same HTTPS invitation, checks the reminder hides after each share and returns on cold opening, "
            + "opens the real universal link and the previously locked lesson before and after restart, "
            + "reopens the same invitation for its recipient, checks the buyer's banner stays absent after activation, "
            + "and proves a second distinct unpaid recipient cannot reuse the invitation through UI and fresh membership reads. "
            + "Does not claim backend license fields, email delivery or deferred installation coverage.")
    @Severity(SeverityLevel.BLOCKER)
    public void testPurchaseShareAndActivatePartnerAccess() throws Exception {
        requireSandbox(true);
        iOSPartnerAccessPageObject partner = new iOSPartnerAccessPageObject(driver);
        partner.completeBuyerOnboarding(Platform.getInstance().getOnboardingGoal());
        partner.assertOfferOnOpening(1, false);
        partner.assertNoInvite();
        // Terminate buyer before preparing For Her: returning to it is cold opening 2.
        partner.prepareUnpaidRecipient();
        PartnerRecipientEvidence firstBefore = PartnerRecipientEvidence.capture(driver);
        Assert.assertFalse("First recipient already has Premium before purchase", firstBefore.active());
        for (int opening = 2; opening <= 5; opening++) {
            if (opening > 2) closeAndReopenCoachApplication();
            partner.assertOfferOnOpening(opening, opening == 5);
        }
        partner.openPartnerPaywall();
        partner.startPartnerStorePurchase();
        partner.confirmPartnerStorePurchase();
        verifyInvitationLifecycleAndSingleRecipient(partner, firstBefore);
    }

    protected void verifyInvitationLifecycleAndSingleRecipient(iOSPartnerAccessPageObject partner,
                                                               PartnerRecipientEvidence firstBefore) throws Exception {
        partner.assertInviteAvailable();
        closeAndReopenCoachApplication();
        partner.waitForLaunchSurface();
        partner.dismissOffer();
        partner.assertInviteAvailable();
        String invite = partner.copySharedInvite();
        closeAndReopenCoachApplication();
        partner.waitForLaunchSurface();
        partner.dismissOffer();
        partner.assertInviteAvailable();
        String resharedInvite = partner.copySharedInvite();
        Assert.assertTrue("One pending invitation must retain the same link across shares and app openings",
                invite.equals(resharedInvite));
        closeAndReopenCoachApplication();
        partner.waitForLaunchSurface();
        partner.dismissOffer();
        partner.assertInviteAvailable();
        verifyRecipientActivationAndSingleRecipient(partner, firstBefore.identityHash(), invite);
    }

    protected void verifyRecipientActivationAndSingleRecipient(iOSPartnerAccessPageObject partner,
                                                              String firstIdentityHash, String invite) throws Exception {
        partner.followInviteAfterRestart(invite);
        PartnerRecipientEvidence.waitForActivation(driver, firstIdentityHash);
        partner.assertRecipientAccess();
        partner.restartRecipientAndAssertAccess();
        PartnerRecipientEvidence firstAfter = PartnerRecipientEvidence.capture(driver);
        Assert.assertTrue("Activation changed the first recipient identity",
                firstIdentityHash.equals(firstAfter.identityHash()));
        Assert.assertTrue("First recipient did not receive Premium", firstAfter.active());
        partner.followInvite(invite);
        partner.assertRecipientAccess();
        closeAndReopenCoachApplication();
        partner.waitForLaunchSurface();
        partner.dismissOffer();
        partner.waitForInviteRemoval();
        partner.assertNoInvite();
        closeAndReopenCoachApplication();
        partner.waitForLaunchSurface();
        partner.dismissOffer();
        partner.assertNoInvite();
        partner.prepareSecondUnpaidRecipient();
        PartnerRecipientEvidence secondBefore = PartnerRecipientEvidence.capture(driver);
        Assert.assertFalse("A different recipient fixture is required",
                firstIdentityHash.equals(secondBefore.identityHash()));
        Assert.assertFalse("Second recipient already has Premium", secondBefore.active());
        partner.followInviteAfterRestart(invite);
        partner.assertRecipientLessonLocked();
        partner.followInvite(invite);
        partner.assertRecipientLessonLocked();
        PartnerRecipientEvidence.assertRemainsUnpaid(driver, secondBefore.identityHash());
        partner.assertRecipientLessonLocked();
    }

    private iOSPartnerAccessPageObject openFifthOpeningOffer() {
        iOSPartnerAccessPageObject partner = new iOSPartnerAccessPageObject(driver);
        partner.completeBuyerOnboarding(Platform.getInstance().getOnboardingGoal());
        for (int opening = 1; opening <= 5; opening++) {
            if (opening > 1) closeAndReopenCoachApplication();
            partner.assertOfferOnOpening(opening, opening == 5);
            if (opening == 1) partner.assertNoInvite();
        }
        return partner;
    }

    private void requireSandbox(boolean purchase) {
        Assert.assertTrue("Partner payment cases require the TestFlight/StoreKit sandbox: -Dstorekit.sandbox=true",
                Platform.getInstance().isStoreKitSandboxEnabled());
        if (purchase) {
            Assert.assertTrue("Enable sandbox confirmation with -Dpurchase.allow=true",
                    Platform.getInstance().isTestPurchaseAllowed());
            iOSPartnerAccessPageObject.required("recipientGoal");
            iOSPartnerAccessPageObject.required("recipientLesson");
        }
        iOSPartnerAccessPageObject.required("paywallTitle");
        iOSPartnerAccessPageObject.required("storeProduct");
        iOSPartnerAccessPageObject.required("storeOneTimeCharge");
        iOSPartnerAccessPageObject.required("storeTestNotice");
        iOSPartnerAccessPageObject.required("inviteTitle");
        iOSPartnerAccessPageObject.required("inviteButton");
    }

    @Override
    protected void cleanupSessionForNextTest() {
        // Keep this test-owned anonymous buyer/license for diagnosis; do not
        // logout, delete accounts, restore transactions or consume extra openings.
        // CoreTestCase still captures failures and closes the Appium session.
    }
}
