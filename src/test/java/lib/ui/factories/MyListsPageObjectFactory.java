package lib.ui.factories;
import lib.Platform;
import lib.ui.HWPageObject.MyListsPageObject;
import lib.ui.ios.iOSMyListsPageObject;

import org.openqa.selenium.remote.RemoteWebDriver;

public class MyListsPageObjectFactory {
    public static MyListsPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isIOS()) {
            return new iOSMyListsPageObject(driver);
        }

        return null;
    }
}

