package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@Epic(value = "The Coach Daily Plan")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DailyPlanTests extends CoreTestCase {
    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Daily Lessons"),
            @Feature(value = "Daily Practice")
    })
    @DisplayName("COA-7966 COA-7967 Daily Plan shows lessons and practice")
    @Description("Opens Today and verifies the Daily Plan day switcher, Daily Lessons, and Daily Practice content visible on the connected male preprod build.")
    @Step("Start test test01DailyPlanLessonsAndPracticeDisplayed")
    @Severity(value = SeverityLevel.NORMAL)
    public void test01DailyPlanLessonsAndPracticeDisplayed() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        dailyPlan.openTodayTab();
        dailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        dailyPlan.assertDailyLessonsAreDisplayed();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Program arrows")
    })
    @DisplayName("COA-7961 COA-7985 Daily Plan does not advance when activities are incomplete")
    @Description("Verifies that an incomplete Daily Plan day stays selected after tapping the next-day arrow; closes the locked next-day popup if the build shows it.")
    @Step("Start test test02DailyPlanLockedNextDayAndFirstDayLeftArrow")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test02DailyPlanLockedNextDayAndFirstDayLeftArrow() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        dailyPlan.openTodayTab();
        dailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        dailyPlan.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
        dailyPlan.assertLeftArrowKeepsCurrentDaySelected();
    }
}
