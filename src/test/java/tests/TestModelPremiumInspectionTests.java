package tests;

import io.qameta.allure.Allure;
import lib.CoreTestCase;
import lib.Platform;
import lib.SimulatorTestIdentity;
import lib.TestData;
import lib.TestModelFixtureSupport;
import lib.RestoringTestAction;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.ios.iOSTestAccountPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.Assert;
import org.junit.Test;

/** Explicit read-only prerequisite inspection, not a pass of COA-8511/8512. */
public class TestModelPremiumInspectionTests extends CoreTestCase {
    private String anonymousUid;

    @Override protected boolean isPlatformSupported() {
        return Platform.getInstance().isIOS() && Boolean.getBoolean("coach.testModel.inspectPremium");
    }

    @Test public void inspectApprovedPremiumAccountWithoutReset() throws Exception {
        String key = System.getProperty("coach.testModel.case");
        Assert.assertTrue("Inspection is limited to COA-8511/8512", "COA-8511".equals(key) || "COA-8512".equals(key));
        TestData.TestAccount account = TestData.testModelAccount(key);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        iOSTestAccountPageObject mail = new iOSTestAccountPageObject(driver);
        if (mail.resumeAnonymousMailProfile()) {
            anonymousUid = SimulatorTestIdentity.currentUid();
            SimulatorTestIdentity.requireOwnedAnonymousMailFixture(anonymousUid);
            mail.loginExistingAccountFromAnonymousProfile(account);
            Assert.assertNotEquals("Inspection still uses the anonymous account", anonymousUid, SimulatorTestIdentity.currentUid());
        } else {
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        }
        coach.assertAuthorizedDashboardIsDisplayed();
        DailyPlanPageObject daily = DailyPlanPageObjectFactory.get(driver);
        daily.openTodayTab();
        new TestModelFixtureSupport(driver, key).inspectPremiumAccount();
        Allure.addAttachment("Read-only current Today state", "Program: " + daily.getActiveProgramName()
                + "\nDay: " + daily.getCurrentDayLabel() + "\nProgress: " + daily.getProgramProgressValue());
        daily.openProgramSelector();
        try {
            Allure.addAttachment("Read-only program selector state", "application/xml",
                    TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        } finally {
            daily.closeProgramSelector();
        }
        if (Boolean.getBoolean("coach.testModel.inspectOtherPrograms")) inspectOtherPrograms(key);
    }

    private void inspectOtherPrograms(String key) throws Exception {
        String expectedUid = System.getenv("COACH_" + key.replace("-", "") + "_UID");
        Assert.assertNotNull("Program inspection requires a verified approved UID", expectedUid);
        Assert.assertEquals("Wrong account; no program navigation allowed", expectedUid, SimulatorTestIdentity.currentUid());
        iOSProgramSelectorPageObject today = new iOSProgramSelectorPageObject(driver);
        String original = today.getActiveProgramName();
        Assert.assertEquals("Inspection baseline changed; preserve the current program", "Last Longer: Retain", original);
        String originalDay = today.getCurrentDayLabel();
        String originalProgress = today.getProgramProgressValue();
        RestoringTestAction.run(() -> {
            for (String program : new String[]{"Overall Health", "Kegel Challenge"}) {
                today.openProgramSelector();
                today.selectCustomizationInspectionProgram(program);
                today.waitForSelectedProgram(program);
                Allure.addAttachment("Program fixture candidate: " + program, "Program: " + today.getActiveProgramName()
                        + "\nDay: " + today.getCurrentDayLabel() + "\nProgress: " + today.getProgramProgressValue());
                Allure.addAttachment("Program fixture candidate UI: " + program, "application/xml",
                        TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
            }
        }, () -> {
            today.dismissSelectorIfOpen();
            if (!original.equals(today.getActiveProgramName())) {
                today.openProgramSelector();
                today.selectCustomizationInspectionProgram(original);
            }
            today.waitForSelectedProgram(original);
            Assert.assertEquals("Original program day changed during inspection", originalDay, today.getCurrentDayLabel());
            Assert.assertEquals("Original progress changed during inspection", originalProgress, today.getProgramProgressValue());
            Allure.addAttachment("Verified restoration after program inspection", original + "; " + originalDay + "; " + originalProgress);
        });
    }

    @Override protected void cleanupSessionForNextTest() {
        if (anonymousUid != null) {
            try {
                if (anonymousUid.equals(SimulatorTestIdentity.currentUid())) {
                    new iOSTestAccountPageObject(driver).preserveAnonymousMailProfile();
                    return;
                }
            } catch (Exception error) {
                throw new IllegalStateException("Cannot safely restore the inspection session", error);
            }
        }
        super.cleanupSessionForNextTest();
    }
}
