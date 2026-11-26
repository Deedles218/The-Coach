package lib.ui;

import io.qameta.allure.Step;
import lib.Platform;
import lib.TestData;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/** UI entry and follow-up for the real marketing routes supplied with the Xray cases. */
public final class MarketingProgramPageObject extends MainPageObject {
    private final CoachFlowPageObject coach;
    private final String today;
    private final String login;
    private final String goal;

    public MarketingProgramPageObject(RemoteWebDriver driver) {
        this(driver, CoachFlowPageObjectFactory.get(driver));
    }

    MarketingProgramPageObject(RemoteWebDriver driver, CoachFlowPageObject coach) {
        super(driver);
        Platform platform = Platform.getInstance();
        this.coach = coach;
        if (platform.isAndroid()) {
            String id = "id:" + platform.getAndroidAppPackage() + ":id/";
            today = id + "nav_graph_daily";
            login = id + "edtEmail";
            String resource = id.substring(3);
            goal = "xpath://*[@resource-id='" + resource + "tvAcive' or @resource-id='" + resource + "tvAnswerNum']";
        } else {
            today = "id:Today";
            login = "xpath://XCUIElementTypeStaticText[contains(@name,'ENTER THE MAIL THAT IS LINKED')"
                    + " or contains(@name,'Enter the mail that is linked to your account')]";
            goal = "xpath://XCUIElementTypeStaticText[contains(translate(@name,"
                    + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'WHAT DO YOU WANT TO ACHIEVE')"
                    + " or @name='GOALS' or @name='WHAT ELSE DO YOU WANT TO ACHIEVE?']";
        }
    }

    @Step("Open the marketing link supplied for the selected Xray case")
    public void openConfiguredLink() {
        openConfiguredLink(null);
    }

    public void openConfiguredLink(String expectedW2w) {
        URI uri = validatedLink(System.getenv("COACH_MARKETING_LINK"), expectedW2w);
        Map<String, Object> args = new HashMap<>();
        args.put("url", uri.toString());
        if (Platform.getInstance().isAndroid()) {
            // Resolve the actual app link/redirect. Forcing the package can bypass it.
            args.put("waitForLaunch", true);
        }
        try {
            driver.executeScript("mobile: deepLink", args);
        } catch (WebDriverException failure) {
            // Appium may include the account-specific URL in the original error.
            throw new AssertionError("Appium did not open the marketing link. Check the app association and browser setup.");
        }
    }

    // No @Step: link values can contain account tokens. Never attach the URI parser's exception.
    static URI validatedLink(String link, String expectedW2w) {
        Assert.assertTrue("Set COACH_MARKETING_LINK to the reviewed case link", link != null && !link.trim().isEmpty());
        URI uri;
        try {
            uri = URI.create(link.trim());
        } catch (IllegalArgumentException invalid) {
            throw new AssertionError("COACH_MARKETING_LINK is not a valid URL");
        }
        Assert.assertTrue("The fixture must use a real HTTPS link without embedded credentials",
                "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null);
        int w2wCount = 0;
        String w2w = null;
        try {
            for (String entry : uri.getRawQuery() == null ? new String[0] : uri.getRawQuery().split("&")) {
                String[] pair = entry.split("=", 2);
                if ("w2w".equals(URLDecoder.decode(pair[0], "UTF-8"))) {
                    w2wCount++;
                    w2w = pair.length == 2 ? URLDecoder.decode(pair[1], "UTF-8") : "";
                }
            }
        } catch (IllegalArgumentException | UnsupportedEncodingException invalid) {
            throw new AssertionError("COACH_MARKETING_LINK has invalid query encoding");
        }
        Assert.assertTrue("The fixture link contains ambiguous duplicate w2w values", w2wCount <= 1);
        Assert.assertTrue("The fixture link must carry the exact w2w value required by Xray",
                expectedW2w == null || w2wCount == 1 && expectedW2w.equals(w2w));
        return uri;
    }

    @Step("Follow the in-app authorization route only if the marketing link requests it")
    public void waitForTodayOrSignIn() {
        waitForRoute(true);
        assertNoUnexpectedGoals();
        if (isElementVisible(login)) {
            TestData.TestAccount account = TestData.existingProgressAccount();
            // The link already opened Login. Avoid ordinary login helpers, which
            // can choose post-auth goals and thereby obscure marketing assignment.
            coach.typeLoginEmail(account.getEmail());
            coach.assertLoginContinueButtonIsEnabled();
            coach.submitLoginEmail();
            coach.assertOtpScreenIsDisplayedForEmail(account.getEmail());
            coach.typeSecurityCode(account.getOtp());
            if (Platform.getInstance().isAndroid()) {
                ((lib.ui.android.AndroidCoachFlowPageObject) coach).closeOtpConfirmationIfPresent();
            }
        }
        waitForRoute(false);
        assertNoUnexpectedGoals();
        coach.openToday();
    }

    @Step("Require the marketing fixture's Today after restart without changing authentication or goals")
    public void waitForRestoredToday() {
        // Observe Login as an unexpected state; do not authenticate the fixture again.
        waitForRoute(true);
        assertNoUnexpectedGoals();
        Assert.assertFalse("Marketing fixture returned to Login after restart; persistence is unverified",
                isElementVisible(login));
        coach.openToday();
    }

    private void waitForRoute(boolean allowLogin) {
        createWait(30).withMessage("Marketing route has no observed Today, GOALS"
                        + (allowLogin ? " or Login" : " after OTP") + "; verify the exact case fixture and link association")
                .until(ignored -> isElementVisible(goal) || isElementVisible(today)
                        || allowLogin && isElementVisible(login));
    }

    private void assertNoUnexpectedGoals() {
        Assert.assertFalse("Marketing route opened GOALS; assignment is unverified. Supply the approved case fixture"
                + " and observed route; this test will not choose a goal to make assignment pass.", isElementVisible(goal));
    }

    @Step("Finish the questionnaire with a goal that would normally select Sex Is a Skill")
    public void finishWithConflictingGoal() {
        waitForRoute(true);
        Assert.assertTrue("COA-9404 requires the marketing link itself to open GOALS on a disposable anonymous fixture;"
                + " no unrelated onboarding or account will be changed", isElementVisible(goal));
        coach.assertQuestionnaireOpenedWithoutLogin();
        OnboardingPageObject onboarding = OnboardingPageObjectFactory.get(driver);
        onboarding.selectGoal(OnboardingGoal.IMPROVE_SEX_SKILLS);
        onboarding.completeQuestionnaire();
        onboarding.closePaywallsAndPopups();
        onboarding.waitForToday();
    }
}
