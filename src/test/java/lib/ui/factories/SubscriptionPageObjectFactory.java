package lib.ui.factories;

import lib.ui.SubscriptionPageObject;
import lib.ui.ios.iOSSubscriptionPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class SubscriptionPageObjectFactory {
    private SubscriptionPageObjectFactory() {
    }

    public static SubscriptionPageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSSubscriptionPageObject(driver));
    }
}
