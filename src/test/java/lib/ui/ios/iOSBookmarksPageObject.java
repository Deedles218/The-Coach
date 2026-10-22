package lib.ui.ios;

import lib.ui.BookmarksPageObject;
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
