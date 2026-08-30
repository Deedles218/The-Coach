package lib.ui.factories;

import lib.Platform;
import lib.ui.OnboardingPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import lib.ui.ios.iOSOnboardingPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class OnboardingPageObjectFactory {
    private OnboardingPageObjectFactory() {
    }

    public static OnboardingPageObject get(RemoteWebDriver driver) {
        if (Platform.getInstance().isAndroid()) {
            return new AndroidOnboardingPageObject(driver);
        }
        return IOSPageObjectFactory.create(() -> new iOSOnboardingPageObject(driver));
    }
}
