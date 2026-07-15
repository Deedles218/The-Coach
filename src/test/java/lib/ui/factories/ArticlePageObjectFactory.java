package lib.ui.factories;
import lib.ui.HWPageObject.ArticlePageObject;
import lib.ui.ios.iOSArticlePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class ArticlePageObjectFactory {
    private ArticlePageObjectFactory() {
    }

    public static ArticlePageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSArticlePageObject(driver));
    }
}



