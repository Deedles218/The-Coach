package lib.ui.ios;
import lib.ui.HWPageObject.ArticlePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSArticlePageObject extends ArticlePageObject {
    static {
        TITLE = "id:Java";
        DESCRIPTION = "id:High-level programming language";
        FOOTER_ELEMENT = "id:View article in browser";
        OPTIONS_ADD_TO_MY_READING_LIST_BUTTON = "id:Save for later";
        CLOSE_ARTICLE_BUTTON = "id:Back";
    }
    public iOSArticlePageObject(RemoteWebDriver driver)
    {
        super(driver);
    }
}
