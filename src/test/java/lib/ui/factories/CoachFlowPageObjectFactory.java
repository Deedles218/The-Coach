package lib.ui.factories;

import lib.ui.CoachFlowPageObject;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class CoachFlowPageObjectFactory {
    private CoachFlowPageObjectFactory() {
    }

    public static CoachFlowPageObject get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSCoachFlowPageObject(driver));
    }
}
