package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

abstract public class OnboardingPageObject extends MainPageObject {
    protected static final int MAX_QUESTIONNAIRE_ACTIONS = 60;
    protected static final int MAX_TRANSIENT_SCREENS = 16;

    public OnboardingPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Complete new-user onboarding for goal: {goal.displayName}")
    public int completeNewUserJourney(OnboardingGoal goal) {
        completeNewUserJourneyToPaywall(goal);
        int paywallsClosed = closePaywallsAndPopups();
        waitForToday();
        return paywallsClosed;
    }

    @Step("Complete new-user onboarding up to the paywall for goal: {goal.displayName}")
    public void completeNewUserJourneyToPaywall(OnboardingGoal goal) {
        Assert.assertNotNull("Onboarding goal must be configured", goal);
        assertFreshStartIsDisplayed();
        openStartFlow();
        selectGoal(goal);
        completeQuestionnaire();
    }

    public void completeNewUserJourneyToPaywall(String goal) {
        completeNewUserJourneyToPaywall(OnboardingGoal.fromConfiguredValue(goal));
    }

    public abstract int closePaywallsBeforeSlides(String firstSlideHeader);

    public abstract int closePaywallsWithoutConsumingSlides(String[] slideHeaders);

    @Step("Verify the app is on a fresh new-user Start screen")
    public abstract void assertFreshStartIsDisplayed();

    @Step("Open the first Start now action")
    public abstract void openStartFlow();

    @Step("Select onboarding goal: {goal.displayName}")
    public abstract void selectGoal(OnboardingGoal goal);

    @Step("Complete the runtime-driven questionnaire")
    public abstract void completeQuestionnaire();

    /**
     * Closes every subscription screen and optional prompt encountered before
     * Today. Returns the number of subscription paywalls actually closed.
     */
    @Step("Close onboarding paywalls and optional popups")
    public abstract int closePaywallsAndPopups();

    @Step("Wait for Today after onboarding")
    public abstract void waitForToday();

    protected String elementText(WebElement element) {
        if (element == null) {
            return "";
        }
        String[] attributes = new String[]{"text", "name", "label", "value", "content-desc"};
        for (String attribute : attributes) {
            try {
                String value = element.getAttribute(attribute);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            } catch (Exception ignored) {
                // Attribute availability differs between UiAutomator2/XCUITest.
            }
        }
        try {
            return element.getText() == null ? "" : element.getText().trim();
        } catch (StaleElementReferenceException ignored) {
            return "";
        }
    }

    protected void clickAndWaitForStateChange(WebElement element, long timeoutInSeconds) {
        final String sourceBeforeClick = safePageSource();
        element.click();
        try {
            createWait(timeoutInSeconds).until(webDriver -> {
                try {
                    if (element == null || !element.isDisplayed()) {
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    return true;
                }
                return !sourceBeforeClick.equals(safePageSource());
            });
        } catch (TimeoutException ignored) {
            // Some WebViews update only enabled/selected state. The bounded
            // outer loop will either click Continue or fail with diagnostics.
        }
    }

    protected String safePageSource() {
        try {
            String source = driver.getPageSource();
            return source == null ? "" : source;
        } catch (Exception ignored) {
            return "";
        }
    }
}
