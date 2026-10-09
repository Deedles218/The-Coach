package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ui.OnboardingGoal;
import lib.ui.OnboardingPageObject;
import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class iOSOnboardingPageObject extends OnboardingPageObject {
    private boolean preserveNotificationPermissionPrompt;
    private static final String START_BUTTON = "id:START NOW";
    private static final String GOAL_TITLE =
            "xpath://XCUIElementTypeStaticText[contains(translate(@name, "
                    + "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), "
                    + "'WHAT DO YOU WANT TO ACHIEVE')]";
    private static final String CONTINUE_BUTTON = "id:CONTINUE";
    private static final String PAYWALL_MARKER = "id:subscription_paywall";
    private static final String PAYWALL_MARKER_FALLBACK = "id:START FREE TRIAL";
    private static final String TESTFLIGHT_PAYWALL_CLOSE = "id:ic_outline_close";
    private static final String TESTFLIGHT_PAYWALL_RESTORE = "id:RESTORE";
    private static final String SPECIAL_OFFER_CLOSE = "id:SpecialOfferClose";
    private static final String TODAY_TAB = "id:Today";
    private static final By PROGRAM_PICKER_CARD = By.xpath(
            "//XCUIElementTypeOther[@name='ProgramSelectionCardView' and @visible='true']"
    );

    private static final By SEMANTIC_QUESTIONNAIRE_ACTION = By.xpath(
            "//XCUIElementTypeButton[@visible='true' and @enabled='true' and ("
                    + "translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='GOT IT' "
                    + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='CONTINUE' "
                    + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='NEXT' "
                    + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='LET’S GO' "
                    + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=\"LET'S GO\" "
                    + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='LET’S START' "
                    + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=\"LET'S START\")]"
    );
    private static final By NOTIFICATION_PROMPT_SKIP = By.xpath(
            "//XCUIElementTypeButton[@visible='true' and @enabled='true' "
                    + "and translate(normalize-space(@name), "
                    + "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='MAYBE LATER']"
    );
    private static final By QUESTIONNAIRE_ANSWER = By.xpath(
            "(//XCUIElementTypeWebView//XCUIElementTypeButton[@visible='true' and @enabled='true' "
                    + "and string-length(normalize-space(@name)) > 0 "
                    + "and not(@name='Back') and not(@name='Close')] "
                    + "| //XCUIElementTypeWebView//XCUIElementTypeStaticText[@visible='true' and @enabled='true' "
                    + "and number(@y) >= 300 and string-length(normalize-space(@name)) > 0])[1]"
    );
    private static final By NATIVE_QUESTIONNAIRE_ANSWER = By.xpath(
            "(//XCUIElementTypeOther[(@name='StartQuestionnaireOneTapAnswerView' "
                    + "or @name='StartQuestionnaireCheckedView') and @visible='true']"
                    + "//XCUIElementTypeStaticText[@visible='true' and @enabled='true' "
                    + "and string-length(normalize-space(@name)) > 0])[1]"
    );
    private static final By SELECTED_NATIVE_QUESTIONNAIRE_ANSWER = By.xpath(
            "//XCUIElementTypeOther[@name='StartQuestionnaireCheckedView' and @visible='true']"
                    + "//XCUIElementTypeButton[@visible='true' and @value='1']"
    );

    public iOSOnboardingPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Reach the iOS notification prompt without granting or skipping permission")
    public void completeFreshJourneyToNotificationPermission() {
        preserveNotificationPermissionPrompt = true;
        try {
            if (!notificationPermissionPromptIsVisible()) {
                completeNewUserJourneyToPaywall(lib.Platform.getInstance().getOnboardingGoal());
                closePaywallsAndPopupsUntil(this::notificationPermissionPromptIsVisible);
            }
            createWait(20).withMessage("iOS notification prompt did not appear during fresh onboarding")
                    .until(ignored -> notificationPermissionPromptIsVisible());
        } finally {
            preserveNotificationPermissionPrompt = false;
        }
    }

    private boolean notificationPermissionPromptIsVisible() {
        return isElementVisible("id:REMIND ME TO PRACTICE")
                || isElementVisible("xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track' and @visible='true']");
    }

    @Override
    @Step("Verify iOS fresh TestFlight Start screen")
    public void assertFreshStartIsDisplayed() {
        waitForElementPresent(
                START_BUTTON,
                "iOS is not on the fresh new-user Start screen. Reinstall The Coach from TestFlight before this test.",
                25
        );
    }

    @Override
    @Step("Open iOS Start now")
    public void openStartFlow() {
        if (isElementVisible(GOAL_TITLE)
                || isPreQuestionnairePromptVisible()
                || questionnaireOrDestinationIsVisible()) {
            return;
        }
        waitForElementAndClick(START_BUTTON, "Cannot tap the first iOS Start now button", 20);
        waitForElementPresent(GOAL_TITLE, "iOS onboarding goal screen did not open", 20);
    }

    @Override
    @Step("Select iOS onboarding goal: {goal.displayName}")
    public void selectGoal(OnboardingGoal goal) {
        selectGoal(goal.getDisplayName());
    }

    @Step("Complete fresh iOS questionnaire for configured goal: {goal}")
    public void completeNewUserJourneyToPaywall(String goal) {
        assertFreshStartIsDisplayed();
        openStartFlow();
        selectGoal(goal);
        completeQuestionnaire();
    }

    @Step("Select configured iOS goal: {goal}")
    public void selectGoal(String goal) {
        if (!isElementVisible(GOAL_TITLE)) {
            closePreQuestionnairePrompts();
            completeAdditionalGoalsIfPresent();
            Assert.assertTrue(
                    "iOS resumed after goal selection, but questionnaire did not appear",
                    questionnaireOrDestinationIsVisible()
            );
            return;
        }
        String expected = goal.toUpperCase(Locale.US);
        String goalLocator = "xpath:(//XCUIElementTypeButton | //XCUIElementTypeStaticText)["
                + "@visible='true' and translate(normalize-space(@name), "
                + "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')="
                + xpathLiteral(expected) + "]";
        waitForElementAndClick(
                goalLocator,
                "Cannot select iOS onboarding goal '" + goal + "'",
                20
        );

        createWait(30).until(webDriver ->
                isElementVisible(CONTINUE_BUTTON)
                        || questionnaireOrDestinationIsVisible()
                        || isPreQuestionnairePromptVisible()
        );
        closePreQuestionnairePrompts();
        completeAdditionalGoalsIfPresent();
    }

    @Override
    @Step("Complete iOS runtime questionnaire")
    public void completeQuestionnaire() {
        for (int actionNumber = 1; actionNumber <= MAX_QUESTIONNAIRE_ACTIONS; actionNumber++) {
            if (preserveNotificationPermissionPrompt && notificationPermissionPromptIsVisible()) return;
            if (acceptSystemAlertIfPresent()) {
                continue;
            }
            if (dismissProgramPickerIfPresent()) {
                continue;
            }
            if (isPaywallVisible() || isElementVisible(TODAY_TAB)) {
                return;
            }

            WebElement action = firstDisplayedEnabled(NOTIFICATION_PROMPT_SKIP);
            if (action == null) {
                action = firstDisplayedEnabled(SEMANTIC_QUESTIONNAIRE_ACTION);
            }
            WebElement nativeAnswer = firstDisplayedEnabled(NATIVE_QUESTIONNAIRE_ANSWER);
            boolean continueNeedsAnswer = action != null
                    && "CONTINUE".equals(elementText(action).toUpperCase(Locale.US))
                    && nativeAnswer != null
                    && firstDisplayedEnabled(SELECTED_NATIVE_QUESTIONNAIRE_ANSWER) == null;
            if (action == null || continueNeedsAnswer) {
                action = nativeAnswer;
            }
            if (action == null) {
                action = firstDisplayedEnabled(QUESTIONNAIRE_ANSWER);
            }
            Assert.assertNotNull(
                    "iOS questionnaire is blocked at action " + actionNumber
                            + ". No semantic action or selectable WebView answer is exposed.",
                    action
            );
            if (requiresCoordinateTap(action)) {
                tapElementCenterAndWaitForStateChange(action, 8);
            } else {
                clickAndWaitForStateChange(action, 8);
            }
        }

        Assert.fail(
                "iOS questionnaire exceeded " + MAX_QUESTIONNAIRE_ACTIONS
                        + " actions without reaching a paywall or Today"
        );
    }

    @Override
    @Step("Close iOS onboarding paywalls and optional popups")
    public int closePaywallsAndPopups() {
        return closePaywallsAndPopupsUntil(() -> false);
    }

    @Step("Close paywalls without consuming the first product onboarding slide")
    public int closePaywallsBeforeSlides(String firstSlideHeader) {
        int closed = closePaywallsAndPopupsUntil(() -> isElementVisible("id:" + firstSlideHeader));
        waitForElementPresent("id:" + firstSlideHeader,
                "Product onboarding did not appear; verify first-visit state and active configuration", 30);
        return closed;
    }

    @Step("Close optional paywalls while preserving any reappearing product slide")
    public int closePaywallsWithoutConsumingSlides(String[] slideHeaders) {
        return closePaywallsAndPopupsUntil(() -> {
            for (String header : slideHeaders) {
                if (isElementVisible("id:" + header)) return true;
            }
            return false;
        });
    }

    private int closePaywallsAndPopupsUntil(BooleanSupplier destinationVisible) {
        int paywallsClosed = 0;
        for (int attempt = 0; attempt < MAX_TRANSIENT_SCREENS; attempt++) {
            if (destinationVisible.getAsBoolean()) {
                return paywallsClosed;
            }
            if (acceptSystemAlertIfPresent()) {
                continue;
            }
            if (dismissProgramPickerIfPresent()) {
                continue;
            }

            if (isPaywallVisible()) {
                WebElement close = firstDisplayedEnabled(By.xpath(
                        "//XCUIElementTypeButton[@visible='true' and @enabled='true' and ("
                                + "@name='subscription_close' or @name='CloseRoundBlack' "
                                + "or @name='navBarRoundClose' or @name='ic outline close' "
                                + "or @name='ic_outline_close' "
                                + "or @name='SpecialOfferClose' "
                                + "or @name='Close' or @label='Close')]"
                ));
                Assert.assertNotNull(
                        "iOS subscription paywall is displayed without an accessible close control",
                        close
                );
                String before = safePageSource();
                close.click();
                waitForSourceToChange(before, 12);
                Assert.assertFalse(
                        "iOS paywall close did not dismiss or advance the paywall",
                        before.equals(safePageSource()) && isPaywallVisible()
                );
                paywallsClosed++;
                continue;
            }

            WebElement optionalClose = firstDisplayedEnabled(By.xpath(
                    "//XCUIElementTypeButton[@visible='true' and @enabled='true' and ("
                            + "@name='push_permission_close' or @name='connect_email_later' "
                            + "or @name='CloseRoundBlack' or @name='ic outline close' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='LATER' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='NOT NOW' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='MAYBE LATER' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='SKIP' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='GOT IT' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='LET’S START' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=\"LET'S START\" "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='ALLOW' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='REMIND ME TO PRACTICE' "
                            + "or translate(normalize-space(@name), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK')]"
            ));
            if (optionalClose != null) {
                clickAndWaitForStateChange(optionalClose, 8);
                continue;
            }

            if (isElementVisible(TODAY_TAB)) {
                return paywallsClosed;
            }
            break;
        }
        return paywallsClosed;
    }

    @Override
    @Step("Wait for iOS Today")
    public void waitForToday() {
        createWait(30).withMessage("iOS Today tab did not open after onboarding").until(webDriver -> {
            WebElement notificationClose = firstDisplayedEnabled(By.xpath(
                    "//XCUIElementTypeStaticText[@name='Allow notifications to stay on track' and @visible='true']"
                            + "/../XCUIElementTypeButton[1]"));
            if (notificationClose != null) {
                notificationClose.click();
                return false;
            }
            return isElementVisible(TODAY_TAB);
        });
    }

    private boolean questionnaireOrDestinationIsVisible() {
        return (preserveNotificationPermissionPrompt && notificationPermissionPromptIsVisible())
                || isPaywallVisible()
                || isElementVisible(TODAY_TAB)
                || isProgramPickerVisible()
                || !driver.findElements(By.xpath("//XCUIElementTypeWebView[@visible='true']")).isEmpty()
                || !driver.findElements(By.xpath(
                        "//XCUIElementTypeStaticText[starts-with(@name,'QUESTION ') "
                                + "or starts-with(@name,'STEP ')]"
                )).isEmpty()
                || !driver.findElements(SEMANTIC_QUESTIONNAIRE_ACTION).isEmpty();
    }

    private boolean isPreQuestionnairePromptVisible() {
        return !driver.findElements(By.xpath(
                "//XCUIElementTypeButton[@visible='true' and ("
                        + "@name='REMIND ME TO PRACTICE' or @name='MAYBE LATER')]"
        )).isEmpty();
    }

    private void closePreQuestionnairePrompts() {
        for (int attempt = 0; attempt < 6; attempt++) {
            if (preserveNotificationPermissionPrompt && notificationPermissionPromptIsVisible()) return;
            if (acceptSystemAlertIfPresent()) {
                continue;
            }
            WebElement skipNotifications = firstDisplayedEnabled(NOTIFICATION_PROMPT_SKIP);
            if (skipNotifications == null) {
                break;
            }
            clickAndWaitForStateChange(skipNotifications, 8);
        }
        acceptPendingSystemAlerts();
        if (!questionnaireOrDestinationIsVisible() && !isElementVisible(CONTINUE_BUTTON)) {
            createWait(30).until(webDriver ->
                    questionnaireOrDestinationIsVisible() || isElementVisible(CONTINUE_BUTTON)
            );
        }
    }

    private void completeAdditionalGoalsIfPresent() {
        if (!isElementVisible("xpath://XCUIElementTypeStaticText[@name='WHAT ELSE DO YOU WANT TO ACHIEVE?']")) {
            return;
        }
        acceptPendingSystemAlerts();
        WebElement continueButton = waitForElementPresent(
                CONTINUE_BUTTON,
                "iOS additional-goals Continue button is not displayed",
                5
        );
        if (!isElementEnabled(continueButton)
                || isElementVisible("xpath://XCUIElementTypeStaticText[@name='WHAT ELSE DO YOU WANT TO ACHIEVE?']")) {
            WebElement secondaryGoal = firstDisplayedEnabled(By.xpath(
                    "//XCUIElementTypeOther[@name='StartQuestionnaireCheckedView' and @visible='true']"
                            + "//XCUIElementTypeStaticText[@visible='true' and @enabled='true' and "
                            + "translate(normalize-space(@name), "
                            + "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')="
                            + "'INCREASE SELF CONFIDENCE']"
            ));
            Assert.assertNotNull(
                    "iOS requires an additional goal, but Increase self confidence is not exposed",
                    secondaryGoal
            );
            tapElementCenterAndWaitForStateChange(secondaryGoal, 8);
            Assert.assertNotNull(
                    "iOS additional goal tap did not select Increase self confidence",
                    firstDisplayedEnabled(By.xpath(
                            "//XCUIElementTypeOther[@name='StartQuestionnaireCheckedView' and @visible='true' "
                                    + "and .//XCUIElementTypeStaticText[@name='INCREASE SELF CONFIDENCE']]"
                                    + "//XCUIElementTypeButton[@visible='true' and @value='1']"
                    ))
            );
        }

        continueButton = waitForElementPresent(
                CONTINUE_BUTTON,
                "Cannot continue from iOS additional-goals screen",
                15
        );
        tapElementCenterAndWaitForStateChange(continueButton, 12);
        createWait(30).until(webDriver -> questionnaireOrDestinationIsVisible());
    }

    private boolean isPaywallVisible() {
        return isElementVisible(PAYWALL_MARKER)
                || isElementVisible("id:subscription_error_illustration")
                || isElementVisible(PAYWALL_MARKER_FALLBACK)
                || isElementVisible(SPECIAL_OFFER_CLOSE)
                || (isElementVisible(TESTFLIGHT_PAYWALL_CLOSE)
                && isElementVisible(TESTFLIGHT_PAYWALL_RESTORE));
    }

    private boolean isProgramPickerVisible() {
        return firstDisplayedEnabled(PROGRAM_PICKER_CARD) != null;
    }

    private boolean dismissProgramPickerIfPresent() {
        WebElement firstCard = firstDisplayedEnabled(PROGRAM_PICKER_CARD);
        if (firstCard == null) {
            return false;
        }
        Rectangle card = firstCard.getRect();
        Map<String, Object> drag = new HashMap<>();
        int centerX = card.getX() + card.getWidth() / 2;
        int sheetTop = Math.max(1, card.getY() - 12);
        int screenBottom = driver.manage().window().getSize().getHeight() - 20;
        drag.put("duration", 0.5);
        drag.put("fromX", centerX);
        drag.put("fromY", sheetTop);
        drag.put("toX", centerX);
        drag.put("toY", screenBottom);
        ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", drag);
        try {
            createWait(8).until(webDriver -> !isProgramPickerVisible());
        } catch (Exception ignored) {
            Map<String, Object> outsideTap = new HashMap<>();
            outsideTap.put("x", Math.max(1, card.getX() + 10));
            outsideTap.put("y", Math.max(20, card.getY() - 50));
            ((JavascriptExecutor) driver).executeScript("mobile: tap", outsideTap);
            createWait(8).until(webDriver -> !isProgramPickerVisible());
        }
        return true;
    }

    private boolean acceptSystemAlertIfPresent() {
        try {
            Alert alert = driver.switchTo().alert();
            alert.accept();
            return true;
        } catch (NoAlertPresentException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void acceptPendingSystemAlerts() {
        for (int attempt = 0; attempt < 4; attempt++) {
            if (!acceptSystemAlertIfPresent()) {
                return;
            }
            try {
                Thread.sleep(750L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean requiresCoordinateTap(WebElement element) {
        if (element == null) {
            return false;
        }
        try {
            return !element.findElements(By.xpath(
                    "ancestor::XCUIElementTypeOther[@name='StartQuestionnaireOneTapAnswerView' "
                            + "or @name='StartQuestionnaireCheckedView'] "
                            + "| ancestor::XCUIElementTypeWebView"
            )).isEmpty();
        } catch (StaleElementReferenceException ignored) {
            return false;
        }
    }

    private void tapElementCenterAndWaitForStateChange(WebElement element, long timeoutInSeconds) {
        String sourceBeforeTap = safePageSource();
        Rectangle rectangle = element.getRect();
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", rectangle.getX() + rectangle.getWidth() / 2);
        tap.put("y", rectangle.getY() + rectangle.getHeight() / 2);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
        waitForSourceToChange(sourceBeforeTap, timeoutInSeconds);
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
