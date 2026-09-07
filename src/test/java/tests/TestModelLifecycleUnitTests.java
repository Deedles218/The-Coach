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

    private Throwable evaluate(final Throwable bodyFailure, final RuntimeException teardownFailure) {
        CoreTestCase fixture = new CoreTestCase() {
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
