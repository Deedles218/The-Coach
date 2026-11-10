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
import org.junit.Test;

@Epic(value = "The Coach Kegel exercises")
public class KegelExerciseTests extends CoreTestCase {
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
        requireMobilePlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        dailyPlan.closeKegelExerciseFlowIfPresent();
        TestData.deterministicFixture();
        TestData.TestAccount account = TestData.dedicatedKegelPlayerAccount();
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        verifyAndroidMutableFixture();
        dailyPlan.openTodayTab();
        prepareKegelProgram();
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
    @Description("Checks all post-workout intensity options and selects Too Hard. An explicitly enabled disposable fixture completes a workout first when feedback is not already present.")
    @Step("Start test test04KegelCompletionIntensityCanBeChanged")
    @Severity(value = SeverityLevel.NORMAL)
    public void test04KegelCompletionIntensityCanBeChanged() {
        DailyPlanPageObject dailyPlan = openDailyPlan();

        if (!dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()
                && Boolean.getBoolean("coach.kegel.completeWorkoutForFeedback")) {
            // Opt in only for a disposable fixture: completing the workout
            // changes its progress and the selected feedback changes intensity.
            dailyPlan.openKegelExerciseFromDailyPlan();
            dailyPlan.assertKegelStartScreenIsDisplayed();
            dailyPlan.startKegelExercise();
            dailyPlan.assertKegelPlayerIsDisplayed();
            dailyPlan.closeKegelPlayerInstructionsIfPresent();
            dailyPlan.createWait(300).withMessage("Completed workout did not show intensity feedback")
                    .until(d -> dailyPlan.isKegelCompletionIntensityFeedbackDisplayed());
        }
        Assert.assertTrue(
                "Kegel completion intensity feedback is not available for the current progress-account state.",
                dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()
        );
        dailyPlan.selectTooHardKegelCompletionIntensityFeedback();
        dailyPlan.closePracticeCompletionFeedbackIfPresent();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {
            @Feature(value = "Kegel exercises"),
            @Feature(value = "Exercise instruction")
    })
    @DisplayName("Kegel exercise instruction shows steps and supports intensity selection")
    @Description("Opens an exercise instruction, verifies its image, title, reviewed step count, and adjust controls, changes intensity, restores it, and returns to the Kegel start page.")
    @Step("Start test test05KegelExerciseInstructionPages")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test05KegelExerciseInstructionPages() {
        DailyPlanPageObject dailyPlan = openKegelStartScreen();

        dailyPlan.openFirstKegelExerciseInstruction();
        dailyPlan.assertKegelExerciseInstructionIsDisplayed();
        if (Boolean.getBoolean("coach.kegel.inspectInstruction")) {
            io.qameta.allure.Allure.addAttachment("Regular Kegel instruction reference", "application/xml",
                    TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        }
        dailyPlan.selectNextKegelExerciseInstructionIntensity();
        dailyPlan.restorePreviousKegelExerciseInstructionIntensity();
        dailyPlan.returnFromKegelExerciseInstruction();
        dailyPlan.assertKegelStartScreenIsDisplayed();
        dailyPlan.closeKegelExerciseFlowIfPresent();
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
        requireMobilePlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);

        dailyPlan.closeKegelExerciseFlowIfPresent();
        if (dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()) {
            verifyAndroidMutableFixture();
            return dailyPlan;
        }
        TestData.deterministicFixture();
        TestData.TestAccount account = TestData.dedicatedKegelPlayerAccount();
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        verifyAndroidMutableFixture();
        if (!dailyPlan.isKegelCompletionIntensityFeedbackDisplayed()) {
            dailyPlan.openTodayTab();
            prepareKegelProgram();
        }
        return dailyPlan;
    }

    private void prepareKegelProgram() {
        String program = System.getProperty("coach.kegelPlayer.program");
        if (Platform.getInstance().isIOS() && program != null && !program.trim().isEmpty()) {
            new lib.ui.ios.iOSProgramSelectorPageObject(driver).ensureWorkoutProgramSelected(program.trim());
        }
    }
    private void verifyAndroidMutableFixture() {
        if (!Platform.getInstance().isAndroid()) return;
        String uid=System.getenv("COACH_ANDROID_MUTABLE_UID");
        Assert.assertNotNull("Kegel changes require a dedicated mutable Android fixture UID",uid);
        try { Assert.assertEquals("Wrong mutable fixture",uid,lib.AndroidModuleEvidence.currentUid()); }
        catch(Exception failure) { throw new AssertionError("Cannot verify mutable Android fixture",failure); }
    }

}
