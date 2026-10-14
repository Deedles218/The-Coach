package tests;

import io.qameta.allure.junit4.DisplayName;
import lib.AllureReportSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;

import java.lang.reflect.Method;

public class AllureReportSupportUnitTests {
    @Test
    public void displayNameHasPriorityOverMethodName() throws Exception {
        Method method = NamedScenario.class.getDeclaredMethod("testTechnicalMethodName");
        Description description = Description.createTestDescription(
                NamedScenario.class,
                method.getName(),
                method.getAnnotations()
        );

        Assert.assertEquals(
                "User can open Today after login",
                AllureReportSupport.readableTestName(description)
        );
    }

    @Test
    public void camelCaseFallbackProducesReadableScenarioName() {
        Description description = Description.createTestDescription(
                AllureReportSupportUnitTests.class,
                "test03Coa7950InvalidEmailIsRejected"
        );

        Assert.assertEquals(
                "COA 7950 Invalid Email Is Rejected",
                AllureReportSupport.readableTestName(description)
        );
    }

    @Test
    public void failureContextContainsActionAndSourceLocations() {
        AssertionError failure = new AssertionError("Continue must stay disabled");
        failure.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("lib.ui.CoachFlowPageObject", "assertContinueDisabled", "CoachFlowPageObject.java", 412),
                new StackTraceElement("tests.CoachAuthorizationTests", "testInvalidEmail", "CoachAuthorizationTests.java", 73)
        });
        Description description = Description.createTestDescription(
                "tests.CoachAuthorizationTests",
                "testInvalidEmail"
        );

        String context = AllureReportSupport.failureContext(failure, description);

        Assert.assertTrue(context.contains("Scenario: Invalid Email"));
        Assert.assertTrue(context.contains("Failed action: Assert Continue Disabled"));
        Assert.assertTrue(context.contains("CoachFlowPageObject.java:412"));
        Assert.assertTrue(context.contains("Called from test: tests.CoachAuthorizationTests#testInvalidEmail"));
    }

    private static class NamedScenario {
        @DisplayName("User can open Today after login")
        public void testTechnicalMethodName() {
        }
    }
}
