package lib.ui.ios;
import lib.ui.SearchPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSSearchPageObject extends SearchPageObject {
    static {
        SEARCH_INIT_ELEMENT = "xpath://XCUIElementTypeSearchField[@name='Search Wikipedia']";
        SEARCH_INPUT = "xpath://XCUIElementTypeSearchField[@name='Search Wikipedia']";
        SEARCH_RESULT_BY_SUBSTRING_TPL = "xpath://XCUIElementTypeLink[contains(@name,'{SUBSTRING}')]";
        SEARCH_RESULT_BY_TWO_SUBSTRINGS_TPL = "xpath://XCUIElementTypeLink[contains(@name,'{firstSUBSTRING}')]//..//..//*[contains(@name,'{secondSUBSTRING}')]//..//..";
        SEARCH_CANCEL_BUTTON = "id:Close";
        SEARCH_RESULT_ELEMENT = "xpath://XCUIElementTypeLink";
        SEARCH_EMPTY_RESULT_ELEMENT = "xpath://XCUIElementTypeStaticText[@name='No results found']";
        ARTICLE_TITLE ="//XCUIElementTypeLink[contains(@name,'Jesus')]";
    }
    public iOSSearchPageObject(RemoteWebDriver driver)
    {
        super(driver);
    }
}
