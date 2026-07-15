package lib.ui.factories;

import lib.ui.DailyPlanPageObject;
import lib.ui.ios.iOSDailyPlanPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class DailyPlanPageObjectFactory {
    private DailyPlanPageObjectFactory() {
    }

    public static DailyPlanPageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSDailyPlanPageObject(driver));
    }
}
