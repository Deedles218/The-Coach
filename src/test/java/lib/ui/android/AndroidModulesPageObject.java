package lib.ui.android;

import lib.ui.ModulesPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Resource IDs verified on Android manProd 1.40.21 / API 36. */
public final class AndroidModulesPageObject extends ModulesPageObject {
    private static final String PREFIX = "com.vamapps.thecoach:id/";
    private final AndroidDailyPlanPageObject today;

    public AndroidModulesPageObject(RemoteWebDriver driver) {
        super(driver, "xpath://android.widget.TextView[@resource-id='" + PREFIX + "tvModuleName']/..",
                "id:" + PREFIX + "tvModuleName", "id:" + PREFIX + "tvDaysNumber",
                "xpath://android.widget.TextView[@resource-id='" + PREFIX + "tvDaysNumber' and starts-with(@text,'Day ')]");
        today = new AndroidDailyPlanPageObject(driver);
        // The legacy DailyPlan Android defaults use generic dialog placeholders.
        // These identifiers were observed on the actual module-blocking dialog.
        LOCKED_MODULE_POPUP_TITLE = "xpath://android.widget.TextView[@resource-id='" + PREFIX
                + "tvTitle' and @text='Complete current module to unlock the next one']";
        LOCKED_MODULE_POPUP_BUTTON = "id:" + PREFIX + "okButton";
    }

    @Override public void openTodayTab() { today.openTodayTab(); }

    @Override public String getActiveProgramName() {
        return today.getActiveProgramName();
    }

    @Override public String getProgramProgressValue() {
        return getElementAccessibleName(waitForElementVisible("id:" + PREFIX + "tvPercentages",
                "Android module progress is absent", 10));
    }
}
