package lib.ui.factories;
import lib.ui.SearchPageObject;
import lib.ui.ios.iOSSearchPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class SearchPageObjectFactory {
    private SearchPageObjectFactory() {
    }

    public static SearchPageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSSearchPageObject(driver));
    }
}
