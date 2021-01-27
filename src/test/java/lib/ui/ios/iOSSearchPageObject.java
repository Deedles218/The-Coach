package lib.ui.ios;
import lib.ui.SearchPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSSearchPageObject extends SearchPageObject {
    static {
        TAB_FEED="id:Feed";
        FEED="id:FEED";
        SEARCH_INIT_ELEMENT = "xpath://XCUIElementTypeButton[@name='ic search']";
        SEARCH_INPUT = "xpath://XCUIElementTypeApplication[@name='Dev Coach']/XCUIElementTypeWindow[1]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeTextField";
        SEARCH_INIT_BUTTON = "id:Search";
        SEARCH_RESULT_BY_SUBSTRING_TPL = "xpath://XCUIElementTypeStaticText[contains(@label,'{SUBSTRING}')]";
        SEARCH_RESULT_BY_TWO_SUBSTRINGS_TPL = "xpath://XCUIElementTypeLink[contains(@name,'{firstSUBSTRING}')]//..//..//*[contains(@name,'{secondSUBSTRING}')]//..//..";
        SEARCH_CANCEL_BUTTON = "xpath://XCUIElementTypeStaticText[@name='CANCEL']";
        SEARCH_RESULT_ELEMENT = "xpath://XCUIElementTypeLink";
        SEARCH_EMPTY_RESULT_ELEMENT = "xpath://XCUIElementTypeStaticText[@name='No results found']";
        ARTICLE_TITLE ="//XCUIElementTypeLink[contains(@name,'Jesus')]";
    }
    public iOSSearchPageObject(RemoteWebDriver driver)
    {
        super(driver);
    }
}
