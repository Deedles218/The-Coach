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
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
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
            @Feature(value = "Daily Practice")
    })
    @DisplayName("COA-7967 Daily Plan shows current day and practice")
    @Description("Opens Today and verifies the Daily Plan day switcher and Daily Practice content visible for the existing-progress account.")
    @Step("Start test test01DailyPlanLessonsAndPracticeDisplayed")
    @Severity(value = SeverityLevel.NORMAL)
    public void test01DailyPlanLessonsAndPracticeDisplayed() {
        requireMobilePlatform();

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        dailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Program arrows")
    })
    @DisplayName("COA-7961 COA-7985 Daily Plan does not advance to a locked next day")
    @Description("Verifies that the current Daily Plan stage stays selected after tapping the next-day arrow; closes the locked next-day popup if the build shows it.")
    @Step("Start test test02DailyPlanLockedNextDayAndFirstDayLeftArrow")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test02DailyPlanLockedNextDayAndFirstDayLeftArrow() {
        requireMobilePlatform();

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        dailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        dailyPlan.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
    }

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Daily Lessons")
    })
    @DisplayName("COA-7966 Daily Plan lesson opens and returns")
    @Description("Starts from the progress-dependent Daily Plan, opens the first available Daily Lesson when the current program day exposes one, verifies content, and returns to Daily Plan.")
    @Step("Start test test03DailyPlanLessonOpensAndReturns")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test03DailyPlanLessonOpensAndReturns() {
        requireMobilePlatform();

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        Assert.assertTrue(
                "Current existing-progress Daily Plan state does not expose Daily Lessons; lesson-opening coverage needs a day/program fixture with a visible lesson.",
                dailyPlan.hasDailyLessonsAvailable()
        );
        dailyPlan.openFirstDailyLesson();
        dailyPlan.assertDailyLessonScreenIsDisplayed();
        dailyPlan.returnFromDailyLessonToDailyPlan();
    }

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Daily Practice"),
            @Feature(value = "Exercises")
    })
    @DisplayName("COA-7967 Daily Plan exercise opens and returns")
    @Description("Starts from the progress-dependent Daily Plan, opens the first Daily Practice exercise, verifies its native start screen or guide-video controls, and returns without completing the activity.")
    @Step("Start test test04DailyPlanExerciseOpensAndReturns")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test04DailyPlanExerciseOpensAndReturns() {
        requireMobilePlatform();

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        dailyPlan.openFirstDailyPractice();
        if (dailyPlan.isLockedModulePopupDisplayed()) {
            dailyPlan.closeLockedModulePopup();
            Assert.fail("Current existing-progress Daily Plan state shows a locked-module popup instead of opening the practice player.");
        }
        dailyPlan.assertDailyPracticeScreenIsDisplayed();
        dailyPlan.returnFromDailyPracticeToDailyPlan();
    }

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Daily Plan customization")
    })
    @DisplayName("COA-8506 COA-8513 COA-8514 Daily Plan item remove action can be cancelled")
    @Description("Long-presses the first Daily Practice item, verifies customization actions, opens the Remove from Daily Plan confirmation, cancels it, and verifies the item remains.")
    @Step("Start test test05DailyPlanRemoveActionCanBeCancelled")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test05DailyPlanRemoveActionCanBeCancelled() {
        org.junit.Assume.assumeTrue("Android has no postpone/remove/recover actions (confirmed by product owner)", Platform.getInstance().isIOS());

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        Assert.assertTrue(
                "Current existing-progress Daily Plan state does not expose customization actions; COA-8506/8513/8514 need a fixture with a removable Daily Plan card.",
                dailyPlan.tryOpenCustomizationActionsForFirstDailyPractice()
        );
        dailyPlan.tapRemoveFromDailyPlanAction();
        dailyPlan.assertRemoveFromDailyPlanConfirmationIsDisplayed();
        dailyPlan.cancelRemoveFromDailyPlan();
        dailyPlan.assertFirstDailyPracticeStillDisplayed();
    }

    private void ensureExistingProgressDailyPlanState() {
        TestData.deterministicFixture();
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        TestData.TestAccount account = TestData.existingProgressAccount();
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
    }

}
