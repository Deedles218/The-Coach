package lib.ui.factories;

import lib.Platform;
import lib.ui.OnboardingSlidesPageObject;
import lib.ui.android.AndroidOnboardingSlidesPageObject;
import lib.ui.ios.iOSOnboardingSlidesPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.io.IOException;

public final class OnboardingSlidesPageObjectFactory {
    private OnboardingSlidesPageObjectFactory() { }
    public static OnboardingSlidesPageObject get(RemoteWebDriver driver) throws IOException {
        if (Platform.getInstance().isAndroid()) return new AndroidOnboardingSlidesPageObject(driver);
        if (Platform.getInstance().isIOS()) return new iOSOnboardingSlidesPageObject(driver);
        throw new IllegalStateException("Product slides require Android or iOS");
    }
}
