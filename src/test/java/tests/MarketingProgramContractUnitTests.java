package lib.ui;

import lib.ui.android.AndroidCoachFlowPageObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.Collections;
import java.util.List;

/** Synthetic URLs and route states only. No deep link, browser, account or driver session is opened. */
public class MarketingProgramContractUnitTests {
    private String platform;
    private String email;
    private String otp;

    @Before public void configureSyntheticFixture() {
        platform = System.getProperty("platform");
        email = System.getProperty("coach.existingProgress.email");
        otp = System.getProperty("coach.existingProgress.otp");
        System.setProperty("platform", "android");
        System.setProperty("coach.existingProgress.email", "synthetic@example.invalid");
        System.setProperty("coach.existingProgress.otp", "0000");
    }

    @After public void restoreConfiguration() {
        restore("platform", platform);
        restore("coach.existingProgress.email", email);
        restore("coach.existingProgress.otp", otp);
    }

    private void restore(String property, String value) {
        if (value == null) System.clearProperty(property); else System.setProperty(property, value);
    }

    private void invalidLink(String link, String expected) {
        try {
            MarketingProgramPageObject.validatedLink(link, expected);
            Assert.fail("Invalid marketing fixture was accepted");
        } catch (AssertionError failure) {
            Assert.assertFalse("Diagnostic must not contain the synthetic private URL", failure.toString().contains("private-token"));
            Assert.assertNull("Parser exception must not expose the private URL", failure.getCause());
            Assert.assertFalse("Invalid marketing fixture was accepted".equals(failure.getMessage()));
        }
    }

    @Test public void missingLinkFailsInsteadOfPassingOrSkipping() { invalidLink(null, null); }
    @Test public void malformedLinkDoesNotLeakItsValueOrParserCause() { invalidLink("https://private-token bad", null); }
    @Test public void insecureLinkIsRejected() { invalidLink("http://example.invalid/private-token", null); }
    @Test public void embeddedCredentialsAreRejected() { invalidLink("https://private-token@example.invalid/", null); }
    @Test public void missingW2wCannotProveClassificationPriority() { invalidLink("https://example.invalid/private-token", "m_sias_new_agr"); }
    @Test public void substringMatchCannotSatisfyTheExactCampaign() { invalidLink("https://example.invalid/?w2w=m_sias_new_agr_extra&token=private-token", "m_sias_new_agr"); }
    @Test public void fragmentCannotMasqueradeAsCampaignQuery() { invalidLink("https://example.invalid/private-token#w2w=test_kegar_test", "test_kegar_test"); }
    @Test public void duplicateCampaignIsAmbiguousEvenIfOneValueMatches() { invalidLink("https://example.invalid/?w2w=m_sias_new_agr&w2w=other&token=private-token", "m_sias_new_agr"); }
    @Test public void encodedDuplicateCampaignIsAlsoRejected() { invalidLink("https://example.invalid/?w2w=one&w%32w=two&token=private-token", null); }
    @Test public void validExactCampaignMayAppearAfterOtherQueryFields() {
        String link = "https://example.invalid/?token=synthetic&w2w=m_sias_new_agr";
        Assert.assertEquals(link, MarketingProgramPageObject.validatedLink(link, "m_sias_new_agr").toString());
    }
    @Test public void encodedCampaignIsComparedAfterQueryDecoding() {
        MarketingProgramPageObject.validatedLink("https://example.invalid/?w%32w=test%5Fkegar%5Ftest", "test_kegar_test");
    }

    private enum Route { LOGIN, OTP, GOALS, TODAY }
    private static final class Driver extends RemoteWebDriver {
        Route route;
        boolean tabsBehindGoals;
        Driver(Route route) { this.route = route; }
        @Override public List<WebElement> findElements(By by) {
            String locator = by.toString();
            boolean goal = locator.contains("tvAcive") || locator.contains("WHAT DO YOU WANT TO ACHIEVE");
            boolean login = locator.contains("edtEmail") || locator.contains("ENTER THE MAIL THAT IS LINKED");
            boolean today = locator.endsWith(":id/nav_graph_daily") || locator.equals("By.id: Today");
            if (!(goal && route == Route.GOALS || login && route == Route.LOGIN
                    || today && (route == Route.TODAY || tabsBehindGoals))) return Collections.emptyList();
            return Collections.singletonList(new RemoteWebElement() {
                @Override public boolean isDisplayed() { return true; }
            });
        }
        @Override public WebElement findElement(By by) {
            List<WebElement> found = findElements(by);
            if (found.isEmpty()) throw new NoSuchElementException("Offline route has no such control");
            return found.get(0);
        }
        @Override public Object executeScript(String script, Object... args) {
            throw new AssertionError("Offline route checks must never execute a mobile command");
        }
    }

    private static final class Coach extends AndroidCoachFlowPageObject {
        final Driver routeDriver;
        final Route afterOtp;
        int emails, otps, openedToday;
        Coach(Driver driver, Route afterOtp) { super(driver); routeDriver = driver; this.afterOtp = afterOtp; }
        @Override public void typeLoginEmail(String email) { emails++; }
        @Override public void assertLoginContinueButtonIsEnabled() { }
        @Override public void submitLoginEmail() { routeDriver.route = Route.OTP; }
        @Override public void assertOtpScreenIsDisplayedForEmail(String email) { }
        @Override public void typeSecurityCode(String otp) { otps++; routeDriver.route = afterOtp; }
        @Override public void closeOtpConfirmationIfPresent() { }
        @Override public void openToday() { openedToday++; }
        @Override public void loginWithEmailAndOtp(String email, String otp) { Assert.fail("Ordinary login may mutate goals"); }
        @Override public void waitForAuthorizedDashboard() { Assert.fail("Ordinary dashboard wait may mutate goals"); }
    }

    private void expectGoalsFailure(MarketingProgramPageObject page) {
        try {
            page.waitForTodayOrSignIn();
            Assert.fail("GOALS was accepted as successful assignment");
        } catch (AssertionError failure) {
            Assert.assertTrue(failure.getMessage(), failure.getMessage().contains("assignment is unverified"));
        }
    }

    @Test public void goalRouteFailsBeforeEnteringAnyCredentials() {
        Driver driver = new Driver(Route.GOALS);
        Coach coach = new Coach(driver, Route.TODAY);
        expectGoalsFailure(new MarketingProgramPageObject(driver, coach));
        Assert.assertEquals(0, coach.emails + coach.otps + coach.openedToday);
    }
    @Test public void persistentTabsCannotHideAGoalOverlay() {
        Driver driver = new Driver(Route.GOALS); driver.tabsBehindGoals = true;
        Coach coach = new Coach(driver, Route.TODAY);
        expectGoalsFailure(new MarketingProgramPageObject(driver, coach));
        Assert.assertEquals(0, coach.openedToday);
    }
    @Test public void goalsAfterOtpFailWithoutChoosingAGoal() {
        Driver driver = new Driver(Route.LOGIN);
        Coach coach = new Coach(driver, Route.GOALS);
        expectGoalsFailure(new MarketingProgramPageObject(driver, coach));
        Assert.assertEquals(1, coach.emails);
        Assert.assertEquals(1, coach.otps);
        Assert.assertEquals(0, coach.openedToday);
    }
    @Test public void loginRouteUsesOnlyAuthenticationBeforeObservedToday() {
        Driver driver = new Driver(Route.LOGIN);
        Coach coach = new Coach(driver, Route.TODAY);
        new MarketingProgramPageObject(driver, coach).waitForTodayOrSignIn();
        Assert.assertEquals(1, coach.emails);
        Assert.assertEquals(1, coach.otps);
        Assert.assertEquals(1, coach.openedToday);
    }
    @Test public void directTodayRouteDoesNotLogIntoAnotherAccount() {
        Driver driver = new Driver(Route.TODAY);
        Coach coach = new Coach(driver, Route.TODAY);
        new MarketingProgramPageObject(driver, coach).waitForTodayOrSignIn();
        Assert.assertEquals(0, coach.emails + coach.otps);
        Assert.assertEquals(1, coach.openedToday);
    }
    @Test public void kegarCaseRefusesToStartUnrelatedOnboardingFromToday() {
        Driver driver = new Driver(Route.TODAY);
        try {
            new MarketingProgramPageObject(driver, new Coach(driver, Route.TODAY)).finishWithConflictingGoal();
            Assert.fail("Missing GOALS was accepted");
        } catch (AssertionError failure) {
            Assert.assertTrue(failure.getMessage(), failure.getMessage().contains("link itself to open GOALS"));
        }
    }
    @Test public void iosGoalHeadingIsReportedWithoutSelectingAReplacementGoal() {
        System.setProperty("platform", "ios");
        Driver driver = new Driver(Route.GOALS);
        Coach coach = new Coach(driver, Route.TODAY);
        expectGoalsFailure(new MarketingProgramPageObject(driver, coach));
        Assert.assertEquals(0, coach.emails + coach.otps + coach.openedToday);
    }

    @Test public void restartRejectsGoalsOnBothPlatformsEvenWhenTabsRemainVisible() {
        for (String platform : new String[]{"android", "ios"}) {
            System.setProperty("platform", platform);
            Driver driver = new Driver(Route.GOALS); driver.tabsBehindGoals = true;
            Coach coach = new Coach(driver, Route.TODAY);
            try {
                new MarketingProgramPageObject(driver, coach).waitForRestoredToday();
                Assert.fail("Restart accepted an unexpected GOALS route");
            } catch (AssertionError failure) {
                Assert.assertTrue(failure.getMessage(), failure.getMessage().contains("assignment is unverified"));
            }
            Assert.assertEquals(0, coach.emails + coach.otps + coach.openedToday);
        }
    }

    @Test public void restartRejectsLoginWithoutEnteringCredentials() {
        Driver driver = new Driver(Route.LOGIN);
        Coach coach = new Coach(driver, Route.TODAY);
        try {
            new MarketingProgramPageObject(driver, coach).waitForRestoredToday();
            Assert.fail("Restart authenticated a logged-out fixture");
        } catch (AssertionError failure) {
            Assert.assertTrue(failure.getMessage(), failure.getMessage().contains("returned to Login after restart"));
        }
        Assert.assertEquals(0, coach.emails + coach.otps + coach.openedToday);
    }

    @Test public void restartAcceptsObservedTodayWithoutAuthentication() {
        Driver driver = new Driver(Route.TODAY);
        Coach coach = new Coach(driver, Route.GOALS);
        new MarketingProgramPageObject(driver, coach).waitForRestoredToday();
        Assert.assertEquals(0, coach.emails + coach.otps);
        Assert.assertEquals(1, coach.openedToday);
    }
}
