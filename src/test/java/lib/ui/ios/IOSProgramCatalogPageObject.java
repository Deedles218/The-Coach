package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;
import org.openqa.selenium.WebElement;
import lib.ProgramSelectionState;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;

/** The activation catalog and the informational Program Page in the ON group. */
public final class IOSProgramCatalogPageObject extends MainPageObject {
    private static final String CATALOG = "id:All Programs";
    private static final String ADD_TO_PLAN = "xpath://XCUIElementTypeButton[translate(@name,"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='add to daily plan']";

    public IOSProgramCatalogPageObject(RemoteWebDriver driver) { super(driver); }

    @Step("Open an unadded Program Page from the Explore carousel")
    public void openUnaddedFromExplore(String title) {
        new iOSExplorePageObject(driver).openExploreTab();
        String collection = iOSExplorePageObject.programCollectionLocator();
        String card = collection + "//XCUIElementTypeOther[(@name='CoachProgramDetailedManV7Card'"
                + " or @name='CoachProgramDetailedManCard') and XCUIElementTypeStaticText[@name="
                + Quotes.escape(title) + "]]";
        iOSProgramListReader reader = new iOSProgramListReader(driver);
        reader.rewind(iOSExplorePageObject.programTitlesLocator(), collection);
        reader.revealFullCard(card, collection);
        waitForElementAndClick(card + "/XCUIElementTypeStaticText[@name=" + Quotes.escape(title) + "]",
                "Prepared program is unavailable in Explore", 10);
        waitForElementEnabled(ADD_TO_PLAN, "Explore Program Page has no Add to Daily Plan action", 20);
        waitForElementVisible("id:" + title, "Explore opened the wrong program", 10);
    }

    @Step("Open All Programs through the selector")
    public void openFromSelector() {
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        today.openProgramSelector();
        today.openCatalogFromSelector();
    }

    @Step("Open informational Program Page: {title}")
    public void openProgram(String title, String expectedAction) {
        waitForElementVisible(CATALOG, "All Programs is not open", 10);
        new IOSProgramSelectionPageObject(driver).openCardDetails(title);
        waitForElementEnabled(expectedAction, "Program Page has no expected CTA", 20);
        waitForElementVisible("id:" + title, "Program Page has the wrong title", 10);
    }

    public void openUnaddedProgram(String title) { openProgram(title, ADD_TO_PLAN); }

    @Step("Activate the displayed program and open its Daily Plan")
    public void addAndStart(String title) { openDailyPlan(title, ADD_TO_PLAN); }

    public void assertMembershipAndOrder(List<Map<String, Object>> catalog, ProgramSelectionState state) {
        waitForElementVisible(CATALOG, "All Programs is not open", 10);
        IOSProgramSelectionPageObject cards = new IOSProgramSelectionPageObject(driver);
        List<String> actual = cards.cardTitles();
        Set<String> expected = new LinkedHashSet<>();
        for (Map<String, Object> row : catalog) expected.add((String) row.get("title"));
        Assert.assertEquals("All Programs has duplicate cards", actual.size(), new LinkedHashSet<>(actual).size());
        Assert.assertEquals("All Programs differs from the catalog", expected, new LinkedHashSet<>(actual));
        Set<String> added = new LinkedHashSet<>();
        for (Map<String, Object> row : state.programs()) added.add((String) row.get("title"));
        boolean reachedAdded = false;
        for (String title : actual) {
            String card = IOSProgramSelectionPageObject.card(title);
            if (added.contains(title)) {
                reachedAdded = true;
                // v3 retains a distinct Main Journey pill on the main card.
                String badge = state.main() != null && title.equals(state.main().get("title")) ? "MAIN JOURNEY" : "ADDED";
                Assert.assertEquals("Added program has no membership badge: " + title, 1,
                        driver.findElements(getLocatorByString("xpath:" + card
                                + "//XCUIElementTypeStaticText[@name=" + Quotes.escape(badge) + "]")).size());
                Assert.assertFalse("An already added card must not be selected again", cards.selected(title));
            } else Assert.assertFalse("Unadded programs must precede added programs", reachedAdded);
        }
        assertAddCount(0);
    }

    public void selectForAddition(List<String> titles, ProgramSelectionState before) {
        IOSProgramSelectionPageObject cards = new IOSProgramSelectionPageObject(driver);
        Set<String> added = new LinkedHashSet<>();
        for (Map<String, Object> row : before.programs()) added.add((String) row.get("title"));
        Assert.assertEquals("Addition target contains duplicates", titles.size(), new LinkedHashSet<>(titles).size());
        Assert.assertTrue("Addition target is unavailable", cards.cardTitles().containsAll(titles));
        for (String title : cards.cardTitles()) {
            if (added.contains(title)) {
                Assert.assertFalse("Cannot add an already saved program", titles.contains(title));
                continue;
            }
            if (cards.selected(title) != titles.contains(title)) cards.toggle(title);
        }
        assertAddCount(titles.size());
    }

    private void assertAddCount(int count) {
        String action = "id:" + (count == 0 ? "ADD PROGRAM" : "ADD PROGRAM (" + count + ")");
        WebElement button = waitForElementVisible(action, "Add Program count differs from selection", 10);
        Assert.assertEquals("Add Program enabled state differs from selection", count > 0, isElementEnabled(button));
    }

    public void addSelectedAndReturnToSelector(int count) {
        Assert.assertTrue("Select at least one program before adding", count > 0);
        assertAddCount(count);
        waitForElementAndClick("id:ADD PROGRAM (" + count + ")", "Cannot add selected programs", 10);
        waitForElementVisible("id:My programs", "Adding programs did not return to the expanded selector", 20);
    }

    public void openDailyPlan(String title, String cta) {
        waitForElementAndClick(cta, "Program Page CTA is unavailable", 10);
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        today.createWait(30).withMessage("Program Page did not open the expected Daily Plan")
                .until(ignored -> today.isElementVisible("id:Today") && title.equals(today.getActiveProgramName()));
    }

    public void assertText(String text) {
        Assert.assertTrue("Expected Program Page text is empty", text != null && !text.trim().isEmpty());
        for (int n = 0; n < 12 && !isElementVisible("id:" + text); n++) swipeUpQuick();
        waitForElementVisible("id:" + text, "Program Page text is absent", 10);
    }
}
