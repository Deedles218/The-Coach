package tests;

import lib.FailureClassifier;
import lib.PostAuthQuestionnaireException;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import java.util.Arrays;

public class IOSAuthorizationDestinationUnitTests {
    private static class Flow extends iOSCoachFlowPageObject {
        boolean goals;
        boolean unavailable;
        boolean waited;
        Flow() { super(null); }
        @Override public WebElement waitForFirstElementPresent(String[] locators, String message, long timeout) {
            Assert.assertTrue(Arrays.asList(locators).contains(ONBOARDING_GOALS_TITLE));
            Assert.assertTrue(Arrays.asList(locators).contains(TAB_TODAY));
            waited = true;
            if (unavailable) throw new TimeoutException(message);
            return null;
        }
        @Override public boolean isElementVisible(String locator) {
            Assert.assertEquals(ONBOARDING_GOALS_TITLE, locator);
            return goals;
        }
    }
    @Test public void todayRemainsSuccessfulDestination() {
        Flow flow = new Flow();
        flow.waitForAuthorizedDashboard();
        Assert.assertTrue(flow.waited);
    }
    @Test public void goalsFailAsTestDataWithoutImplicitProvisioning() {
        Flow flow = new Flow();
        flow.goals = true;
        try {
            flow.waitForAuthorizedDashboard();
            Assert.fail("Existing-progress fixture must not silently complete goals");
        } catch (PostAuthQuestionnaireException failure) {
            Assert.assertEquals("test-data", FailureClassifier.type(failure));
            Assert.assertTrue(failure.getMessage().contains("after OTP"));
        }
    }
    @Test(expected = TimeoutException.class) public void unrelatedDestinationStillFails() {
        Flow flow = new Flow();
        flow.unavailable = true;
        flow.waitForAuthorizedDashboard();
    }
}
