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
import lib.ui.OnboardingGoal;
import lib.ui.OnboardingPageObject;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic("The Coach new-user onboarding")
public class OnboardingGoalProgramTests extends CoreTestCase {
    @Test
    @Features({
            @Feature("New user"),
            @Feature("Questionnaire"),
            @Feature("Paywall"),
            @Feature("Today program")
    })
    @DisplayName("Selected onboarding goal maps to the program shown on Today")
    @Description("Creates an anonymous user from Start now, selects a configured goal, completes the runtime questionnaire, closes all paywalls and optional popups, then verifies the Today program mapping.")
    @Step("Run goal-to-Today new-user E2E")
    @Severity(SeverityLevel.BLOCKER)
    public void testSelectedGoalMatchesTodayProgram() {
        if (Platform.getInstance().isAndroid()) {
            Assert.assertTrue(
                    "Android new-user E2E requires -Dandroid.app=<apk> "
                            + "-Dandroid.noReset=false -Dandroid.fullReset=true",
                    Platform.getInstance().isFreshAndroidInstallConfigured()
            );
        }

        OnboardingGoal goal = OnboardingGoal.fromConfiguredValue(
                Platform.getInstance().getOnboardingGoal()
        );
        OnboardingPageObject onboarding = OnboardingPageObjectFactory.get(driver);
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Onboarding page object is unavailable for current platform", onboarding);
        Assert.assertNotNull("Daily Plan page object is unavailable for current platform", dailyPlan);

        int paywallsClosed = onboarding.completeNewUserJourney(goal);
        dailyPlan.assertTodayTabIsSelected();
        dailyPlan.assertActiveProgramMatches(goal.getExpectedProgram());
        Assert.assertTrue(
                "The onboarding flow reached Today without showing the required paywall",
                paywallsClosed > 0
        );
    }
}
