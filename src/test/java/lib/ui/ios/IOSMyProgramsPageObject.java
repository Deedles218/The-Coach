package lib.ui.ios;

import io.qameta.allure.Step;
import lib.Platform;
import lib.ProgramSelectionState;
import lib.ui.ProgramSwitchFeaturePageObject;
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
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
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
    private static final String HEADER_CHEVRON = "xpath://XCUIElementTypeButton[@name='nav bar bookmarks' and @visible='true']"
            + "/parent::XCUIElementTypeOther/XCUIElementTypeOther[1]/XCUIElementTypeOther[1]"
            + "/XCUIElementTypeImage[@visible='true']";
    private static final String PROGRAM_LIST = "xpath://XCUIElementTypeScrollView[.//XCUIElementTypeStaticText[@name='MAIN JOURNEY']]";

    public IOSMyProgramsPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Open My Programs from Today")
    public void openProgramSelector() {
        assertCoachForeground();
        if (isElementVisible(TEST_ID_DAILY_PLAN_PROGRAM_SELECTOR)) {
            new iOSCoachFlowPageObject(driver).tapHittableControl(new String[]{TEST_ID_DAILY_PLAN_PROGRAM_SELECTOR},
                    "Cannot tap the identified program selector", 10);
        } else if (isElementVisible(HEADER_CHEVRON)) {
            Rectangle target = waitForElementVisible(HEADER_CHEVRON, "Program selector chevron is absent", 10).getRect();
            Map<String, Object> tap = new HashMap<>();
            tap.put("x", target.x + target.width / 2);
            tap.put("y", target.y + target.height / 2);
            driver.executeScript("mobile: tap", tap);
        } else {
            new iOSCoachFlowPageObject(driver).tapHittableControl(new String[]{TEST_ID_ACTIVE_PROGRAM, ACTIVE_PROGRAM_TITLE},
                    "Cannot open the program selector from the Today header", 10);
        }
        createWait(15).withMessage("My Programs did not open. Check the test profile's A/B group").until(ignored -> {
            assertCoachForeground();
            return isElementVisible(SELECTOR_TITLE);
        });
        waitForElementVisible(MAIN_JOURNEY, "My Programs has no Main Journey section", 10);
    }

    private void assertCoachForeground() {
        Map<?, ?> active = (Map<?, ?>) driver.executeScript("mobile: activeAppInfo", java.util.Collections.emptyMap());
        Assert.assertEquals("Selector check interrupted: Coach is not the foreground application",
                Platform.getInstance().getIOSBundleId(), active.get("bundleId"));
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

    @Step("Compare My Programs roles, active mark and displayed stages with the server")
    public void assertMatchesState(ProgramSelectionState state) {
        waitForElementVisible(SELECTOR_TITLE, "My Programs is not open", 10);
        String titles = PROGRAM_LIST + "//XCUIElementTypeOther[XCUIElementTypeStaticText["
                + "contains(@name,'STAGE ') or @name='Completed']]/XCUIElementTypeStaticText[1]";
        List<String> names = getElementAccessibleNames(titles);
        Set<String> expected = state.programs().stream().map(p -> (String) p.get("title"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Assert.assertEquals("My Programs has duplicate rows", names.size(), new LinkedHashSet<>(names).size());
        Assert.assertEquals("My Programs contains missing or unadded programs", expected, new LinkedHashSet<>(names));
        int mainY = waitForElementPresent(MAIN_JOURNEY, "Main Journey heading is absent", 10).getRect().y;
        List<WebElement> secondary = driver.findElements(getLocatorByString("id:SECONDARY"));
        if (!state.secondary().isEmpty()) Assert.assertEquals("Secondary heading is absent or duplicated", 1, secondary.size());
        int secondaryY = secondary.isEmpty() ? Integer.MAX_VALUE : secondary.get(0).getRect().y;
        for (Map<String, Object> row : state.programs()) {
            String title = (String) row.get("title");
            String titleLocator = PROGRAM_LIST + "//XCUIElementTypeStaticText[@name=" + Quotes.escape(title) + "]";
            WebElement name = waitForElementPresent(titleLocator, "Saved program row is absent", 10);
            boolean main = row.get("program_id").equals(state.mainId());
            Assert.assertTrue("Program is in the wrong section: " + title, main
                    ? name.getRect().y > mainY && name.getRect().y < secondaryY
                    : name.getRect().y > secondaryY);
            String detail = "completed".equals(row.get("status")) ? "Completed"
                    : (row.get("current_module_name") + " · Stage " + integer(row.get("current_stage"))
                    + " of " + integer(row.get("total_stages"))).toUpperCase(Locale.ROOT);
            waitForElementPresent(titleLocator + "/parent::XCUIElementTypeOther/XCUIElementTypeStaticText[@name="
                    + Quotes.escape(detail) + "]", "Program module/stage differs from server: " + title, 10);
        }
        assertActiveProgramMarked((String) state.program(state.active()).get("title"));
    }

    @Step("Switch to a saved program: {title}")
    public void selectSavedProgram(String title, boolean confirmationExpected) {
        chooseSavedProgram(title, confirmationExpected);
        ProgramSwitchFeaturePageObject tutorial = new ProgramSwitchFeaturePageObject(driver);
        createWait(20).withMessage("Switch opened the wrong Daily Plan").until(ignored -> {
            tutorial.dismissProgramTooltipsIfPresent();
            if (!isElementVisible(ACTIVE_PROGRAM_TITLE) && !isElementVisible(TEST_ID_ACTIVE_PROGRAM)) return false;
            return title.equals(getActiveProgramName());
        });
    }

    public void chooseSavedProgram(String title, boolean confirmationExpected) {
        String row = PROGRAM_LIST + "//XCUIElementTypeStaticText[@name=" + Quotes.escape(title) + "]";
        waitForElementAndClick(row, "Saved program is not visible in My Programs", 10);
        ProgramSwitchFeaturePageObject tutorial = new ProgramSwitchFeaturePageObject(driver);
        if (confirmationExpected) {
            tutorial.waitForConfirmation();
            tutorial.confirmSwitch();
        }
        waitForElementNotVisible(SELECTOR_TITLE, "Switch did not dismiss My Programs", 20);
    }

    private String integer(Object value) {
        Assert.assertTrue("Stage metadata is not numeric", value instanceof Number);
        return String.valueOf(((Number) value).intValue());
    }

    public void assertTooltipAtSelector(String text, String headerSelector) {
        Rectangle tooltip = waitForElementVisible("id:" + text, "Selector tooltip is absent", 10).getRect();
        // The overlay makes the real header non-visible to XCUITest. Its
        // retained bounds still identify the control the tooltip points at.
        List<WebElement> headers = driver.findElements(getLocatorByString(headerSelector));
        Assert.assertEquals("Reviewed selector anchor is absent or ambiguous", 1, headers.size());
        Rectangle header = headers.get(0).getRect();
        Assert.assertTrue("Selector anchor has no layout bounds", header.width > 0 && header.height > 0);
        Assert.assertTrue("Tooltip is not next to the program selector header",
                tooltip.y >= header.y && tooltip.y < header.y + header.height + 2 * tooltip.height
                        && tooltip.x < header.x + header.width && header.x < tooltip.x + tooltip.width);
    }

    @Override
    @Step("Dismiss My Programs by tapping outside the sheet")
    public void closeProgramSelector() {
        Rectangle handle = grabberBounds();
        Assert.assertTrue("My Programs must leave a tappable area above the sheet", handle.y > 40);
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", handle.x + handle.width / 2);
        // A long list expands the sheet near the status bar. The midpoint
        // above the grabber can hit that bar; tap just outside its hit region.
        tap.put("y", handle.y - 4);
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

    @Step("Scroll My Programs to Add a program and back to the original rows")
    public void scrollListDownAndBack(String activeProgram) {
        Rectangle sheet = grabberBounds();
        Rectangle list = listBounds();
        int top = list.y;
        int bottom = list.y + list.height;
        Assert.assertFalse("Scrolling fixture is too short: Add a program is already visible on entry",
                addProgramFullyVisible(top, bottom));
        Map<String, Rectangle> initialRows = visibleRows(top, bottom);
        Assert.assertFalse("My Programs has no visible rows to scroll", initialRows.isEmpty());
        String firstRow = initialRows.entrySet().stream()
                .min(java.util.Comparator.comparingInt(row -> row.getValue().y)).get().getKey();
        scrollUntilAddProgram(sheet, top, bottom);
        // Check the active state after scrolling, before selecting any program.
        closeProgramSelector();
        Assert.assertEquals("Scrolling changed the active program", activeProgram, getActiveProgramName());
        openProgramSelector();
        scrollUntilAddProgram(sheet, top, bottom);
        for (int attempt = 0; attempt < 12; attempt++) {
            scrollRows(sheet, top, bottom, false);
            Rectangle row = visibleRows(top, bottom).get(firstRow);
            if (row != null && Math.abs(row.y - initialRows.get(firstRow).y) <= 4) {
                assertActiveProgramMarked(activeProgram);
                return;
            }
        }
        Assert.fail("My Programs did not scroll back to its original first row");
    }

    @Step("Scroll the long My Programs list until Add a program is fully visible")
    public void scrollToAddProgram() {
        Rectangle sheet = grabberBounds();
        Rectangle list = listBounds();
        scrollUntilAddProgram(sheet, list.y, list.y + list.height);
    }

    private void scrollUntilAddProgram(Rectangle sheet, int top, int bottom) {
        for (int attempt = 0; attempt < 12; attempt++) {
            if (addProgramFullyVisible(top, bottom)) return;
            scrollRows(sheet, top, bottom, true);
        }
        Assert.fail("Add a program did not become visible after scrolling My Programs");
    }

    private void scrollRows(Rectangle sheet, int top, int bottom, boolean down) {
        Map<String, Rectangle> before = visibleRows(top, bottom);
        Assert.assertFalse("My Programs has no visible rows to scroll", before.isEmpty());
        int lower = bottom - 16;
        int upper = Math.max(top + 16, bottom - 180);
        Assert.assertTrue("My Programs list has no room for a scroll gesture", upper < lower);
        Map<String, Object> drag = new HashMap<>();
        Rectangle list = listBounds();
        int x = list.x + list.width / 2;
        drag.put("fromX", x);
        drag.put("toX", x);
        drag.put("fromY", down ? lower : upper);
        drag.put("toY", down ? upper : lower);
        drag.put("duration", .5);
        driver.executeScript("mobile: dragFromToForDuration", drag);
        createWait(10).ignoring(StaleElementReferenceException.class)
                .withMessage("My Programs rows did not scroll " + (down ? "upward" : "downward") + " inside the sheet")
                .until(ignored -> visibleRows(top, bottom).entrySet().stream().anyMatch(row -> {
                    Rectangle previous = before.get(row.getKey());
                    return previous != null && (down ? row.getValue().y < previous.y - 3
                            : row.getValue().y > previous.y + 3);
                }));
        Assert.assertEquals("The sheet moved instead of its program list", sheet, grabberBounds());
    }

    private boolean addProgramFullyVisible(int top, int bottom) {
        return driver.findElements(getLocatorByString(ADD_PROGRAM)).stream().anyMatch(element -> {
            Rectangle bounds = element.getRect();
            return element.isDisplayed() && bounds.height > 0 && bounds.y > top
                    && bounds.y + bounds.height < bottom;
        });
    }

    private Map<String, Rectangle> visibleRows(int top, int bottom) {
        Map<String, Rectangle> rows = new HashMap<>();
        for (WebElement element : driver.findElements(getLocatorByString(
                PROGRAM_LIST + "//XCUIElementTypeStaticText[@visible='true']"))) {
            Rectangle bounds = element.getRect();
            String name = element.getAttribute("name");
            if (name != null && !name.trim().isEmpty() && bounds.y > top && bounds.y + bounds.height < bottom) {
                rows.put(name, bounds);
            }
        }
        return rows;
    }

    private Rectangle listBounds() {
        return waitForElementVisible(PROGRAM_LIST, "My Programs has no scrollable program list", 10).getRect();
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

    public String getFirstDailyContentTitle() {
        // Maintenance days may contain a practice without any lesson. Read the
        // same DailyPlanItem title contract for either kind of content.
        return getElementAccessibleName(waitForElementVisible(
                "xpath:(//XCUIElementTypeOther[@name='DailyPlanItem' and @visible='true']"
                        + "//XCUIElementTypeStaticText[@visible='true' and normalize-space(@name)!=''])[1]",
                "Today has no visible content card", 10));
    }

    @Step("Compare Daily Plan with the UID-verified response from this UI action")
    public void assertCurrentDailyContent(Map<String, Object> content, String expectedId) {
        Assert.assertEquals("Today response belongs to another program", expectedId, content.get("programId"));
        Assert.assertEquals("Today header differs from its response", content.get("headline"), getActiveProgramName());
        Map<String, Object> metadata = (Map<String, Object>) content.get("sectionMetadata");
        if (metadata.containsKey("module_name")) {
            waitForElementVisible("id:" + metadata.get("module_name"), "Today module differs from its response", 15);
            Assert.assertEquals("Today stage differs from its response", "Stage " + integer(metadata.get("module_current_day"))
                    + " of " + integer(metadata.get("module_total_days")), getCurrentDayLabel());
        }
        List<Map<String, Object>> questions = (List<Map<String, Object>>) content.get("questions");
        Assert.assertFalse("Current Today response has no content", questions.isEmpty());
        int swipes = 0;
        try {
            for (Map<String, Object> question : questions.subList(0, Math.min(3, questions.size()))) {
                String card = "xpath://XCUIElementTypeOther[@name='DailyPlanItem']//XCUIElementTypeStaticText[@label="
                        + Quotes.escape((String) question.get("headline")) + "]";
                for (int n = 0; n < 6 && !isElementVisible(card); n++) { mobileSwipeUp(); swipes++; }
                waitForElementVisible(card, "Today card differs from the selected program response", 10);
            }
        } finally {
            for (int n = 0; n < swipes; n++) mobileSwipeDown();
        }
    }

    private Rectangle grabberBounds() {
        waitForElementVisible(SELECTOR_TITLE, "My Programs is not open", 10);
        return waitForElementVisible(SHEET_GRABBER, "My Programs sheet grabber is missing", 10).getRect();
    }
}
