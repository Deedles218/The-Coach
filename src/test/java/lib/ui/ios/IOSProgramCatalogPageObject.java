package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

/** The activation catalog and the informational Program Page in the ON group. */
public final class IOSProgramCatalogPageObject extends MainPageObject {
    private static final String CATALOG = "id:All Programs";
    private static final String ADD_AND_START = "id:Add and start today";

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
        waitForElementEnabled(ADD_AND_START, "Explore did not open the unadded Program Page", 20);
        waitForElementVisible("id:" + title, "Explore opened the wrong program", 10);
    }

    @Step("Open All Programs through the selector")
    public void openFromSelector() {
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        today.openProgramSelector();
        today.scrollToAddProgram();
        today.openCatalogFromSelector();
    }

    @Step("Open informational Program Page: {title}")
    public void openProgram(String title, String expectedAction) {
        waitForElementVisible(CATALOG, "All Programs is not open", 10);
        String card = "id:" + title;
        for (int n = 0; n < 12 && !isElementVisible(card); n++) swipeUpQuick();
        waitForElementAndClick(card, "Program is unavailable in All Programs", 10);
        waitForElementEnabled(expectedAction, "Program Page has no expected CTA", 20);
        waitForElementVisible("id:" + title, "Program Page has the wrong title", 10);
    }

    public void openUnaddedProgram(String title) { openProgram(title, ADD_AND_START); }

    @Step("Activate the displayed program and open its Daily Plan")
    public void addAndStart(String title) { openDailyPlan(title, ADD_AND_START); }

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
