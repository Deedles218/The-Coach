package lib.ui.factories;

import lib.ui.ExplorePageObject;
import lib.ui.ios.iOSExplorePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class ExplorePageObjectFactory {
    private ExplorePageObjectFactory() {
    }

    public static ExplorePageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSExplorePageObject(driver));
    }
}
