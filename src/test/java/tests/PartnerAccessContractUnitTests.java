package tests;

import lib.ui.ios.iOSPartnerAccessPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/** Keep a missing or stale share link from masquerading as a successful purchase. */
public class PartnerAccessContractUnitTests {
    @Test public void nativeStoreAuthenticationMatchesTheConfiguredAccount() {
        iOSPartnerAccessPageObject.assertStoreAccountMatches(
                "Enter the password for TESTER@EXAMPLE.COM to authorise this transaction.", "tester@example.com");
    }

    @Test(expected = AssertionError.class) public void nativeStoreAuthenticationRequiresAnExpectedAccount() {
        iOSPartnerAccessPageObject.assertStoreAccountMatches("Enter the password for tester@example.com", null);
    }

    @Test(expected = AssertionError.class) public void nativeStoreAuthenticationRejectsMissingAccount() {
        iOSPartnerAccessPageObject.assertStoreAccountMatches("Sign In", "tester@example.com");
    }

    @Test(expected = AssertionError.class) public void nativeStoreAuthenticationRejectsAmbiguousAccounts() {
        iOSPartnerAccessPageObject.assertStoreAccountMatches(
                "Enter the password for tester@example.com or other@example.com", "tester@example.com");
    }

    @Test public void wrongStoreAccountFailureDoesNotExposeEitherAccount() {
        AssertionError rejection = null;
        try {
            iOSPartnerAccessPageObject.assertStoreAccountMatches(
                    "Enter the password for private@example.com", "tester@example.com");
        } catch (AssertionError failure) {
            rejection = failure;
        }
        Assert.assertNotNull("A different native account was accepted", rejection);
        Assert.assertFalse(rejection.getMessage().contains("private@example.com"));
        Assert.assertFalse(rejection.getMessage().contains("tester@example.com"));
    }

    @Test public void readsInviteFromConfiguredShareText() {
        String url = "https://coach.example/invite-test?token=test-only";
        Assert.assertEquals(url, iOSPartnerAccessPageObject.inviteUrlFromSharedText(
                "This is your partner's access to The Coach app\n" + url));
    }

    @Test(expected = AssertionError.class) public void rejectsThePreviouslyReportedTextWithoutLink() {
        iOSPartnerAccessPageObject.inviteUrlFromSharedText("This is your partner's access to The Coach app");
    }

    @Test(expected = AssertionError.class) public void rejectsUnchangedClipboardMarker() {
        iOSPartnerAccessPageObject.inviteUrlFromSharedText("partner-e2e-test-marker");
    }

    @Test(expected = AssertionError.class) public void rejectsEmptyClipboard() {
        iOSPartnerAccessPageObject.inviteUrlFromSharedText(null);
    }

    @Test(expected = AssertionError.class) public void rejectsInsecureInvite() {
        iOSPartnerAccessPageObject.inviteUrlFromSharedText("http://coach.example/invite");
    }

    @Test(expected = AssertionError.class) public void rejectsAmbiguousSharePayload() {
        iOSPartnerAccessPageObject.inviteUrlFromSharedText(
                "https://coach.example/old-invite https://coach.example/new-invite");
    }

    @Test(expected = AssertionError.class) public void rejectsHostWithoutInvitePath() {
        iOSPartnerAccessPageObject.inviteUrlFromSharedText("https://coach.example");
    }

    @Test public void invalidUrlFailureDoesNotPrintInviteToken() {
        AssertionError rejection = null;
        try {
            iOSPartnerAccessPageObject.inviteUrlFromSharedText("https://coach.example/invite?token=%private");
        } catch (AssertionError failure) {
            rejection = failure;
        }
        Assert.assertNotNull("Malformed URI was accepted", rejection);
        Assert.assertFalse(rejection.getMessage().contains("%private"));
    }

    @Test(expected = TimeoutException.class) public void driverTimeoutCannotProveAnOfferIsAbsent() {
        iOSPartnerAccessPageObject page = new iOSPartnerAccessPageObject(new RemoteWebDriver() { }) {
            @Override public void waitForLaunchSurface() { }
            @Override public boolean isElementVisible(String locator) { throw new TimeoutException("Driver read failed"); }
        };
        page.assertOfferOnOpening(1, false);
    }

    @Test(expected = AssertionError.class) public void lateOfferIsDetectedAfterTodayFirstAppears() {
        iOSPartnerAccessPageObject page = new iOSPartnerAccessPageObject(new RemoteWebDriver() { }) {
            int observations;
            @Override public void waitForLaunchSurface() { }
            @Override public boolean isElementVisible(String locator) { return ++observations > 1; }
            @Override public WebDriverWait createWait(long timeout) {
                return new WebDriverWait(driver, Duration.ofSeconds(1));
            }
        };
        page.assertOfferOnOpening(1, false);
    }

    @Test public void purchaseCannotStartWithoutExplicitConfirmationApproval() {
        String previous = System.getProperty("purchase.allow");
        System.setProperty("purchase.allow", "false");
        try {
            new iOSPartnerAccessPageObject(new RemoteWebDriver() { }).confirmPartnerStorePurchase();
            Assert.fail("Purchase confirmation was attempted without approval");
        } catch (AssertionError rejection) {
            Assert.assertTrue(rejection.getMessage().contains("purchase.allow"));
        } finally {
            if (previous == null) System.clearProperty("purchase.allow");
            else System.setProperty("purchase.allow", previous);
        }
    }

    @Test public void approvalAloneCannotConfirmAnUnopenedNativeStore() {
        String previous = System.getProperty("purchase.allow");
        System.setProperty("purchase.allow", "true");
        try {
            new iOSPartnerAccessPageObject(new RemoteWebDriver() { }).confirmPartnerStorePurchase();
            Assert.fail("An unrelated system control could be clicked without entering the store");
        } catch (AssertionError rejection) {
            Assert.assertTrue(rejection.getMessage().contains("native store context"));
        } finally {
            if (previous == null) System.clearProperty("purchase.allow");
            else System.setProperty("purchase.allow", previous);
        }
    }
}
