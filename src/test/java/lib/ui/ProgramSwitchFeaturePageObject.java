package lib.ui;

import io.appium.java_client.HasSettings;
import lib.Platform;
import lib.ui.android.AndroidProgramSelectorPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.HashMap;
import java.util.Map;

/** COA-9549 controls observed in the preprod iOS and 1.41.2 Android builds. */
public final class ProgramSwitchFeaturePageObject extends MainPageObject {
    private static final String ANDROID_ID = "com.vamapps.thecoach:id/";
    private final DailyPlanPageObject today;
    private final boolean android;

    public ProgramSwitchFeaturePageObject(RemoteWebDriver driver) {
        super(driver);
        android = Platform.getInstance().isAndroid();
        today = android ? new AndroidProgramSelectorPageObject(driver) : new iOSProgramSelectorPageObject(driver);
        if (android && driver instanceof HasSettings) {
            // Android's tooltip is a separate popup window. Include all windows
            // in UiAutomator2's source before looking for its accessible text.
            ((HasSettings) driver).setSetting("enableMultiWindows", true);
        }
    }

    public void openToday() { today.openTodayTab(); }

    public String activeProgram() { return today.getActiveProgramName(); }

    private String firstTooltip() {
        return android
                ? "xpath://*[@text='Tap to explore other programs']"
                : "xpath://XCUIElementTypeStaticText[@name='Tap to explore more programs' and @visible='true']";
    }

    private String secondTooltip() {
        return android
                ? "xpath://*[@text='To switch back, tap here']"
                : "xpath://XCUIElementTypeStaticText[@name='To switch back, tap here' and @visible='true']";
    }

    private String confirmation() {
        return android ? "id:" + ANDROID_ID + "tvDialogtitle"
                : "id:Cancel";
    }

    public void assertFirstTooltipAbsent() {
        Assert.assertFalse("COA-9549: first tooltip appeared before a Daily Plan item was completed",
                isElementVisible(firstTooltip()));
    }

    public void waitForFirstTooltip() {
        waitForElementVisible(firstTooltip(),
                "COA-9549: first tooltip is absent. On Android, expose its text/OK in the accessibility tree.", 15);
    }

    public void dismissFirstTooltip() {
        waitForFirstElementAndClick(android
                        ? new String[]{"id:" + ANDROID_ID + "btnOk", "xpath://android.widget.Button[@text='OK']"}
                        : new String[]{"id:OK"},
                "COA-9549: first tooltip OK is not accessible", 10);
        waitForElementNotPresent(firstTooltip(), "First tooltip remained after OK", 10);
    }

    public void waitForSecondTooltip() {
        waitForElementVisible(secondTooltip(),
                "COA-9549: second tooltip is absent. On Android, expose its text/OK in the accessibility tree.", 15);
    }

    public void dismissSecondTooltip() {
        waitForFirstElementAndClick(android
                        ? new String[]{"id:" + ANDROID_ID + "btnOk", "xpath://android.widget.Button[@text='OK']"}
                        : new String[]{"id:OK"},
                "COA-9549: second tooltip OK is not accessible", 10);
        waitForElementNotPresent(secondTooltip(), "Second tooltip remained after OK", 10);
    }

    public void assertTooltipsAbsent() {
        Assert.assertFalse("First tooltip repeated", isElementVisible(firstTooltip()));
        Assert.assertFalse("Second tooltip repeated", isElementVisible(secondTooltip()));
    }

    public void completeDailyPlanItem(String title) {
        Assert.assertTrue("Daily Plan item title cannot contain a quote in this fixture",
                title == null || !title.contains("'"));
        String item = title != null && android
                ? "xpath://*[@resource-id='" + ANDROID_ID + "cvLessonContainer']"
                    + "//*[@resource-id='" + ANDROID_ID + "tvLessonName' and @text='" + title + "']"
                : title != null
                    ? "xpath://XCUIElementTypeOther[@name='DailyPlanItem']"
                        + "//XCUIElementTypeStaticText[@name='" + title + "']"
                    : android
                        ? "xpath:(//*[@resource-id='" + ANDROID_ID + "cvLessonContainer' and .//*[@resource-id='" + ANDROID_ID
                            + "tvTagText' and starts-with(@text,'Lesson 1')]])[1]//*[@resource-id='" + ANDROID_ID + "tvLessonName']"
                        : "xpath:(//XCUIElementTypeOther[@name='DailyPlanItem' and .//XCUIElementTypeStaticText[@name='Lesson 1']])[1]"
                            + "//XCUIElementTypeStaticText[not(@name='Lesson 1')][1]";
        if (title != null) {
            for (int swipe = 0; swipe < 10 && !isElementVisible(item); swipe++) today.mobileSwipeUp();
        }
        waitForElementAndClick(item, "Configured Daily Plan item is absent: " + title, 15);
        String continueButton = android ? "id:" + ANDROID_ID + "bContinueLesson" : "id:CONTINUE";
        String finishButton = android ? continueButton : "id:FINISH";
        for (int page = 0; page < 20; page++) {
            if (isElementVisible(finishButton)
                    && (!android || "FINISH".equalsIgnoreCase(waitForElementVisible(finishButton,
                    "Finish button is absent", 3).getText()))) {
                waitForElementAndClick(finishButton, "Cannot finish Daily Plan item", 10);
                break;
            }
            waitForElementAndClick(continueButton, "Cannot advance Daily Plan item", 15);
            if (page == 19) Assert.fail("Daily Plan item did not reach FINISH within 20 pages");
        }
        if (android && isElementVisible("id:" + ANDROID_ID + "rvFeedBackDetails"))
            waitForElementAndClick(continueButton, "Cannot finish Daily Plan feedback", 10);
        if (!android && isElementVisible("xpath://XCUIElementTypeStaticText[@name='How would you rate the lesson?' and @visible='true']")) {
            waitForElementAndClick("xpath:(//XCUIElementTypeButton[@name='RatingStarClear' and @visible='true'])[5]",
                    "Cannot rate completed lesson", 10);
            waitForElementAndClick("id:SEND", "Cannot submit lesson completion rating", 10);
            waitForElementNotPresent("xpath://XCUIElementTypeStaticText[@name='How would you rate the lesson?' and @visible='true']",
                    "Lesson rating remained after submission", 10);
        }
        today.openTodayTab();
    }

    public void openSelector() { today.openProgramSelector(); }

    public void selectProgramForFirstTime(String title) {
        Assert.assertFalse("Program title cannot contain a quote in this fixture", title.contains("'"));
        String item = android
                ? "xpath://*[@resource-id='" + ANDROID_ID + "rvActivePrograms']//*[@resource-id='" + ANDROID_ID
                    + "tvTitle' and @text='" + title + "']"
                : "xpath://XCUIElementTypeOther[@name='ProgramSelectionCardView']"
                    + "//XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='" + title + "']";
        for (int n = 0; n < 15; n++) {
            if (isElementVisible(item)) {
                waitForElementAndClick(item, "Cannot select program " + title, 10);
                return;
            }
            if (android) {
                WebElement list = waitForElementVisible("id:" + ANDROID_ID + "rvActivePrograms",
                        "Program selector list is absent", 10);
                Map<String,Object> args = new HashMap<String,Object>();
                args.put("elementId", ((RemoteWebElement) list).getId());
                args.put("direction", "down");
                args.put("percent", .8);
                if (!Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args))) break;
            } else {
                Rectangle list = waitForElementVisible("xpath://XCUIElementTypeCollectionView"
                        + "[.//XCUIElementTypeOther[@name='ProgramSelectionCardView']]",
                        "Program selector collection is absent", 10).getRect();
                Map<String,Object> args = new HashMap<String,Object>();
                args.put("duration", .5);
                args.put("fromX", list.x + list.width / 2);
                args.put("toX", list.x + list.width / 2);
                args.put("fromY", list.y + (int)(list.height * .8));
                args.put("toY", list.y + (int)(list.height * .4));
                ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", args);
            }
        }
        Assert.fail("Fixture program is absent from selector: " + title);
    }

    public void waitForConfirmation() {
        waitForElementVisible(confirmation(), "First switch confirmation is absent", 10);
        waitForElementVisible(android ? "id:" + ANDROID_ID + "btnYes" : "id:Switch program",
                "First switch confirmation has no confirm action", 10);
    }

    public void cancelConfirmation() {
        waitForElementAndClick(android ? "id:" + ANDROID_ID + "btnNo" : "id:Cancel",
                "Cannot cancel first switch", 10);
        waitForElementNotPresent(confirmation(), "Confirmation remained after Cancel", 10);
    }

    public void confirmSwitch() {
        waitForElementAndClick(android ? "id:" + ANDROID_ID + "btnYes" : "id:Switch program",
                "Cannot confirm first switch", 10);
        waitForElementNotPresent(confirmation(), "Confirmation remained after Switch", 10);
    }

    public void waitForProgram(String title) {
        createWait(20).until(driver -> title.equalsIgnoreCase(activeProgram()));
    }

    public void assertConfirmationAbsent() {
        Assert.assertFalse("Confirmation repeated after first confirmed switch", isElementVisible(confirmation()));
    }
}
