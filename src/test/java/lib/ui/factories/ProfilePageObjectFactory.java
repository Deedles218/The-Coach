package lib.ui.factories;

import lib.ui.ProfilePageObject;
import lib.ui.ios.iOSProfilePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class ProfilePageObjectFactory {
    private ProfilePageObjectFactory() {
    }

    public static ProfilePageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSProfilePageObject(driver));
    }
}
