package lib.ui.factories;

import lib.Platform;
import lib.ui.DailyPlanPageObject;
import lib.ui.ios.iOSDailyPlanPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class DailyPlanPageObjectFactory {
    public static DailyPlanPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isIOS()) {
            return new iOSDailyPlanPageObject(driver);
        }

        return null;
    }
}
