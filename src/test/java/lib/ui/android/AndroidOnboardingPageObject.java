package lib.ui.android;

import io.qameta.allure.Step;
import lib.ui.OnboardingGoal;
import lib.ui.OnboardingPageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.List;
import java.util.Locale;

public class AndroidOnboardingPageObject extends OnboardingPageObject {
    private static final String APP_PACKAGE = "com.vamapps.thecoach";
    private static final String ID_PREFIX = APP_PACKAGE + ":id/";

    private static final String START_BUTTON = "id:" + ID_PREFIX + "btnSignInAnonymous";
    private static final String GOAL_TITLE = "id:" + ID_PREFIX + "tvAcive";
    private static final String CONTINUE_BUTTON = "id:" + ID_PREFIX + "bContinue";
    private static final String QUESTION_NUMBER = "id:" + ID_PREFIX + "tvAnswerNum";
    private static final String QUESTIONNAIRE_WEBVIEW =
            "xpath://android.webkit.WebView[.//android.widget.TextView[starts-with(@text,'STEP ')]]";
    private static final String PAYWALL_ROOT = "id:" + ID_PREFIX + "clPaywallContainer";
    private static final String PAYWALL_CLOSE = "id:" + ID_PREFIX + "btnClose";
    private static final String TODAY_TAB = "id:" + ID_PREFIX + "nav_graph_daily";

    private static final By SEMANTIC_QUESTIONNAIRE_ACTION = By.xpath(
            "//*[@enabled='true' and "
                    + "(translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='GOT IT' "
                    + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='CONTINUE' "
                    + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='NEXT' "
                    + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='LET’S GO' "
                    + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=\"LET'S GO\")]"
    );
    private static final By QUESTIONNAIRE_ANSWER = By.xpath(
            "(//android.webkit.WebView//android.view.View["
                    + "count(android.widget.TextView)=1 and count(android.view.View)=1]"
                    + "/android.widget.TextView[string-length(normalize-space(@text)) > 0])[1]"
    );

    public AndroidOnboardingPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verify Android fresh Start screen")
    public void assertFreshStartIsDisplayed() {
        waitForElementPresent(
                START_BUTTON,
                "Android is not on the fresh new-user Start screen. Run with fullReset=true and noReset=false.",
                20
        );
    }

    @Override
    @Step("Open Android Start now")
    public void openStartFlow() {
        waitForElementAndClick(START_BUTTON, "Cannot tap the first Android Start now button", 20);
        waitForElementPresent(GOAL_TITLE, "Android onboarding goal screen did not open", 20);
    }

    @Override
    @Step("Select Android onboarding goal: {goal.displayName}")
    public void selectGoal(OnboardingGoal goal) {
        String uppercaseGoal = goal.getDisplayName().toUpperCase(Locale.US);
        String goalLocator = "xpath://android.widget.Button["
                + "translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')="
                + xpathLiteral(uppercaseGoal) + "]";
        waitForElementAndClick(
                goalLocator,
                "Cannot select Android onboarding goal '" + goal.getDisplayName() + "'",
                20
        );

        WebElement continueButton = waitForElementPresent(
                CONTINUE_BUTTON,
                "Android additional-goals Continue button is not displayed",
                15
        );
        if (!isElementEnabled(continueButton)) {
            WebElement secondaryGoal = firstDisplayedEnabled(By.xpath(
                    "//android.widget.Button[translate(normalize-space(@text), "
                            + "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')="
                            + "'INCREASE SELF CONFIDENCE' and @enabled='true']"
            ));
            if (secondaryGoal == null) {
                secondaryGoal = firstDisplayedEnabled(By.xpath(
                    "//android.widget.Button[@clickable='true' and @enabled='true' "
                            + "and string-length(normalize-space(@text)) > 0 "
                            + "and translate(normalize-space(@text), "
                            + "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')!='CONTINUE']"
                ));
            }
            Assert.assertNotNull(
                    "Android requires an additional goal, but no selectable option is exposed",
                    secondaryGoal
            );
            secondaryGoal.click();
        }

        waitForElementAndClick(
                CONTINUE_BUTTON,
                "Cannot continue from Android additional-goals screen",
                15
        );
        waitForFirstElementPresent(
                new String[]{QUESTION_NUMBER, QUESTIONNAIRE_WEBVIEW, PAYWALL_ROOT, TODAY_TAB},
                "Android questionnaire did not start after goal selection",
                30
        );
    }

    @Override
    @Step("Complete Android runtime questionnaire")
    public void completeQuestionnaire() {
        for (int actionNumber = 1; actionNumber <= MAX_QUESTIONNAIRE_ACTIONS; actionNumber++) {
            if (questionnaireDestinationIsVisible()) {
                return;
            }

            WebElement action = firstDisplayedEnabled(SEMANTIC_QUESTIONNAIRE_ACTION);
            if (action == null) {
                action = firstDisplayedEnabled(QUESTIONNAIRE_ANSWER);
            }
            if (action == null) {
                waitForQuestionnaireActionOrDestination(20);
                if (questionnaireDestinationIsVisible()) {
                    return;
                }
                action = firstDisplayedEnabled(SEMANTIC_QUESTIONNAIRE_ACTION);
                if (action == null) {
                    action = firstDisplayedEnabled(QUESTIONNAIRE_ANSWER);
                }
            }
            Assert.assertNotNull(
                    "Android questionnaire is blocked at action " + actionNumber
                            + ". No semantic action or selectable answer is exposed.",
                    action
            );
            clickAndWaitForStateChange(action, 8);
        }

        Assert.fail(
                "Android questionnaire exceeded " + MAX_QUESTIONNAIRE_ACTIONS
                        + " actions without reaching a paywall or Today"
        );
    }

    private boolean questionnaireDestinationIsVisible() {
        return isElementPresent(PAYWALL_ROOT) || isElementPresent(TODAY_TAB);
    }

    private void waitForQuestionnaireActionOrDestination(long timeoutInSeconds) {
        try {
            createWait(timeoutInSeconds).until(webDriver ->
                    questionnaireDestinationIsVisible()
                            || firstDisplayedEnabled(SEMANTIC_QUESTIONNAIRE_ACTION) != null
                            || firstDisplayedEnabled(QUESTIONNAIRE_ANSWER) != null
            );
        } catch (Exception ignored) {
            // The assertion in completeQuestionnaire reports the blocked state
            // after the bounded wait. This specifically covers the animated
            // "Analyzing the answers" transition before the paywall is ready.
        }
    }

    @Override
    @Step("Close Android onboarding paywalls and optional popups")
    public int closePaywallsAndPopups() {
        int paywallsClosed = 0;
        for (int attempt = 0; attempt < MAX_TRANSIENT_SCREENS; attempt++) {
            WebElement paywallClose = firstDisplayedEnabled(By.id(ID_PREFIX + "btnClose"));
            boolean paywallVisible = isElementPresent(PAYWALL_ROOT) || paywallClose != null;
            if (paywallVisible) {
                Assert.assertNotNull(
                        "Android paywall is displayed without a semantic btnClose control",
                        paywallClose
                );
                String before = safePageSource();
                paywallClose.click();
                waitForSourceToChange(before, 12);
                Assert.assertFalse(
                        "Android paywall close did not dismiss or advance the paywall. "
                                + "Back navigation is intentionally not used because it returns to onboarding.",
                        before.equals(safePageSource()) && isElementPresent(PAYWALL_ROOT)
                );
                paywallsClosed++;
                continue;
            }

            WebElement optionalClose = firstDisplayedEnabled(
                    By.xpath("//*[@enabled='true' and @clickable='true' and ("
                            + "@resource-id='" + ID_PREFIX + "ivClose' "
                            + "or @resource-id='" + ID_PREFIX + "ivButtonClose' "
                            + "or @resource-id='" + ID_PREFIX + "btnSkip' "
                            + "or @resource-id='com.android.permissioncontroller:id/permission_deny_button' "
                            + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='LATER' "
                            + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='NOT NOW' "
                            + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='MAYBE LATER' "
                            + "or translate(normalize-space(@text), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='SKIP')]"
                    )
            );
            if (optionalClose != null) {
                clickAndWaitForStateChange(optionalClose, 8);
                continue;
            }

            if (isElementPresent(TODAY_TAB)) {
                return paywallsClosed;
            }
            break;
        }
        return paywallsClosed;
    }

    @Override
    @Step("Wait for Android Today")
    public void waitForToday() {
        waitForElementPresent(TODAY_TAB, "Android Today tab did not open after onboarding", 30);
    }

    private WebElement firstDisplayedEnabled(By locator) {
        List<WebElement> candidates = driver.findElements(locator);
        for (WebElement candidate : candidates) {
            try {
                if (candidate.isDisplayed() && isElementEnabled(candidate)) {
                    return candidate;
                }
            } catch (StaleElementReferenceException ignored) {
                // The runtime questionnaire can rerender between polls.
            }
        }
        return null;
    }

    private void waitForSourceToChange(final String previousSource, long timeoutInSeconds) {
        try {
            createWait(timeoutInSeconds).until(webDriver -> !previousSource.equals(safePageSource()));
        } catch (Exception ignored) {
            // The assertion after the wait distinguishes a genuine stuck close.
        }
    }

    private String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        return "\"" + value + "\"";
    }
}
