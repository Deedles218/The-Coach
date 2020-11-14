package lib.ui.ios;
import lib.ui.MyListsPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSMyListsPageObject extends MyListsPageObject {
    static
    {
          ARTICLE_BY_TITLE_TPL ="xpath://XCUIElementTypeLink[contains(@name,'{TITLE}')]";
          SWIPE_DELETE_BUTTON = "id:swipe action delete";
          ARTICLE_BY_DESCRIPTION_TPL = "xpath://XCUIElementTypeLink[contains(@name, '{DESCRIPTION}')]";
    }
    public iOSMyListsPageObject(RemoteWebDriver driver)
    {
        super(driver);
    }
}
