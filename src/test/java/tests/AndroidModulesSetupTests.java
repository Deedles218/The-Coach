package tests;

import lib.*;
import lib.ui.OnboardingGoal;
import lib.ui.OnboardingPageObject;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.android.AndroidDailyPlanPageObject;
import org.junit.*;

/** Explicit approved initial setup; never part of ModulesSuite. */
public class AndroidModulesSetupTests extends AndroidTestCase {
    @Override protected boolean isPlatformSupported() {
        return super.isPlatformSupported() && Boolean.getBoolean("coach.modules.completeOnboarding");
    }

    @Test public void completeApprovedInitialLastLongerQuestionnaire() throws Exception {
        String expected = System.getenv("COACH_COA9044_UID");
        Assert.assertNotNull("Approved module account UID is required", expected);
        Assert.assertEquals("Wrong account; questionnaire preserved", expected, AndroidModuleEvidence.currentUid());
        OnboardingPageObject onboarding = OnboardingPageObjectFactory.get(driver);
        onboarding.selectGoal(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
        onboarding.completeQuestionnaire();
        onboarding.closePaywallsAndPopups();
        onboarding.waitForToday();
        new AndroidDailyPlanPageObject(driver).openTodayTab();
    }
}
