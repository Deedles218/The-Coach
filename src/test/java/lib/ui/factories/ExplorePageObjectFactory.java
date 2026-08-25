package lib.ui.factories;

import lib.Platform;
import lib.ui.ExplorePageObject;
import lib.ui.android.AndroidExplorePageObject;
import lib.ui.ios.iOSExplorePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class ExplorePageObjectFactory {
    private ExplorePageObjectFactory() {
    }

    public static ExplorePageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) {
            return new AndroidExplorePageObject(driver);
        }
        return IOSPageObjectFactory.create(() -> new iOSExplorePageObject(driver));
    }
}
