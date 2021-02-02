package lib.ui.ios;
import lib.ui.SearchPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSSearchPageObject extends SearchPageObject {
    static {
        TAB_FEED="id:Feed";
        IMAGE = "xpath://XCUIElementTypeStaticText[@label = 'This is image test']";
        INPUT_FIELD="xpath://XCUIElementTypeTextView[@value = 'Type Something...']";
        INPUT_FIELD_ACTIVE="xpath://XCUIElementTypeOther[@name='inputAccessoryViewContainer']/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther[2]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeTextView";   // "XCUIElementTypeOther[@name='inputAccessoryViewContainer']/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeTextView";
        SEND_BUTTON="id:chat send button active";
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
