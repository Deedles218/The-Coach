package lib.ui.factories;

import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class CoachFlowPageObjectFactory {
    private CoachFlowPageObjectFactory() {
    }

    public static CoachFlowPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) {
            return new AndroidCoachFlowPageObject(driver);
        }
        return IOSPageObjectFactory.create(() -> new iOSCoachFlowPageObject(driver));
    }

    /** Keep platform-specific login behavior, with a caller-owned dashboard readiness check. */
    public static CoachFlowPageObject get(RemoteWebDriver driver, Runnable dashboardReady) {
        java.util.Objects.requireNonNull(dashboardReady, "Dashboard readiness check is required");
        if (Platform.getInstance().isAndroid()) {
            return new AndroidCoachFlowPageObject(driver) {
                @Override public void waitForAuthorizedDashboard() { dashboardReady.run(); }
            };
        }
        return IOSPageObjectFactory.create(() -> new iOSCoachFlowPageObject(driver) {
            @Override public void waitForAuthorizedDashboard() { dashboardReady.run(); }
        });
    }
}
