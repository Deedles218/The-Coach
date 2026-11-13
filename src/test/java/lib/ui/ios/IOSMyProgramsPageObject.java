package lib.ui.ios;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** My Programs in the program activation experiment. */
public final class IOSMyProgramsPageObject extends iOSDailyPlanPageObject {
    private static final String SELECTOR_TITLE = "id:My programs";
    private static final String MAIN_JOURNEY = "id:MAIN JOURNEY";
    private static final String SHEET_GRABBER = "id:Sheet Grabber";
    private static final String ACTIVE_MARK = "xpath://XCUIElementTypeStaticText[@name='✓' and @visible='true']";
    private static final String ADD_PROGRAM = "id:Add a program";
    private static final String CATALOG_TITLE = "id:All Programs";
    private static final String CATALOG_BACK = "id:ic outline chevron left";

    public IOSMyProgramsPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Open My Programs from Today")
    public void openProgramSelector() {
        new iOSCoachFlowPageObject(driver).tapHittableControl(
                new String[]{TEST_ID_DAILY_PLAN_PROGRAM_SELECTOR, TEST_ID_ACTIVE_PROGRAM, ACTIVE_PROGRAM_TITLE},
                "Cannot open the program selector from the Today header", 10);
        waitForElementVisible(SELECTOR_TITLE, "My Programs did not open. Check the test profile's A/B group", 15);
        waitForElementVisible(MAIN_JOURNEY, "My Programs has no Main Journey section", 10);
    }

    @Step("Verify My Programs marks {program} as active")
    public void assertActiveProgramMarked(String program) {
        int headerY = waitForElementVisible(SELECTOR_TITLE, "My Programs is not open", 10).getRect().y;
        String title = Quotes.escape(program);
        By visibleTitles = By.xpath("//XCUIElementTypeStaticText[@visible='true' and (@name="
                + title + " or @label=" + title + ")]");
        // The checkmark is a separate accessibility element on the observed iOS builds.
        createWait(10).ignoring(StaleElementReferenceException.class)
                .withMessage("My Programs must show one active checkmark on the row for " + program)
                .until(ignored -> {
                    List<Rectangle> marks = driver.findElements(getLocatorByString(ACTIVE_MARK)).stream()
                            .map(WebElement::getRect).filter(rect -> rect.y > headerY).collect(Collectors.toList());
                    if (marks.size() != 1) return false;
                    Rectangle mark = marks.get(0);
                    return driver.findElements(visibleTitles).stream().map(WebElement::getRect)
                            .anyMatch(rect -> rect.y > headerY
                                    && rect.y < mark.y + mark.height && mark.y < rect.y + rect.height);
                });
    }

    @Override
    @Step("Dismiss My Programs by tapping outside the sheet")
    public void closeProgramSelector() {
        Rectangle handle = grabberBounds();
        Assert.assertTrue("My Programs must leave a tappable area above the sheet", handle.y > 40);
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", handle.x + handle.width / 2);
        tap.put("y", handle.y / 2);
        driver.executeScript("mobile: tap", tap);
        waitForElementNotVisible(SELECTOR_TITLE, "My Programs remained open after tapping outside", 10);
    }

    @Step("Dismiss My Programs with a downward swipe")
    public void swipeSelectorDown() {
        Rectangle handle = grabberBounds();
        int x = handle.x + handle.width / 2;
        int fromY = handle.y + handle.height / 2;
        int toY = driver.manage().window().getSize().height - 40;
        Assert.assertTrue("My Programs must leave room for a downward swipe", toY > fromY);
        Map<String, Object> drag = new HashMap<>();
        drag.put("fromX", x);
        drag.put("fromY", fromY);
        drag.put("toX", x);
        drag.put("toY", toY);
        drag.put("duration", .5);
        driver.executeScript("mobile: dragFromToForDuration", drag);
        waitForElementNotVisible(SELECTOR_TITLE, "My Programs remained open after swiping down", 10);
    }

    @Step("Open the program catalog with Add a program")
    public void openCatalogFromSelector() {
        waitForElementAndClick(ADD_PROGRAM, "Add a program is not visible in My Programs", 10);
        waitForElementVisible(CATALOG_TITLE, "Add a program did not open All Programs", 15);
    }

    @Step("Return from the program catalog to Today")
    public void returnToToday() {
        if (isElementVisible(CATALOG_TITLE)) {
            waitForElementAndClick(CATALOG_BACK, "Cannot return from All Programs", 10);
            waitForElementNotVisible(CATALOG_TITLE, "All Programs remained open after tapping Back", 10);
        }
        if (isElementVisible(SELECTOR_TITLE)) closeProgramSelector();
    }

    public String getFirstDailyLessonTitle() {
        return getElementAccessibleName(waitForElementVisible(FIRST_LESSON_TITLE, "Today has no visible first lesson", 10));
    }

    private Rectangle grabberBounds() {
        waitForElementVisible(SELECTOR_TITLE, "My Programs is not open", 10);
        return waitForElementVisible(SHEET_GRABBER, "My Programs sheet grabber is missing", 10).getRect();
    }
}
