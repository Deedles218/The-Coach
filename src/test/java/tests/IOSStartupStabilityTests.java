package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.ios.iOSStartupStabilityPageObject;
import org.junit.Assert;
import org.junit.Test;

@Epic("The Coach iOS startup stability")
public class IOSStartupStabilityTests extends CoreTestCase {
    @Override protected boolean isPlatformSupported() {
        return Platform.getInstance().isIOS();
    }

    @Test
    @Issues({@Issue("COA-9746"), @Issue("COA-9747")})
    @Description("Log in with the existing regression account, then observe repeated cold launches before dismissing conditional startup screens. Preserve screenshots and fail if the app exits or TestFlight reports a crash.")
    public void testAuthorizedColdLaunchesRemainResponsive() throws Exception {
        Assert.assertFalse("Crash retest must not dismiss TestFlight crash reports automatically",
                Boolean.getBoolean("coach.ios.testFlightReports"));
        UpdateEvidence.attachInstalledBuild();
        int count = Integer.getInteger("coach.startup.launchCount", 5);
        int observationSeconds = Integer.getInteger("coach.startup.observationSeconds", 15);
        Assert.assertTrue("Cold launch count must be between 1 and 20", count >= 1 && count <= 20);
        TestData.TestAccount account = TestData.existingProgressAccount();
        iOSStartupStabilityPageObject startup = new iOSStartupStabilityPageObject(driver);
        startup.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        for (int attempt = 1; attempt <= count; attempt++) {
            startup.coldLaunchAndVerify(attempt, observationSeconds);
        }
    }
}
