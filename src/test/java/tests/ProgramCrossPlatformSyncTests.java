package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.MobileCaseFixture;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ProgramCrossPlatformProbe;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import lib.ui.ios.IOSMyProgramsPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.json.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Epic("COA-9583 Program activation")
@Feature("Preprod iOS/Android program synchronization")
public class ProgramCrossPlatformSyncTests extends CoreTestCase {
    @Before @Override public void setUp() throws Exception {
        requireIOSPlatform();
        Assert.assertTrue("Use preserved iOS preprod", Platform.getInstance().isTestFlightAttachConfigured()
                && Platform.getInstance().getIOSBundleId().startsWith("com.vamapps.preprod."));
        PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
    }
    @Override protected void cleanupSessionForNextTest() { }

    @Test @Issue("COA-9795")
    public void activeProgramSynchronizesFromIOSToAndroidAndBackWithoutChangingProgress() throws Exception {
        MobileCaseFixture f = MobileCaseFixture.load("COA-9795");
        ProgramSelectionApi.assertDedicatedProfile("COA-9795");
        ProgramSelectionState before = ProgramSelectionApi.read();
        String main = f.required("main.id"), secondary = f.required("switch.id");
        before.assertRoles(main, ProgramOnboardingScenarios.ids(f, "secondary.ids"));
        before.assertActive(main);
        Assert.assertTrue("Cross-platform fixture must have the selected Secondary", before.secondaryIds().contains(secondary));
        Assert.assertNotEquals("Cross-platform fixture needs different progress percentages",
                ((Number) before.program(main).get("progress_percent")).doubleValue(),
                ((Number) before.program(secondary).get("progress_percent")).doubleValue(), .01);
        String expectedPackage = Platform.getInstance().getIOSBundleId().endsWith("for-her")
                ? "com.vamapps.thecoach.forher" : "com.vamapps.thecoach";
        Assert.assertEquals("Use the same Him/Her variant on both devices", expectedPackage, f.required("android.package"));
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        today.openTodayTab();
        today.openProgramSelector();
        today.assertMatchesState(before);
        today.selectSavedProgram((String) before.program(secondary).get("title"), ProgramMembershipScenarios.bool(f, "confirmation"));
        ProgramSelectionState switched = ProgramSelectionApi.read();
        switched.assertSameRoles(before);
        switched.assertSameProgress(before);
        switched.assertActive(secondary);
        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(Platform.getInstance().getIOSBundleId());
        Path folder = Files.createTempDirectory(Paths.get("target"), "program-cross-platform-");
        Path baseline = folder.resolve("baseline.json"), result = folder.resolve("android-result.json");
        Files.write(baseline, new Json().toJson(before.raw()).getBytes(StandardCharsets.UTF_8));
        List<String> command = new ArrayList<>(Arrays.asList(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dplatform=android", "-Dandroid.noReset=true", "-Dandroid.fullReset=false",
                "-Dandroid.udid=" + f.required("android.udid"), "-Dandroid.appPackage=" + expectedPackage,
                "-Dandroid.deviceName=" + f.required("android.deviceName"),
                "-Dandroid.platformVersion=" + f.required("android.platformVersion"),
                "-Dappium.url=" + f.required("android.appiumUrl"),
                "-Dcoach.mobileCases.fixture=" + f.required("android.fixture"),
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                ProgramCrossPlatformProbe.class.getName(), baseline.toString(), result.toString()));
        ProcessBuilder child = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(folder.resolve("android.log").toFile());
        child.environment().remove("ANDROID_APP");
        child.environment().put("COACH_PROGRAM_SELECTION_BACKEND_HASH", (String) before.raw().get("backend_identity"));
        child.environment().put("COACH_PROGRAM_SELECTION_ANDROID_APK_SHA256", f.required("android.apk.sha256"));
        Process android = child.start();
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(12);
        try {
            while (!android.waitFor(10, TimeUnit.SECONDS)) {
                Assert.assertTrue("Android sync probe timed out; see " + folder, System.nanoTime() < deadline);
                driver.manage().timeouts().getImplicitWaitTimeout();
            }
            Assert.assertEquals("Android sync phase failed; see " + folder, 0, android.exitValue());
            ProgramSelectionState androidResult = new ProgramSelectionState(new Json().toType(
                    new String(Files.readAllBytes(result), StandardCharsets.UTF_8), Map.class));
            androidResult.assertSameRoles(before);
            androidResult.assertSameProgress(before);
            androidResult.assertActive(main);
            apps.activateApp(Platform.getInstance().getIOSBundleId());
            today.openTodayTab();
            Assert.assertEquals("iOS did not load Android Active Program", before.main().get("title"), today.getActiveProgramName());
            today.waitForElementVisible(f.selector("main.content.ready"), "iOS Main cards did not synchronize", 15);
            ProgramSelectionState returned = ProgramSelectionApi.read();
            returned.assertSameRoles(before);
            returned.assertSameProgress(before);
            returned.assertActive(main);
            today.openProgramSelector();
            today.assertMatchesState(returned);
            today.closeProgramSelector();
        } finally {
            if (android.isAlive()) android.destroyForcibly();
        }
    }
}
