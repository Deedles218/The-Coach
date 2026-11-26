package tests;

import lib.FailureClassifier;
import lib.PostAuthQuestionnaireException;
import lib.ui.ios.iOSCoachFlowPageObject;
import lib.ui.ios.iOSOnboardingPageObject;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IOSAuthorizationDestinationUnitTests {
    private String previousGoal;
    @Before public void isolateGoal() {
        previousGoal = System.getProperty("coach.ios.postAuthGoal");
        System.clearProperty("coach.ios.postAuthGoal");
    }
    @After public void restoreGoal() {
        if (previousGoal == null) System.clearProperty("coach.ios.postAuthGoal");
        else System.setProperty("coach.ios.postAuthGoal", previousGoal);
    }
    private static class Flow extends iOSCoachFlowPageObject {
        boolean goals;
        boolean unavailable;
        boolean waited;
        boolean otp;
        boolean email;
        boolean welcome;
        boolean tabsOnly;
        String selectedGoal;
        boolean unexpectedQuestionnaire;
        boolean programIntro;
        boolean introClosed;
        int walkthroughPage;
        boolean shopIntro = true;
        final List<Integer> walkthroughActions = new ArrayList<>();
        int offerPage;
        final List<String> offerActions = new ArrayList<>();
        Flow() { super(new RemoteWebDriver() { }); }
        @Override public void ensureLoggedOutOnStartScreen() { }
        @Override public void loginWithEmailAndOtp(String email, String otpCode) { waitForAuthorizedDashboard(); }
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
            if (locator.contains("Introducing your main program:")) return programIntro;
            if (locator.contains("YOU'RE IN THE RIGHT PLACE") || "id:RESTORE".equals(locator)) return offerPage == 1;
            if (locator.contains("WITH A SPECIAL OFFER")) return offerPage == 2;
            if (locator.contains("Here's how your Daily Plan works")) return walkthroughPage == 2;
            if (locator.contains("Enhance your results with products from the Shop")) return walkthroughPage == 5 && shopIntro;
            if (locator.contains("XCUIElementTypePageIndicator")) {
                return locator.contains("@value='page " + walkthroughPage + " of 5'");
            }
            if (ONBOARDING_GOALS_TITLE.equals(locator)) return goals;
            if (OTP_SCREEN_TITLE.equals(locator)) return otp;
            if (LOGIN_SCREEN_TITLE.equals(locator)) return email;
            if (START_SCREEN_TITLE.equals(locator)) return welcome;
            if (TAB_TODAY.equals(locator)) return !unavailable;
            return !unavailable && !tabsOnly && offerPage == 0 && (PROFILE_BUTTON_LEGACY.equals(locator)
                    || "id:DailyPlanItem".equals(locator));
        }
        @Override protected void completePostLoginGoals(String goal) {
            new iOSOnboardingPageObject(null) {
                @Override public boolean isGoalSelectionDisplayed() { return goals; }
                @Override public boolean isElementVisible(String locator) { return false; }
                @Override protected boolean isUnexpectedPostLoginQuestionnaireVisible() { return unexpectedQuestionnaire; }
                @Override public void selectGoal(String value) { selectedGoal = value; goals = false; }
                @Override public void completeQuestionnaire() { Assert.fail("Ordinary login invoked full onboarding"); }
            }.completePostLoginGoalQuestions(goal);
        }
        @Override public void closePdfGuideUpsellIfPresent() { }
        @Override public void closeNotificationPromptIfPresent() { }
        @Override public void closeConnectEmailPromptIfPresent() { }
        @Override public WebElement waitForElementVisible(String locator, String message, long timeout) {
            if (!isElementVisible(locator)) throw new TimeoutException(message);
            return null;
        }
        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            if ("id:ic_outline_close".equals(locator) || "id:SpecialOfferClose".equals(locator)) {
                Assert.assertEquals(offerPage == 1 ? "id:ic_outline_close" : "id:SpecialOfferClose", locator);
                offerActions.add(locator);
                offerPage = offerPage == 1 ? 2 : 0;
                return null;
            }
            if (locator.contains("XCUIElementTypePageIndicator")) {
                Assert.assertTrue(isElementVisible(locator));
                Assert.assertTrue(locator.contains("ancestor::XCUIElementTypeOther"));
                String button = walkthroughPage == 5 ? "@name=\"LET'S START\"" : "@name='GOT IT'";
                Assert.assertTrue(locator.contains(button));
                walkthroughActions.add(walkthroughPage);
                walkthroughPage = walkthroughPage == 5 ? 0 : walkthroughPage + 1;
                return null;
            }
            Assert.assertTrue(locator.contains("Introducing your main program:"));
            Assert.assertTrue(locator.contains("@name='GOT IT'"));
            programIntro = false;
            introClosed = true;
            return null;
        }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            Assert.assertFalse(message, isElementVisible(locator));
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
            Assert.fail("Login must not guess a missing fixture goal");
        } catch (PostAuthQuestionnaireException failure) {
            Assert.assertEquals("test-data", FailureClassifier.type(failure));
            Assert.assertTrue(failure.getMessage().contains("after OTP"));
        }
    }
    @Test public void configuredLoginGoalsContinueToLoadedToday() {
        System.setProperty("coach.ios.postAuthGoal", "Improve sex skills");
        Flow flow = new Flow();
        flow.goals = true;
        flow.waitForAuthorizedDashboard();
        Assert.assertEquals("Improve sex skills", flow.selectedGoal);
        Assert.assertFalse(flow.goals);
    }
    @Test public void knownProgramIntroIsDismissedBeforeReportingToday() {
        Flow flow = new Flow();
        flow.programIntro = true;
        flow.waitForAuthorizedDashboard();
        Assert.assertTrue(flow.introClosed);
    }
    @Test public void otherGotItControlsAreNotTappedWithoutTheProgramIntro() {
        Flow flow = new Flow();
        flow.waitForAuthorizedDashboard();
        Assert.assertFalse(flow.introClosed);
    }
    @Test public void knownLoginWalkthroughAdvancesThroughTheFinalPageInOrder() {
        Flow flow = new Flow();
        flow.walkthroughPage = 2;
        flow.waitForAuthorizedDashboard();
        Assert.assertEquals(Arrays.asList(2, 3, 4, 5), flow.walkthroughActions);
        Assert.assertEquals(0, flow.walkthroughPage);
    }
    @Test public void existingAccountLoginClosesBothObservedOffers() {
        Flow flow = new Flow();
        flow.offerPage = 1;
        flow.ensureExistingProgressUserIsLoggedIn("fixture@example.invalid", "test-code");
        Assert.assertEquals(Arrays.asList("id:ic_outline_close", "id:SpecialOfferClose"), flow.offerActions);
        Assert.assertEquals(0, flow.offerPage);
    }
    @Test public void offersStayVisibleOutsideExistingAccountPreparation() {
        Flow flow = new Flow();
        flow.offerPage = 1;
        try {
            flow.waitForAuthorizedDashboard();
            Assert.fail("Unrelated authorization consumed an offer");
        } catch (TimeoutException expected) {
            Assert.assertTrue(flow.offerActions.isEmpty());
            Assert.assertEquals(1, flow.offerPage);
        }
    }
    @Test public void unexpectedFinalWalkthroughPageIsNotTapped() {
        Flow flow = new Flow();
        flow.walkthroughPage = 2;
        flow.shopIntro = false;
        try {
            flow.waitForAuthorizedDashboard();
            Assert.fail("Login accepted an unidentified final walkthrough page");
        } catch (TimeoutException expected) {
            Assert.assertTrue(expected.getMessage().startsWith("Unexpected final login walkthrough page"));
            Assert.assertEquals(Arrays.asList(2, 3, 4), flow.walkthroughActions);
        }
    }
    @Test(expected = PostAuthQuestionnaireException.class)
    public void fullQuestionnaireAfterLoginGoalsIsNotCompleted() {
        System.setProperty("coach.ios.postAuthGoal", "Improve sex skills");
        Flow flow = new Flow();
        flow.goals = true;
        flow.unexpectedQuestionnaire = true;
        flow.waitForAuthorizedDashboard();
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
