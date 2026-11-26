package tests;

import lib.RestoringTestAction;
import lib.ui.android.AndroidCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Exercises the actual second-device ownership and cleanup boundary without a session. */
public class AndroidCartCleanupUnitTests {
    @Test public void authenticationOrIdentityFailureStillLogsOutAndQuits() throws Exception {
        for (String stage : Arrays.asList("authentication", "identity", "badge")) {
            List<String> calls = new ArrayList<>();
            AssertionError primary = new AssertionError(stage);
            try {
                AndroidCartTests.withSecondDeviceSession(() -> calls.add("welcome"), () -> {
                    calls.add(stage);
                    throw primary;
                }, () -> calls.add("logout"), () -> calls.add("quit"));
                Assert.fail("The scenario failure was lost");
            } catch (AssertionError actual) {
                Assert.assertSame(primary, actual);
            }
            Assert.assertEquals(Arrays.asList("welcome", stage, "logout", "quit"), calls);
        }
    }

    @Test public void logoutAndQuitFailuresAreSuppressedUnderThePrimaryFailure() throws Exception {
        AssertionError primary = new AssertionError("identity");
        IllegalStateException logout = new IllegalStateException("logout");
        IllegalStateException quit = new IllegalStateException("quit");
        try {
            AndroidCartTests.withSecondDeviceSession(() -> {}, () -> { throw primary; },
                    () -> { throw logout; }, () -> { throw quit; });
            Assert.fail("The scenario failure was lost");
        } catch (AssertionError actual) {
            Assert.assertSame(primary, actual);
            Assert.assertArrayEquals(new Throwable[]{logout, quit}, actual.getSuppressed());
        }
    }

    @Test public void unownedDeviceIsNotLoggedOutButItsDriverIsClosed() throws Exception {
        List<String> calls = new ArrayList<>();
        AssertionError primary = new AssertionError("Welcome is absent");
        try {
            AndroidCartTests.withSecondDeviceSession(() -> { throw primary; }, () -> calls.add("scenario"),
                    () -> calls.add("logout"), () -> calls.add("quit"));
            Assert.fail("Unexpected account was accepted");
        } catch (AssertionError actual) {
            Assert.assertSame(primary, actual);
        }
        Assert.assertEquals(Arrays.asList("quit"), calls);
    }

    @Test public void successfulScenarioCannotHideLogoutOrQuitFailure() throws Exception {
        IllegalStateException logout = new IllegalStateException("logout");
        IllegalStateException quit = new IllegalStateException("quit");
        try {
            AndroidCartTests.withSecondDeviceSession(() -> {}, () -> {},
                    () -> { throw logout; }, () -> { throw quit; });
            Assert.fail("Cleanup failures were lost");
        } catch (IllegalStateException actual) {
            Assert.assertSame(logout, actual);
            Assert.assertArrayEquals(new Throwable[]{quit}, actual.getSuppressed());
        }
    }

    @Test public void profileNavigationFailureDoesNotReplaceIdentityFailure() throws Exception {
        AssertionError identity = new AssertionError("wrong account");
        IllegalStateException navigation = new IllegalStateException("cannot leave Profile");
        List<String> calls = new ArrayList<>();
        AndroidCoachFlowPageObject coach = new AndroidCoachFlowPageObject(null) {
            @Override public void openToday() { calls.add("today"); }
            @Override public void openProfile() { calls.add("profile"); }
            @Override public void assertAuthenticatedProfileForEmail(String email) { throw identity; }
            @Override public void returnToMainScreenFromProfile() { calls.add("return"); throw navigation; }
        };
        try {
            AndroidCartTests.verifySecondDeviceIdentity(coach, "synthetic@example.invalid");
            Assert.fail("The account mismatch was lost");
        } catch (AssertionError actual) {
            Assert.assertSame(identity, actual);
            Assert.assertArrayEquals(new Throwable[]{navigation}, actual.getSuppressed());
        }
        Assert.assertEquals(Arrays.asList("today", "profile", "return"), calls);
    }

    @Test public void cartRestorationFailureStillAllowsLogoutAndQuit() throws Exception {
        List<String> calls = new ArrayList<>();
        IllegalStateException restoration = new IllegalStateException("cannot remove fixture item");
        try {
            AndroidCartTests.withSecondDeviceSession(() -> {}, () ->
                    RestoringTestAction.run(() -> calls.add("badge"), () -> { throw restoration; }),
                    () -> calls.add("logout"), () -> calls.add("quit"));
            Assert.fail("Restoration failure was lost");
        } catch (IllegalStateException actual) {
            Assert.assertSame(restoration, actual);
        }
        Assert.assertEquals(Arrays.asList("badge", "logout", "quit"), calls);
    }
}
