package lib.ui.ios;
import lib.ui.SearchPageObject;
import org.apache.tools.ant.taskdefs.Sleep;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSSearchPageObject extends SearchPageObject {
    static {
        //элементы таббара
        TAB_FEED="id:Feed";
//        TAB_CHAT="id:Coach";
//        TAB_PROGRAM="id:Program";
//
//        //элементы экрана програмы
//
//        PROGRAM_DP="xpath://XCUIElementTypeStaticText[@name='Your Plan for Today']";
//        TRACKS="id:YOUR TRACKS";
//        NUTRITION="id:Nutrition";
//        SPORT="id:Sport";
//        SLEEP="id:Sleep";
//        HAIR_LOSS="id:Hair loss";
//        MENTAL_HEALTH="id:Mental Health";
//        MALE_HEALTH="id:Male Health";
//        SUPPLEMENTS="id:Supplements";

        //элементы из чата для проверки инпутов
        IMAGE = "xpath://XCUIElementTypeStaticText[@label = 'This is image test']";
        GIF="xpath://XCUIElementTypeApplication[@name='Dev Coach']/XCUIElementTypeWindow[1]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeTable/XCUIElementTypeCell[100]/XCUIElementTypeOther[1]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeImage[1]";

        //поле ввода в чате
        INPUT_FIELD="xpath://XCUIElementTypeTextView[@value = 'Type Something...']";
        INPUT_FIELD_ACTIVE="xpath://XCUIElementTypeOther[@name='inputAccessoryViewContainer']/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther[2]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeTextView";
        SEND_BUTTON="id:chat send button active";

        //главные экраны приложения - названия
        FEED_TAB ="id:FEED";

        FEED_SEARCH_INIT_ELEMENT = "xpath://XCUIElementTypeButton[@name='ic search']";
        FEED_SEARCH_INPUT = "xpath://XCUIElementTypeApplication[@name='Dev Coach']/XCUIElementTypeWindow[1]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeTextField";
        FEED_SEARCH_INIT_BUTTON = "id:Search";

        SEARCH_RESULT_BY_SUBSTRING_TPL = "xpath://XCUIElementTypeStaticText[contains(@label,'{SUBSTRING}')]";
        //те что ниже не меняла
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
