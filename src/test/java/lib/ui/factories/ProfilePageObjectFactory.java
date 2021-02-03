package lib.ui.factories;

import lib.Platform;
import lib.ui.ios.iOSProfilePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ProfilePageObjectFactory {
    public static iOSProfilePageObject get(RemoteWebDriver driver)
    {
        if(Platform.getInstance().isIOS()){
            return new iOSProfilePageObject(driver);
        }
        return null;
    }
}
