package lib.ui.ios;

import lib.ui.ModulesPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Locators observed in DailyDaySwitcherView; English, portrait, default text size. */
public final class iOSModulesPageObject extends ModulesPageObject {
    private final iOSDailyPlanPageObject today;
    private static final String HEADER = "//XCUIElementTypeOther[@name='DailyDaySwitcherView']";

    public iOSModulesPageObject(RemoteWebDriver driver) {
        super(driver, "id:DailyDaySwitcherView",
                "xpath:" + HEADER + "/XCUIElementTypeStaticText[starts-with(@name,'Module ')]",
                "xpath:" + HEADER + "/XCUIElementTypeStaticText[starts-with(@name,'Stage ')]",
                "xpath:" + HEADER + "/XCUIElementTypeStaticText[starts-with(@name,'Day ')]");
        today = new iOSDailyPlanPageObject(driver);
    }

    @Override public void openTodayTab() {
        new iOSCoachFlowPageObject(driver).closePartnerPromoIfPresent();
        today.openTodayTab();
    }
}
