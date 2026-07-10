package lib.ui.factories;
import lib.ui.HWPageObject.MyListsPageObject;
import lib.ui.ios.iOSMyListsPageObject;

import org.openqa.selenium.remote.RemoteWebDriver;

public final class MyListsPageObjectFactory {
    private MyListsPageObjectFactory() {
    }

    public static MyListsPageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSMyListsPageObject(driver));
    }
}
