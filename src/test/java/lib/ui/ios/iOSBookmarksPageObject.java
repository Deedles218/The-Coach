package lib.ui.ios;

import io.appium.java_client.HasSettings;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.appmanagement.ApplicationState;
import io.qameta.allure.Allure;
import lib.Platform;
import lib.ui.BookmarksPageObject;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSBookmarksPageObject extends BookmarksPageObject {
    static {
        TODAY_TAB = "id:Today";
        TODAY_SELECTED = "xpath://XCUIElementTypeButton[@name='Today' and @value='1']";
        EXPLORE_TAB = "id:Explore";
        EXPLORE_SELECTED = "xpath://XCUIElementTypeButton[@name='Explore' and @value='1']";
        BOOKMARKS_ENTRY = "id:nav bar bookmarks";
        BOOKMARKS_TITLE = "id:My Bookmarks";
        BOOKMARKS_BACK = "id:ic_outline_chevron_left";
        EMPTY_IMAGE = "id:add_bookmark";
        EMPTY_TEXT = "id:Bookmark lessons or practices from any program in your Daily Plan.";
        INACTIVE_BOOKMARK = "id:empty bookmark";
        ACTIVE_BOOKMARK = "xpath:(//*[self::XCUIElementTypeButton or self::XCUIElementTypeImage]"
                + "[@visible='true' and contains(translate(@name,'BOOKMARK','bookmark'),'bookmark')"
                + " and @name!='empty bookmark' and @name!='nav bar bookmarks' and @name!='add_bookmark'])[1]";
        CONTENT_CLOSE = "xpath://XCUIElementTypeButton[@visible='true' and (@name='ic outline close'"
                + " or @name='ProgramCloseButtonIcon' or @name='nav bar back round black')]";
        CONTENT_EXIT_QUIT = "xpath://XCUIElementTypeButton[@name='Quit' and @visible='true']";
        CONTENT_NEXT = "xpath://XCUIElementTypeButton[@name='CONTINUE' and @visible='true']";
        CONTENT_PREVIOUS = "xpath://XCUIElementTypeButton[@name='ic outline chevron left' and @visible='true']";
        FINISH = "id:FINISH";
        CANCEL = "id:CANCEL";
        REMOVE = "id:REMOVE";
        FEEDBACK = "xpath://*[contains(translate(@name,'FEEDBACK','feedback'),'feedback')"
                + " or contains(@name,'How was this')]";
        PAYWALL = "xpath://*[contains(translate(@name,'SUBSCRIBEPAYWALL','subscribepaywall'),'subscribe')"
                + " or contains(translate(@name,'SUBSCRIBEPAYWALL','subscribepaywall'),'paywall')]";
    }

    public iOSBookmarksPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected String tooltipFor(String text) {
        return "xpath://XCUIElementTypeStaticText[@visible='true' and normalize-space(@name)="
                + "normalize-space(" + xpathLiteral(text) + ")]";
    }

    @Override
    protected void selectRecurringDay(String expectedDay, boolean previous) {
        for (int attempt = 0; attempt < 8 && !isElementVisible("id:leftSwitcherArrow"); attempt++) swipe("down");
        // Repeated practices can be separated by several days (COA-9664).
        new iOSProgramSelectorPageObject(driver).selectObservedDay(expectedDay);
    }

    @Override
    @io.qameta.allure.Step("Save opened content in the Coach application")
    public void saveOpenedContent() {
        inCoachWindow(() -> {
            new iOSCoachFlowPageObject(driver).tapHittableControl(new String[]{INACTIVE_BOOKMARK},
                    "Inactive bookmark is not hittable in Coach", 10);
            waitForElementNotVisible(INACTIVE_BOOKMARK, "Bookmark did not switch to active state", 15);
            assertOpenedContentSaved();
        });
    }

    @Override
    public void openCard(String title) {
        inCoachWindow(() -> super.openCard(title));
    }

    @Override
    public void openCardExpectingPaywall(String title) {
        inCoachWindow(() -> super.openCardExpectingPaywall(title));
    }

    @Override
    public void closeOpenedContent() {
        try {
            super.closeOpenedContent();
        } catch (TimeoutException failure) {
            if (!reactivateCoachAfterBackgrounding("closing the reader")) throw failure;
            super.closeOpenedContentIfPresent();
            waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                    "Neither Today nor My Bookmarks returned after closing the reader", 10);
        }
    }

    @Override
    public void closeOpenedContentIfPresent() {
        try {
            super.closeOpenedContentIfPresent();
        } catch (TimeoutException failure) {
            if (!reactivateCoachAfterBackgrounding("closing the reader during cleanup")) throw failure;
            super.closeOpenedContentIfPresent();
        }
    }

    @Override
    public void backFromBookmarks() {
        try {
            super.backFromBookmarks();
            waitForBookmarksOrigin();
        } catch (TimeoutException failure) {
            if (!reactivateCoachAfterBackgrounding("Back from My Bookmarks")) throw failure;
            if (isScreenVisible()) super.backFromBookmarks();
            waitForBookmarksOrigin();
        }
    }

    private boolean reactivateCoachAfterBackgrounding(String action) {
        return reactivateCoachAfterBackgrounding((InteractsWithApps) driver,
                Platform.getInstance().getIOSBundleId(), action);
    }

    private static boolean reactivateCoachAfterBackgrounding(InteractsWithApps apps, String bundle, String action) {
        ApplicationState state = apps.queryAppState(bundle);
        if (state != ApplicationState.RUNNING_IN_BACKGROUND
                && state != ApplicationState.RUNNING_IN_BACKGROUND_SUSPENDED) return false;
        apps.activateApp(bundle);
        Allure.addAttachment("Observed external application interruption",
                "Coach lost foreground during " + action + " and was reactivated.");
        return true;
    }

    private void waitForBookmarksOrigin() {
        waitForFirstElementPresent(new String[]{TODAY_SELECTED, EXPLORE_SELECTED},
                "Back from My Bookmarks did not return to a selected Coach tab", 10);
    }

    @Override
    protected void tapBookmarksEntry(String entry) {
        inCoachWindow(() -> new iOSCoachFlowPageObject(driver).tapHittableControl(
                new String[]{entry}, "Bookmarks entry is not hittable in Coach", 15));
    }

    private void inCoachWindow(Runnable action) {
        HasSettings settings = (HasSettings) driver;
        Object previous = settings.getSettings().get("defaultActiveApplication");
        try {
            settings.setSetting("defaultActiveApplication", Platform.getInstance().getIOSBundleId());
            action.run();
        } finally {
            settings.setSetting("defaultActiveApplication", previous == null ? "auto" : previous);
        }
    }

    @Override
    public void openFromToday() {
        try {
            super.openFromToday();
        } catch (TimeoutException failure) {
            if (!reactivateCoachAfterBackgrounding("opening My Bookmarks")) throw failure;
            super.openFromToday();
        }
    }

    @Override
    protected boolean isContentPaywallVisible() {
        return new iOSOnboardingPageObject(driver).isPaywallVisible();
    }

    @Override
    protected void dismissContentPaywallIfPresent() {
        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        if (onboarding.isPaywallVisible()) {
            onboarding.closePaywallsAndPopups();
            createWait(15).withMessage("Content paywall stayed visible during cleanup")
                    .until(ignored -> !onboarding.isPaywallVisible());
        }
    }
}
