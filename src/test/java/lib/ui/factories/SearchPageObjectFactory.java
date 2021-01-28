package lib.ui.factories;
import lib.Platform;
import lib.ui.ios.iOSSearchPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class SearchPageObjectFactory
{
    public static iOSSearchPageObject get(RemoteWebDriver driver)
    {
        if(Platform.getInstance().isIOS()){
            return new iOSSearchPageObject(driver);
        }
        return null;
    }
}
