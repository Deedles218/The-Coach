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
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;

@Epic(value = "The Coach Daily Plan")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DailyPlanTests extends CoreTestCase {
    private static final String EXISTING_PROGRESS_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";

    @BeforeClass
    public static void deleteScreenshotsBeforeRun() {
        deleteScreenshotFiles(new File(System.getProperty("user.dir")));
        deleteScreenshotFiles(new File(System.getProperty("user.dir"), "target/screenshots"));
    }

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
        if (!Platform.getInstance().isIOS()) {
            return;
        }

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
        if (!Platform.getInstance().isIOS()) {
            return;
        }

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
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        Assume.assumeTrue(
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
    @Description("Starts from the progress-dependent Daily Plan, opens the first Daily Practice exercise, verifies the practice start screen, and returns without starting or completing it.")
    @Step("Start test test04DailyPlanExerciseOpensAndReturns")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test04DailyPlanExerciseOpensAndReturns() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        ensureExistingProgressDailyPlanState();
        dailyPlan.openTodayTab();
        dailyPlan.openFirstDailyPractice();
        if (dailyPlan.isLockedModulePopupDisplayed()) {
            dailyPlan.closeLockedModulePopup();
            Assume.assumeTrue(
                    "Current existing-progress Daily Plan state shows a locked-module popup instead of opening the practice player.",
                    false
            );
        }
        dailyPlan.assertDailyPracticeScreenIsDisplayed();
        dailyPlan.returnFromDailyPracticeToDailyPlan();
    }

    private void ensureExistingProgressDailyPlanState() {
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
    }

    private static void deleteScreenshotFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile() && isScreenshotFile(file)) {
                if (!file.delete()) {
                    System.out.println("Cannot delete old screenshot: " + file.getAbsolutePath());
                }
            }
        }
    }

    private static boolean isScreenshotFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith("_screenshot.png") || fileName.endsWith(".screenshot.png");
    }
}
