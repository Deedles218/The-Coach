package tests;

import lib.FailureClassifier;
import lib.SessionStateRecoveryException;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;

public class FailureClassifierUnitTests {
    @Test public void fixtureMismatchIsTestDataEvenWhenWrapped() {
        Assert.assertEquals("test-data", FailureClassifier.type(new RuntimeException(
                new AssertionError("Daily Plan day does not match the deterministic fixture. Expected Stage 1 but was Day 6"))));
    }

    @Test public void missingCredentialsAreNotProductFailures() {
        Assert.assertEquals("test-data", FailureClassifier.type(new IllegalStateException(
                "Required test data is missing. Configure COACH_KEGEL_PLAYER_EMAIL")));
    }

    @Test public void missingFrozenFixtureIsConfiguration() {
        Assert.assertEquals("configuration", FailureClassifier.type(new IllegalArgumentException(
                "Set -Dlocalization.fixture=<frozen UTF-8 properties file>")));
    }

    @Test public void sessionAndCleanupFailuresAreEnvironment() {
        Assert.assertEquals("environment", FailureClassifier.type(new SessionNotCreatedException("Simulator missing")));
        Assert.assertEquals("environment", FailureClassifier.type(
                new SessionStateRecoveryException("Could not restore entry state", new TimeoutException("Logout failed"))));
    }

    @Test public void uiTimeoutDoesNotClaimAConfirmedLocatorOrProductDefect() {
        TimeoutException failure = new TimeoutException("Explore content did not load");
        Assert.assertEquals("automation-sync", FailureClassifier.type(failure));
        Assert.assertEquals("needs-investigation", FailureClassifier.confidence(failure));
    }

    @Test public void assertionMismatchRequiresInvestigation() {
        Assert.assertEquals("assertion-mismatch", FailureClassifier.type(new AssertionError("Validation absent")));
        Assert.assertEquals("needs-investigation", FailureClassifier.confidence(new AssertionError("Validation absent")));
        Assert.assertEquals("needs-investigation", FailureClassifier.type(new IllegalStateException("Unexpected application state")));
    }

    @Test public void blockedPreconditionIsDistinctFromUnsupportedPlatform() {
        Assert.assertEquals("blocked-by-precondition", FailureClassifier.type(new AssumptionViolatedException("BLOCKED: fixture")));
        Assert.assertEquals("not-applicable", FailureClassifier.type(new AssumptionViolatedException("Android-only scenario")));
    }

    @Test public void observedExternalErrorEnrichesButDoesNotReplaceTheOriginalTimeout() {
        TimeoutException failure = new TimeoutException("Privacy screen did not open");
        String source = "<XCUIElementTypeWebView><XCUIElementTypeOther name=\"Error 502\" visible=\"true\">"
                + "<XCUIElementTypeStaticText label=\"502 - Bad Gateway .\"/></XCUIElementTypeOther></XCUIElementTypeWebView>";
        FailureClassifier.addPageEvidence(failure, source);
        FailureClassifier.addPageEvidence(failure, source);
        Assert.assertEquals(1, failure.getSuppressed().length);
        Assert.assertEquals("external-service", FailureClassifier.type(failure));
        Assert.assertEquals("high", FailureClassifier.confidence(failure));
        Assert.assertTrue(failure.getMessage().contains("Privacy screen did not open"));
    }

    @Test public void arbitraryHttpNumberInContentDoesNotBecomeAnExternalServiceDiagnosis() {
        TimeoutException failure = new TimeoutException("Lesson did not open");
        FailureClassifier.addPageEvidence(failure, "<XCUIElementTypeStaticText label=\"502 - Bad Gateway\"/>");
        Assert.assertEquals("automation-sync", FailureClassifier.type(failure));
        Assert.assertEquals(0, failure.getSuppressed().length);
    }

    @Test public void cachedHiddenErrorPageDoesNotOverrideCurrentFailure() {
        TimeoutException failure = new TimeoutException("Page is still loading");
        FailureClassifier.addPageEvidence(failure,
                "<XCUIElementTypeWebView><XCUIElementTypeOther name=\"Error 502\" visible=\"false\">"
                        + "502 - Bad Gateway</XCUIElementTypeOther></XCUIElementTypeWebView>");
        Assert.assertEquals("automation-sync", FailureClassifier.type(failure));
    }
}
