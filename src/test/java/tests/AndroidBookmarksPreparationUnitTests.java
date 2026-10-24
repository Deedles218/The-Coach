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
        Screen() { super(null); }
        @Override public void ensureExistingProgressUserIsLoggedIn(String email, String otp) { actions.add("login"); }
        @Override public void openProfile() { actions.add("profile"); profile = true; }
        @Override public void assertAuthenticatedProfileForEmail(String email) {
            actions.add("identity");
            if (rejectIdentity) throw new AssertionError("Wrong fixture");
        }
        @Override public void returnToMainScreenFromProfile() { actions.add("close-profile"); profile = false; }
        @Override public void openToday() {
            Assert.assertFalse("Android Profile hides the Today tab", profile);
            actions.add("today");
        }
    }
    private static class Scenario extends AndroidBookmarksTests {
        void prepareAccount(Screen screen, TestData.TestAccount account) { coach = screen; login(account); }
    }
    private void prepare(Screen screen) {
        String emailKey = "coach.bookmarks.content.email", otpKey = "coach.bookmarks.content.otp";
        String email = System.getProperty(emailKey), otp = System.getProperty(otpKey);
        try {
            System.setProperty(emailKey, "fixture@example.invalid");
            System.setProperty(otpKey, "0000");
            new Scenario().prepareAccount(screen, TestData.bookmarksAccount("CONTENT"));
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
}
