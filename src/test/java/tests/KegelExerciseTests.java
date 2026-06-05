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
}
