package lib.ui.factories;
import lib.ui.NavigationUI;
import lib.ui.ios.iOSNavigationUI;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class NavigationUIFactory {
    private NavigationUIFactory() {
    }

    public static NavigationUI get(RemoteWebDriver driver) {
        return IOSPageObjectFactory.create(() -> new iOSNavigationUI(driver));
    }
}
