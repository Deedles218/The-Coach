package lib.ui.ios;

import io.qameta.allure.Allure;
import lib.TestData;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Scoped Kegel fixture interactions using observed native identifiers. */
public final class iOSKegelCustomizationPageObject extends iOSDailyPlanPageObject {
    public iOSKegelCustomizationPageObject(RemoteWebDriver driver) { super(driver); }

    public void resetKegelFromSettings() {
        // Caller has verified the approved account and opened Kegel detail, never Profile Settings.
        Allure.addAttachment("Kegel reset settings", "application/xml", TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        waitForElementEnabled("id:RESTART PROGRAM", "Kegel Restart Program is unavailable", 10);
        waitForElementAndClick("id:RESTART PROGRAM", "Cannot restart Kegel fixture", 10);
        Allure.addAttachment("Kegel reset confirmation", "application/xml", TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        waitForElementPresent("xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='Are you sure that you want to delete all your progress within the program?']",
                "Expected program-only reset confirmation is absent; no confirmation given", 10);
        waitForElementAndClick("id:Yes", "Cannot confirm program-only Kegel reset", 10);
        waitForElementNotPresent("id:Yes", "Kegel reset confirmation did not close", 15);
    }

    public static String catchUpCardLocator(String title) {
        Assert.assertTrue("Unexpected practice title", title != null && !title.contains("'") && !title.contains("\""));
        return "xpath://XCUIElementTypeOther[@name='DailyPlanItem' and .//XCUIElementTypeStaticText[@name='" + title + "']"
                + " and ancestor::XCUIElementTypeCell/preceding-sibling::XCUIElementTypeCell"
                + "[.//XCUIElementTypeStaticText[@name='TitleBlock.Title']][1]"
                + "//XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='TO CATCH-UP']]";
    }

    public void assertExactCatchUp(String title) {
        String target = catchUpCardLocator(title);
        for (int i=0; i<8 && !isElementVisible(target); i++) mobileSwipeUp();
        waitForElementPresent(CUSTOMIZATION_CATCH_UP_SECTION, "TO CATCH-UP section is absent", 10);
        waitForElementPresent(target, "The exact prepared practice is absent", 10);
        Assert.assertEquals("Prepared practice is duplicated", 1, getAmountElements(target));
        waitForElementPresent(target + "/ancestor::XCUIElementTypeCell/preceding-sibling::XCUIElementTypeCell"
                        + "[.//XCUIElementTypeStaticText[@name='TitleBlock.Title']][1]"
                        + "//XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='TO CATCH-UP']",
                "The exact practice is not under the TO CATCH-UP header", 10);
    }

    public void postponeExactCatchUp(String title) {
        assertExactCatchUp(title);
        WebElement target = waitForElementPresent(catchUpCardLocator(title), "Prepared card vanished", 10);
        touchAndHoldElement(target, .8);
        assertCustomizationActionsSheetIsDisplayed();
        moveSelectedCardToTomorrow();
        waitForElementPresent(catchUpCardLocator(title) + "//XCUIElementTypeImage[@name='ItemMovedForward']",
                "The exact moved practice has no clock icon", 10);
    }

    public void returnToDayHeader() {
        for (int i=0; i<8 && !isElementVisible(CURRENT_DAY_LABEL); i++) mobileSwipeDown();
        waitForElementPresent(CURRENT_DAY_LABEL, "Day header is not visible", 10);
    }
}
