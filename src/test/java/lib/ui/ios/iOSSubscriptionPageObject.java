package lib.ui.ios;

import lib.ui.SubscriptionPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSSubscriptionPageObject extends SubscriptionPageObject {
    static {
        // Preferred app-side test-id contract. The text fallbacks keep the
        // suite usable while the application migrates from labels to ids.
        PAYWALL_MARKER = "id:subscription_paywall";
        PAYWALL_MARKER_FALLBACK = "id:START FREE TRIAL";
        START_FREE_TRIAL_BUTTON = "id:subscription_start_free_trial";
        START_FREE_TRIAL_BUTTON_FALLBACK = "id:START FREE TRIAL";
        SELECTED_PLAN_BUTTON = "id:subscription_selected_plan";
        RESTORE_PURCHASES_BUTTON = "id:subscription_restore_purchases";
        RESTORE_PURCHASES_BUTTON_FALLBACK = "id:RESTORE PURCHASES";
        PREMIUM_ACCESS_MARKER = "id:PREMIUM SUBSCRIBER";
        RESTORE_SUCCESS_MARKER = "id:subscription_restored";
        RESTORE_SUCCESS_MARKER_FALLBACK = "xpath://XCUIElementTypeButton[@name='Today' and @visible='true']";
        RESTORE_RESULT_MARKER = "xpath://XCUIElementTypeStaticText[contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'restor') or contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'expir') or contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'no active') or contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'not found')]";
        STOREKIT_CONFIRM_BUTTON = "id:Buy";
    }

    public iOSSubscriptionPageObject(RemoteWebDriver driver) {
        super(driver);
    }
}
