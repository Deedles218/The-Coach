package lib.ui.factories;

import lib.Platform;
import lib.ui.BookmarksPageObject;
import lib.ui.android.AndroidBookmarksPageObject;
import lib.ui.ios.iOSBookmarksPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class BookmarksPageObjectFactory {
    private BookmarksPageObjectFactory() {
    }

    public static BookmarksPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) {
            return new AndroidBookmarksPageObject(driver);
        }
        return IOSPageObjectFactory.create(() -> new iOSBookmarksPageObject(driver));
    }
}
