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
        CONTENT_CLOSE = "id:ic outline close";
        CONTENT_EXIT_QUIT = "id:Quit";
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
}
