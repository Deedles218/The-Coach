package tests;

import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.SimulatorTestIdentity;
import lib.ui.OnboardingGoal;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.ios.iOSTestAccountPageObject;
import org.junit.Test;

/** Explicit setup entry point, excluded from normal runs unless opted in. */
public class TestModelAccountProvisioningTests extends CoreTestCase {
    @Override
    protected boolean isPlatformSupported() {
        return Platform.getInstance().isIOS() && Boolean.getBoolean("coach.testModel.provision");
    }

    @Test
    public void testProvisionAndVerifyDedicatedAccount() throws Exception {
        String key = System.getProperty("coach.testModel.case");
        TestData.TestAccount account = TestData.testModelAccount(key);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        iOSTestAccountPageObject registration = new iOSTestAccountPageObject(driver);
        if (registration.resumeAnonymousRegistration(account)) {
            SimulatorTestIdentity.record(key);
            return;
        }
        coach.ensureLoggedOutOnStartScreen();
        if (registration.loginIfRegistered(account)) {
            SimulatorTestIdentity.record(key);
            return;
        }
        OnboardingGoal goal = "COA-8517".equals(key)
                ? OnboardingGoal.BOOST_OVERALL_HEALTH : OnboardingGoal.BEAT_PREMATURE_EJACULATION;
        OnboardingPageObjectFactory.get(driver).completeNewUserJourney(goal);
        registration.linkAndVerify(account);
        SimulatorTestIdentity.record(key);
    }
}
