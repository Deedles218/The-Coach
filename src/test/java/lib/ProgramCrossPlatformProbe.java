package lib;

import io.appium.java_client.InteractsWithApps;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.android.AndroidMyProgramsPageObject;
import org.junit.Assert;
import org.junit.runner.Description;
import org.openqa.selenium.json.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/** Isolated Android JVM: existing Page Objects use platform-scoped static locators. */
public final class ProgramCrossPlatformProbe extends CoreTestCase {
    @Override protected void cleanupSessionForNextTest() { }

    public static void main(String[] args) throws Throwable {
        Assert.assertEquals("Android probe needs a baseline and result path", 2, args.length);
        Assert.assertTrue("The sync probe must run on Android", Platform.getInstance().isAndroid());
        MobileCaseFixture fixture = MobileCaseFixture.load("COA-9795");
        ProgramSelectionState baseline = new ProgramSelectionState(new Json().toType(
                new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8), Map.class));
        ProgramCrossPlatformProbe probe = new ProgramCrossPlatformProbe();
        try {
            Assert.assertEquals("Preserve the reviewed installed Android APK", "true", System.getProperty("android.noReset"));
            Assert.assertEquals("Never reset a sync fixture", "false", System.getProperty("android.fullReset"));
            Assert.assertNull("Attach to the reviewed installed APK; do not supply android.app or ANDROID_APP", Platform.getInstance().getAndroidAppPath());
            probe.setUp();
            AndroidCoachFlowPageObject coach = new AndroidCoachFlowPageObject(probe.driver);
            TestData.TestAccount account = TestData.existingProgressAccount();
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
            ProgramSelectionApi.assertDedicatedProfile("COA-9795");
            ProgramSelectionState observed = ProgramSelectionApi.read();
            observed.assertSameRoles(baseline);
            observed.assertSameProgress(baseline);
            observed.assertActive(fixture.required("switch.id"));
            AndroidMyProgramsPageObject today = new AndroidMyProgramsPageObject(probe.driver, fixture);
            today.openTodayTab();
            Assert.assertEquals("Android did not load iOS Active Program", observed.program(observed.active()).get("title"), today.getActiveProgramName());
            today.waitForElementVisible(fixture.selector("switch.content.ready"), "Android loaded another program's cards", 15);
            today.openProgramSelector();
            today.assertMatchesState(observed);
            today.selectSavedProgram((String) baseline.main().get("title"), strictBoolean(fixture.required("confirmation")));
            today.waitForElementVisible(fixture.selector("main.content.ready"), "Android Main cards did not return", 15);
            ProgramSelectionState returned = ProgramSelectionApi.read();
            returned.assertSameRoles(baseline);
            returned.assertSameProgress(baseline);
            returned.assertActive(baseline.mainId());
            today.openProgramSelector();
            today.assertMatchesState(returned);
            today.closeProgramSelector();
            Files.write(Paths.get(args[1]), new Json().toJson(returned.raw()).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable failure) {
            probe.captureFailureArtifacts(failure, Description.createTestDescription(ProgramCrossPlatformProbe.class, "androidSyncBeforeReturningToIOS"));
            throw failure;
        } finally {
            if (probe.driver != null) {
                try { ((InteractsWithApps) probe.driver).terminateApp(Platform.getInstance().getAndroidAppPackage()); }
                finally { probe.driver.quit(); }
            }
        }
    }

    private static boolean strictBoolean(String value) {
        Assert.assertTrue("Review the first-switch confirmation branch", "true".equals(value) || "false".equals(value));
        return Boolean.parseBoolean(value);
    }
}
