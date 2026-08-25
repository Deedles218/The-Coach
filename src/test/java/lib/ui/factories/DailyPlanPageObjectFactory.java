package lib.ui.factories;

import lib.Platform;
import lib.ui.DailyPlanPageObject;
import lib.ui.android.AndroidDailyPlanPageObject;
import lib.ui.ios.iOSDailyPlanPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class DailyPlanPageObjectFactory {
    private DailyPlanPageObjectFactory() {
    }

    public static DailyPlanPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) {
            return new AndroidDailyPlanPageObject(driver);
        }
        return IOSPageObjectFactory.create(() -> new iOSDailyPlanPageObject(driver));
    }
}
