package lib.ui.factories;
import lib.Platform;
import lib.ui.ios.iOSNavigationUI;
import org.openqa.selenium.remote.RemoteWebDriver;

public class NavigationUIFactory
{
    public static iOSNavigationUI get(RemoteWebDriver driver)
    {
        if (Platform.getInstance().isIOS()){
            return new iOSNavigationUI(driver);
        }
        return null;
    }
}
