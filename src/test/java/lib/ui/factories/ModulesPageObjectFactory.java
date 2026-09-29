package lib.ui.factories;

import lib.Platform;
import lib.ui.ModulesPageObject;
import lib.ui.android.AndroidModulesPageObject;
import lib.ui.ios.iOSModulesPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class ModulesPageObjectFactory {
    private ModulesPageObjectFactory() { }
    public static ModulesPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) return new AndroidModulesPageObject(driver);
        return IOSPageObjectFactory.create(() -> new iOSModulesPageObject(driver));
    }
}
