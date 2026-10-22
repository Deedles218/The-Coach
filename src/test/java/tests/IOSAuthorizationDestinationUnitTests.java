package tests;

import lib.FailureClassifier;
import lib.PostAuthQuestionnaireException;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Arrays;

public class IOSAuthorizationDestinationUnitTests {
    private static class Flow extends iOSCoachFlowPageObject {
        boolean goals;
        boolean unavailable;
        boolean waited;
        boolean otp;
        boolean email;
        boolean welcome;
        boolean tabsOnly;
        Flow() { super(new RemoteWebDriver() { }); }
        @Override public WebDriverWait createWait(long timeout) {
            waited = true;
            return new WebDriverWait(driver, Duration.ZERO);
        }
        @Override public WebElement waitForFirstElementPresent(String[] locators, String message, long timeout) {
            Assert.assertTrue(Arrays.asList(locators).contains(ONBOARDING_GOALS_TITLE));
            Assert.assertTrue(Arrays.asList(locators).contains(TAB_TODAY));
            waited = true;
            if (unavailable) throw new TimeoutException(message);
            return null;
        }
        @Override public boolean isElementVisible(String locator) {
            if (ONBOARDING_GOALS_TITLE.equals(locator)) return goals;
            if (OTP_SCREEN_TITLE.equals(locator)) return otp;
            if (LOGIN_SCREEN_TITLE.equals(locator)) return email;
            if (START_SCREEN_TITLE.equals(locator)) return welcome;
            if (TAB_TODAY.equals(locator)) return !unavailable;
            return !unavailable && !tabsOnly && (PROFILE_BUTTON_LEGACY.equals(locator)
                    || "id:DailyPlanItem".equals(locator));
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

    @Test(expected = TimeoutException.class) public void todayBehindOtpDoesNotCompleteAuthorization() {
        Flow flow = new Flow();
        flow.otp = true;
        flow.waitForAuthorizedDashboard();
    }

    @Test(expected = TimeoutException.class) public void todayBehindEmailDoesNotCompleteAuthorization() {
        Flow flow = new Flow();
        flow.email = true;
        flow.waitForAuthorizedDashboard();
    }

    @Test(expected = TimeoutException.class) public void todayBehindWelcomeDoesNotCompleteAuthorization() {
        Flow flow = new Flow();
        flow.welcome = true;
        flow.waitForAuthorizedDashboard();
    }

    @Test(expected = TimeoutException.class) public void persistentTabsNeedLoadedDashboardContent() {
        Flow flow = new Flow();
        flow.tabsOnly = true;
        flow.waitForAuthorizedDashboard();
    }
}
