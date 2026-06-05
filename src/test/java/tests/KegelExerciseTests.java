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
import org.junit.Test;

@Epic(value = "The Coach Kegel exercises")
public class KegelExerciseTests extends CoreTestCase {
    private static final String EXISTING_PROGRESS_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Kegel exercises"),
            @Feature(value = "Exercise player")
    })
    @DisplayName("Kegel exercise opens, plays, controls work, and exits safely")
    @Description("Starts from an existing-progress Daily Plan, opens the Kegel workout, verifies the start screen, starts the player, checks controls, and exits through confirmation.")
    @Step("Start test test01KegelExercisePlayerFlow")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test01KegelExercisePlayerFlow() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        dailyPlan.closeKegelExerciseFlowIfPresent();
        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        dailyPlan.openTodayTab();
        dailyPlan.openKegelExerciseFromDailyPlan();
        dailyPlan.assertKegelStartScreenIsDisplayed();
        dailyPlan.startKegelExercise();
        dailyPlan.assertKegelPlayerIsDisplayed();
        dailyPlan.pressAllKegelPlayerControls();
        dailyPlan.exitKegelExercisePlayer();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {
            @Feature(value = "Kegel exercises"),
            @Feature(value = "Exercise player settings")
    })
    @DisplayName("Kegel sound and vibration controls support independent configurations")
    @Description("Checks enabled and disabled sound/vibration combinations independently and restores both controls to enabled.")
    @Step("Start test test02KegelSoundAndVibrationConfigurations")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test02KegelSoundAndVibrationConfigurations() {
        DailyPlanPageObject dailyPlan = openKegelPlayer();

        dailyPlan.closeKegelPlayerInstructionsIfPresent();
        dailyPlan.setKegelPlayerSoundEnabled(true);
        dailyPlan.setKegelPlayerVibrationEnabled(true);
        dailyPlan.setKegelPlayerSoundEnabled(false);
        dailyPlan.assertKegelPlayerVibrationEnabled(true);
        dailyPlan.setKegelPlayerSoundEnabled(true);
        dailyPlan.setKegelPlayerVibrationEnabled(false);
        dailyPlan.assertKegelPlayerSoundEnabled(true);
        dailyPlan.setKegelPlayerSoundEnabled(false);
        dailyPlan.assertKegelPlayerVibrationEnabled(false);
        dailyPlan.setKegelPlayerSoundEnabled(true);
        dailyPlan.setKegelPlayerVibrationEnabled(true);
        dailyPlan.exitKegelExercisePlayer();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {
            @Feature(value = "Daily Plan"),
            @Feature(value = "Kegel exercises"),
            @Feature(value = "Exercise start screen")
    })
    @DisplayName("Kegel start page shows level, goal, intensity, duration, and exercise information")
    @Description("Verifies the Kegel start page information and confirms that multiple exercises include intensity and duration metadata.")
    @Step("Start test test03KegelStartPageExerciseInformation")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test03KegelStartPageExerciseInformation() {
        DailyPlanPageObject dailyPlan = openKegelStartScreen();

        dailyPlan.assertKegelStartScreenExerciseInformation();
        dailyPlan.closeKegelExerciseFlowIfPresent();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {
            @Feature(value = "Kegel exercises"),
            @Feature(value = "Exercise intensity")
    })
    @DisplayName("Kegel completion feedback allows changing exercise intensity")
    @Description("Checks all post-workout intensity options and selects Too Hard when completion feedback is available for the progress account.")
    @Step("Start test test04KegelCompletionIntensityCanBeChanged")
    @Severity(value = SeverityLevel.NORMAL)
    public void test04KegelCompletionIntensityCanBeChanged() {
        DailyPlanPageObject dailyPlan = openDailyPlan();

        Assume.assumeTrue(
                "Kegel completion intensity feedback is not available for the current progress-account state.",
                dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()
        );
        dailyPlan.selectTooHardKegelCompletionIntensityFeedback();
        dailyPlan.closePracticeCompletionFeedbackIfPresent();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    private DailyPlanPageObject openKegelPlayer() {
        DailyPlanPageObject dailyPlan = openKegelStartScreen();
        dailyPlan.startKegelExercise();
        dailyPlan.assertKegelPlayerIsDisplayed();
        return dailyPlan;
    }

    private DailyPlanPageObject openKegelStartScreen() {
        DailyPlanPageObject dailyPlan = openDailyPlan();
        dailyPlan.closePracticeCompletionFeedbackIfPresent();
        dailyPlan.openKegelExerciseFromDailyPlan();
        dailyPlan.assertKegelStartScreenIsDisplayed();
        return dailyPlan;
    }

    private DailyPlanPageObject openDailyPlan() {
        if (!Platform.getInstance().isIOS()) {
            Assume.assumeTrue("Kegel exercise coverage is iOS-only.", false);
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        dailyPlan.closeKegelExerciseFlowIfPresent();
        if (dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()) {
            return dailyPlan;
        }
        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        if (!dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()) {
            dailyPlan.openTodayTab();
        }
        return dailyPlan;
    }
}
