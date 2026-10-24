package tests;

import lib.CoreTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Offline regression checks: no driver, account, simulator or API requests. */
public class TestModelLifecycleUnitTests {
    @Test
    public void primaryFailureSurvivesTeardownFailure() {
        AssertionError primary = new AssertionError("fixture timeout");
        RuntimeException teardown = new IllegalStateException("session already gone");
        Throwable actual = evaluate(primary, teardown);
        Assert.assertSame(primary, actual);
        Assert.assertArrayEquals(new Throwable[]{teardown}, actual.getSuppressed());
    }

    @Test
    public void teardownFailureStillFailsSuccessfulScenario() {
        RuntimeException teardown = new IllegalStateException("cleanup failed");
        Assert.assertSame(teardown, evaluate(null, teardown));
    }

    @Test
    public void successfulLifecycleDoesNotInventFailure() {
        Assert.assertNull(evaluate(null, null));
    }

    @Test
    public void failedRecoveryBlocksNextScenarioBeforeItsBody() throws Throwable {
        final boolean[] executed = {false};
        CoreTestCase fixture = new CoreTestCase() {
            @Override protected boolean isAllureReportingEnabled() { return false; }
            @Override public void tearDown() {
                throw new lib.SessionStateRecoveryException("Entry state could not be restored", null);
            }
        };
        Description description = Description.createTestDescription("tests.OfflineRecoveryFixture", "first");
        Statement successfulBody = new Statement() {
            @Override public void evaluate() { }
        };
        try {
            fixture.sessionLifecycle.apply(successfulBody, description).evaluate();
            Assert.fail("Recovery failure must fail the first scenario");
        } catch (lib.SessionStateRecoveryException expected) {
            // The recovery failure stays visible; only dependent scenarios are blocked.
        }
        CoreTestCase next = new CoreTestCase() {
            @Override protected boolean isAllureReportingEnabled() { return false; }
        };
        Statement nextBody = new Statement() {
            @Override public void evaluate() { executed[0] = true; }
        };
        try {
            next.sessionLifecycle.apply(nextBody,
                    Description.createTestDescription("tests.OfflineRecoveryFixture", "second")).evaluate();
            Assert.fail("Dependent scenario must be blocked");
        } catch (org.junit.AssumptionViolatedException expected) {
            Assert.assertTrue(expected.getMessage().contains("BLOCKED: Session state recovery"));
        }
        Assert.assertFalse("Blocked scenario must not create a driver or run its body", executed[0]);
    }

    private Throwable evaluate(final Throwable bodyFailure, final RuntimeException teardownFailure) {
        CoreTestCase fixture = new CoreTestCase() {
            @Override
            protected boolean isAllureReportingEnabled() {
                return false;
            }

            @Override
            public void tearDown() {
                if (teardownFailure != null) throw teardownFailure;
            }
        };
        Statement body = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (bodyFailure != null) throw bodyFailure;
            }
        };
        try {
            fixture.sessionLifecycle.apply(body,
                    Description.createTestDescription(getClass(), "offline-fixture")).evaluate();
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }
}
