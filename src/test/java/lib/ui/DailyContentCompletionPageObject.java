package lib.ui;

import lib.MobileCaseFixture;
import lib.Platform;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

/** Reuses the existing lesson reader and per-card rendered completion detection. */
public final class DailyContentCompletionPageObject extends MainPageObject {
    private final DailyPlanPageObject daily;
    private final BookmarksPageObject cards;

    public DailyContentCompletionPageObject(RemoteWebDriver driver) {
        super(driver);
        daily = DailyPlanPageObjectFactory.get(driver);
        cards = BookmarksPageObjectFactory.get(driver);
    }

    public void finishLessonForPersistence(MobileCaseFixture fixture) {
        daily.openTodayTab();
        String program = daily.getActiveProgramName(), day = daily.getCurrentDayLabel();
        Assert.assertEquals("Completion fixture opened another program", fixture.required("program"), program);
        Assert.assertEquals("Completion fixture opened another day", fixture.required("day"), day);
        String title = fixture.required("content.title");
        cards.assertDailyPlanItemNotCompleted(title);
        WebElement row = waitForElementVisible(card(title), "Required content card is absent", 10);
        Assert.assertTrue("Fixture card has the wrong content type", row.findElements(
                getLocatorByString(fixture.relativeSelector("content.kind"))).stream().anyMatch(element ->
                element.isDisplayed() && fixture.required("content.kindText").equals(element.getText().trim())));
        Assert.assertEquals("Fixture data identifies another business scenario", "lesson", fixture.required("content.type"));
        new ProgramSwitchFeaturePageObject(driver).completeDailyPlanItem(title);
        cards.assertDailyPlanItemCompleted(title);
        Assert.assertEquals("FINISH changed the program", program, daily.getActiveProgramName());
        Assert.assertEquals("FINISH changed the selected day", day, daily.getCurrentDayLabel());
    }

    public void assertCompletedLessonAfterRestart(MobileCaseFixture fixture) {
        daily.openTodayTab();
        Assert.assertEquals("Restart did not retain the program", fixture.required("program"), daily.getActiveProgramName());
        Assert.assertEquals("Restart did not retain the selected day", fixture.required("day"), daily.getCurrentDayLabel());
        cards.assertDailyPlanItemCompleted(fixture.required("content.title"));
    }

    private String card(String title) {
        String literal = Quotes.escape(title);
        if (Platform.getInstance().isIOS()) return "xpath://XCUIElementTypeOther[@name='DailyPlanItem' and .//XCUIElementTypeStaticText[@name=" + literal + "]]";
        String prefix = Platform.getInstance().getAndroidAppPackage() + ":id/";
        return "xpath://*[@resource-id='" + prefix + "tvLessonName' and @text=" + literal
                + "]/ancestor::*[@resource-id='" + prefix + "cvLessonContainer'][1]";
    }
}
