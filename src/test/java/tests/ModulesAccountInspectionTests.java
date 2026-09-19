package tests;

import io.qameta.allure.Allure;
import lib.*;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.OnboardingPageObject;
import lib.ui.OnboardingGoal;
import lib.ui.ios.iOSModulesPageObject;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.*;
import org.openqa.selenium.json.Json;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Snapshot only by default; initial onboarding is a separate explicit opt-in. */
public class ModulesAccountInspectionTests extends CoreTestCase {
    @Override protected boolean isPlatformSupported() {
        return Platform.getInstance().isIOS() && Boolean.getBoolean("coach.modules.inspectAccount");
    }

    @Test public void inspectSuppliedModuleAccount() throws Exception {
        Assert.assertEquals("Inspection requires preprod", "com.vamapps.preprod.The-Coach",
                Platform.getInstance().getIOSBundleId());
        TestData.TestAccount account = TestData.testModelAccount("COA-9044");
        long boundary = System.currentTimeMillis();
        if (Boolean.getBoolean("coach.modules.completeOnboarding")) {
            // Resume only the account whose current UID/email was verified by
            // the runner/operator before authorizing initial questionnaire setup.
            String expectedUid = System.getenv("COACH_COA9044_UID");
            Assert.assertNotNull("Verify the supplied account UID before onboarding", expectedUid);
            Assert.assertEquals("Wrong account; questionnaire preserved", expectedUid, SimulatorTestIdentity.currentUid());
            OnboardingPageObject onboarding = OnboardingPageObjectFactory.get(driver);
            onboarding.selectGoal(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
            onboarding.completeQuestionnaire();
            onboarding.closePaywallsAndPopups();
            new iOSCoachFlowPageObject(driver).closePartnerPromoIfPresent();
            onboarding.waitForToday();
        } else {
            CoachFlowPageObjectFactory.get(driver).ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        }
        iOSModulesPageObject today = new iOSModulesPageObject(driver);
        today.openTodayTab();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("uid", SimulatorTestIdentity.currentUid());
        result.put("program", today.getActiveProgramName());
        result.put("stage", today.getCurrentDayLabel());
        result.put("progress", today.getProgramProgressValue());
        result.put("actionBoundary", boundary);
        result.put("loginVerified", true);
        result.put("initialOnboardingCompleted", Boolean.getBoolean("coach.modules.completeOnboarding"));
        String json = new Json().toJson(result);
        Path path = Paths.get("target", "modules-premium-inspection.json");
        Files.createDirectories(path.getParent());
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        Allure.addAttachment("Supplied module account baseline (no progress changes)", "application/json", json, "json");
    }
}
