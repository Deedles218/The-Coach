package lib.ui.factories;
import lib.Platform;
import lib.ui.HWPageObject.ArticlePageObject;
import lib.ui.ios.iOSArticlePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ArticlePageObjectFactory
{
    public static ArticlePageObject get(RemoteWebDriver driver){
        if (Platform.getInstance().isIOS())

        return new iOSArticlePageObject(driver);
        return null;
    }
}




