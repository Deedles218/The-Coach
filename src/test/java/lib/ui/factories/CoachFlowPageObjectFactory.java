package lib.ui.factories;

import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class CoachFlowPageObjectFactory {
    public static CoachFlowPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isIOS()) {
            return new iOSCoachFlowPageObject(driver);
        }
        return null;
    }
}
