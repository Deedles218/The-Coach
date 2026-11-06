package lib.ui.ios;

import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Observed For Her 2.1.0 recipient questionnaire; other onboarding keeps its default flow. */
public final class iOSPartnerRecipientOnboardingPageObject extends iOSOnboardingPageObject {
    private static final String WEB = "//XCUIElementTypeWebView[@visible='true']";

    public iOSPartnerRecipientOnboardingPageObject(RemoteWebDriver driver) { super(driver); }

    @Override
    protected boolean completeVariantQuestionnaireScreen() {
        if (driver.findElements(By.xpath(WEB)).isEmpty()) return false;
        String before = safePageSource();
        if (visibleText("How close do you currently feel to your partner?")) {
            // This build exposes none of the seven image choices to accessibility.
            // The middle choice was verified on iPhone 11; derive its tap from
            // the WebView bounds. App-side accessible choice IDs are preferable.
            Rectangle view = waitForElementVisible("xpath:(" + WEB + ")[last()]",
                    "Recipient image questionnaire has no WebView", 10).getRect();
            Map<String, Object> tap = new HashMap<>();
            tap.put("x", view.x + view.width / 2);
            tap.put("y", view.y + (int) (view.height * .61));
            driver.executeScript("mobile: tap", tap);
        } else if (visibleText("Rate from 1 to 5:")) {
            tapText("3");
        } else if (visibleText("Do you relate to the following statement?")) {
            tapText("Yes");
        } else if (visibleText("Would you like to explore any of these?")) {
            selectAndContinue("Experimenting with my A & G Zones");
        } else if (visibleText("Anything else spark your interest?")) {
            selectAndContinue("New Positions");
        } else if (visibleText("Are you also interested in the following topics?")) {
            selectAndContinue("Unlocking Your Magnetism");
        } else {
            // Existing semantic Continue handles informational slides.
            if (isElementVisible("xpath:" + WEB + "//XCUIElementTypeButton[@name='CONTINUE' and @visible='true']")) {
                return false;
            }
            List<WebElement> headings = driver.findElements(By.xpath(WEB
                    + "//XCUIElementTypeStaticText[@visible='true' and contains(@traits,'Header')]"));
            if (headings.isEmpty()) return false;
            Rectangle heading = headings.get(headings.size() - 1).getRect();
            List<WebElement> answers = driver.findElements(By.xpath(WEB
                    + "//XCUIElementTypeStaticText[@visible='true' and @enabled='true' "
                    + "and not(contains(@traits,'Header')) and string-length(normalize-space(@name))>0 "
                    + "and number(@y)>" + (heading.y + heading.height + 20) + "]"));
            if (answers.isEmpty()) return false;
            tapElementCenterAndWaitForStateChange(answers.get(0), 8);
        }
        createWait(12).withMessage("Recipient WebView answer did not advance the questionnaire")
                .until(ignored -> !before.equals(safePageSource()));
        return true;
    }

    private boolean visibleText(String value) {
        return isElementVisible("xpath:" + WEB + "//XCUIElementTypeStaticText[@visible='true' and @name="
                + Quotes.escape(value) + "]");
    }

    private void tapText(String value) {
        WebElement answer = waitForElementVisible("xpath:" + WEB
                        + "//XCUIElementTypeStaticText[@visible='true' and @name=" + Quotes.escape(value) + "]",
                "Observed recipient questionnaire answer is absent", 10);
        tapElementCenterAndWaitForStateChange(answer, 8);
    }

    private void selectAndContinue(String value) {
        tapText(value);
        waitForElementAndClick("xpath:" + WEB + "//XCUIElementTypeButton[@name='CONTINUE' and @visible='true']",
                "Recipient multi-choice Continue is absent", 10);
    }
}
