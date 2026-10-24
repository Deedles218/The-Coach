package tests;

import lib.TestData;
import lib.ui.android.AndroidCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AndroidBookmarksPreparationUnitTests {
    private static class Screen extends AndroidCoachFlowPageObject {
        final List<String> actions = new ArrayList<>();
        boolean profile;
        boolean rejectIdentity;
        boolean rejectClose;
        Screen() { super(null); }
        @Override public void ensureExistingProgressUserIsLoggedIn(String email, String otp) { actions.add("login"); }
        @Override public void openProfile() { actions.add("profile"); profile = true; }
        @Override public void assertAuthenticatedProfileForEmail(String email) {
            actions.add("identity");
            if (rejectIdentity) throw new AssertionError("Wrong fixture");
        }
        @Override public void returnToMainScreenFromProfile() {
            actions.add("close-profile");
            if (rejectClose) throw new AssertionError("Profile did not close");
            profile = false;
        }
        @Override public void openToday() {
            Assert.assertFalse("Android Profile hides the Today tab", profile);
            actions.add("today");
        }
    }
    private static class Scenario extends AndroidBookmarksTests {
        void prepareAccount(Screen screen, TestData.TestAccount account) { coach = screen; login(account); }
        void verifyUpgradedAccount(Screen screen, TestData.TestAccount account) {
            coach = screen;
            verifyProfileAndReturnToToday(account);
        }
    }
    private void prepare(Screen screen) {
        withAccount(screen, false);
    }
    private void withAccount(Screen screen, boolean afterUpgrade) {
        String emailKey = "coach.bookmarks.content.email", otpKey = "coach.bookmarks.content.otp";
        String email = System.getProperty(emailKey), otp = System.getProperty(otpKey);
        try {
            System.setProperty(emailKey, "fixture@example.invalid");
            System.setProperty(otpKey, "0000");
            Scenario scenario = new Scenario();
            TestData.TestAccount account = TestData.bookmarksAccount("CONTENT");
            if (afterUpgrade) scenario.verifyUpgradedAccount(screen, account);
            else scenario.prepareAccount(screen, account);
        } finally {
            if (email == null) System.clearProperty(emailKey); else System.setProperty(emailKey, email);
            if (otp == null) System.clearProperty(otpKey); else System.setProperty(otpKey, otp);
        }
    }
    @Test public void verifiedProfileIsClosedBeforeReturningToToday() {
        Screen screen = new Screen(); prepare(screen);
        Assert.assertEquals(Arrays.asList("login", "profile", "identity", "close-profile", "today"), screen.actions);
    }
    @Test public void identityFailureIsNotRecoveredIntoASuccessfulPreparation() {
        Screen screen = new Screen(); screen.rejectIdentity = true;
        try { prepare(screen); Assert.fail("Wrong identity was accepted"); }
        catch (AssertionError expected) { Assert.assertEquals("Wrong fixture", expected.getMessage()); }
        Assert.assertEquals(Arrays.asList("login", "profile", "identity"), screen.actions);
    }
    @Test public void upgradedSessionClosesProfileBeforeTodayWithoutLoggingInAgain() {
        Screen screen = new Screen(); withAccount(screen, true);
        Assert.assertEquals(Arrays.asList("profile", "identity", "close-profile", "today"), screen.actions);
    }
    @Test public void wrongUpgradedIdentityCannotBeRepairedByLoggingInAgain() {
        Screen screen = new Screen(); screen.rejectIdentity = true;
        try { withAccount(screen, true); Assert.fail("Wrong upgraded identity was accepted"); }
        catch (AssertionError expected) { Assert.assertEquals("Wrong fixture", expected.getMessage()); }
        Assert.assertEquals(Arrays.asList("profile", "identity"), screen.actions);
    }
    @Test public void failedUpgradeProfileClosePreventsTodayAndDoesNotRelogin() {
        Screen screen = new Screen(); screen.rejectClose = true;
        try { withAccount(screen, true); Assert.fail("Failed Profile close was accepted"); }
        catch (AssertionError expected) { Assert.assertEquals("Profile did not close", expected.getMessage()); }
        Assert.assertEquals(Arrays.asList("profile", "identity", "close-profile"), screen.actions);
    }
}
