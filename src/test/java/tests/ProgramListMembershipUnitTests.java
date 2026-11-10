package tests;

import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;

/** Catalog identity must preserve the distinct Retain variant and reject real mismatches. */
public class ProgramListMembershipUnitTests {
    @Test public void activeProgramMayAppearFirstInSelector() {
        TestModelAutomationTests.assertProgramListsMatch(
                Arrays.asList("Kegel Challenge", "Last Longer", "Keep It Hard"),
                Arrays.asList("Last Longer", "Keep It Hard", "Kegel Challenge"));
    }

    @Test public void retainVariantIsReportedAsRetainInsteadOfDuplicateLastLonger() {
        assertFailure("Retain must not be present", () ->
                TestModelAutomationTests.assertProgramListsMatch(
                        Arrays.asList("Last Longer", "Last Longer: Retain"),
                        Arrays.asList("Last Longer: Retain", "Last Longer")));
    }

    @Test public void actualDuplicateStillFails() {
        assertFailure("duplicate program entries", () ->
                TestModelAutomationTests.assertProgramListsMatch(
                        Arrays.asList("Last Longer", "Last Longer"), Arrays.asList("Last Longer")));
    }

    @Test public void lgbtqProgramRemainsDistinctFromSexIsASkill() {
        TestModelAutomationTests.assertProgramListsMatch(
                Arrays.asList("Kegel Journey", "Sex Is a Skill", "Sex Is a Skill (LGBTQ+)"),
                Arrays.asList("Sex Is a Skill (LGBTQ+)", "Kegel Journey", "Sex Is a Skill"));
    }

    @Test public void missingLgbtqVariantStillFails() {
        assertFailure("program lists differ", () ->
                TestModelAutomationTests.assertProgramListsMatch(
                        Arrays.asList("Sex Is a Skill"),
                        Arrays.asList("Sex Is a Skill (LGBTQ+)")));
    }

    @Test public void missingProgramStillFails() {
        assertFailure("program lists differ", () ->
                TestModelAutomationTests.assertProgramListsMatch(
                        Arrays.asList("Last Longer"), Arrays.asList("Last Longer", "Keep It Hard")));
    }

    private void assertFailure(String message, Runnable check) {
        try {
            check.run();
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains(message));
            return;
        }
        Assert.fail("Expected assertion containing: " + message);
    }
}
