package tests;

import lib.RestoringTestAction;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Offline proof that program mutations are restored even when assertions or cleanup fail. */
public class RestoringTestActionUnitTests {
    @Test
    public void successfulScenarioRestoresOriginalStateExactlyOnce() throws Exception {
        final String[] activeProgram = {"original"};
        List<String> calls = new ArrayList<>();
        RestoringTestAction.run(() -> {
            calls.add("scenario");
            activeProgram[0] = "selected";
        }, () -> {
            calls.add("restore");
            Assert.assertEquals("selected", activeProgram[0]);
            activeProgram[0] = "original";
        });
        Assert.assertEquals("original", activeProgram[0]);
        Assert.assertEquals(Arrays.asList("scenario", "restore"), calls);
    }

    @Test
    public void checkedScenarioFailureStillRestoresOriginalState() {
        IOException failure = new IOException("selection verification failed");
        final String[] activeProgram = {"original"};
        Throwable actual = evaluate(() -> {
            activeProgram[0] = "selected";
            throw failure;
        }, () -> activeProgram[0] = "original");
        Assert.assertEquals("original", activeProgram[0]);
        Assert.assertSame(failure, actual);
        Assert.assertEquals(0, actual.getSuppressed().length);
    }

    @Test
    public void failedAssertionStillRestoresOriginalState() {
        AssertionError failure = new AssertionError("active card is not first");
        final String[] activeProgram = {"original"};
        Throwable actual = evaluate(() -> {
            activeProgram[0] = "selected";
            throw failure;
        }, () -> activeProgram[0] = "original");
        Assert.assertEquals("original", activeProgram[0]);
        Assert.assertSame(failure, actual);
    }

    @Test
    public void restorationFailureIsSuppressedUnderOriginalAssertion() {
        AssertionError scenarioFailure = new AssertionError("Today content is stale");
        IOException restoreFailure = new IOException("restoration request failed");
        Throwable actual = evaluate(() -> { throw scenarioFailure; }, () -> { throw restoreFailure; });
        Assert.assertSame(scenarioFailure, actual);
        Assert.assertArrayEquals(new Throwable[]{restoreFailure}, actual.getSuppressed());
    }

    @Test
    public void failedRestorationAssertionIsSuppressedUnderOriginalCheckedFailure() {
        IOException scenarioFailure = new IOException("selection request failed");
        AssertionError restoreFailure = new AssertionError("original program was not restored");
        Throwable actual = evaluate(() -> { throw scenarioFailure; }, () -> { throw restoreFailure; });
        Assert.assertSame(scenarioFailure, actual);
        Assert.assertArrayEquals(new Throwable[]{restoreFailure}, actual.getSuppressed());
    }

    @Test
    public void cleanupFailureAloneFailsSuccessfulScenario() {
        IOException restoreFailure = new IOException("restoration request failed");
        Assert.assertSame(restoreFailure, evaluate(() -> {}, () -> { throw restoreFailure; }));
    }

    @Test
    public void cleanupAssertionAloneFailsSuccessfulScenario() {
        AssertionError restoreFailure = new AssertionError("original program was not restored");
        Assert.assertSame(restoreFailure, evaluate(() -> {}, () -> { throw restoreFailure; }));
    }

    @Test
    public void sharedFailureInstanceCannotTriggerSelfSuppression() {
        IOException failure = new IOException("shared fixture failure");
        Throwable actual = evaluate(() -> { throw failure; }, () -> { throw failure; });
        Assert.assertSame(failure, actual);
        Assert.assertEquals(0, actual.getSuppressed().length);
    }

    private Throwable evaluate(RestoringTestAction.CheckedAction scenario,
                               RestoringTestAction.CheckedAction restore) {
        try {
            RestoringTestAction.run(scenario, restore);
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }
}
