package lib.ui.factories;

import lib.Platform;
import lib.ui.BookmarksPageObject;
import lib.ui.ios.iOSBookmarksPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class BookmarksPageObjectFactory {
    private BookmarksPageObjectFactory() {
    }

    public static BookmarksPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) {
            throw new UnsupportedOperationException("Bookmarks Xray cases are currently scoped to iOS");
        }
        return IOSPageObjectFactory.create(() -> new iOSBookmarksPageObject(driver));
    }
}
