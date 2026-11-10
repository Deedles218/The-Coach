package lib.ui.ios;

import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.appmanagement.ApplicationState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lib.Platform;
import org.junit.Assert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.ByteArrayInputStream;
import java.time.Duration;

/** Observes startup without automatically reactivating a crashed application. */
public class iOSStartupStabilityPageObject extends iOSCoachFlowPageObject {
    public iOSStartupStabilityPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Cold launch {attempt}: observe process and startup screens")
    public void coldLaunchAndVerify(int attempt, int observationSeconds) {
        Assert.assertTrue("Startup observation must last between 5 and 60 seconds",
                observationSeconds >= 5 && observationSeconds <= 60);
        InteractsWithApps apps = (InteractsWithApps) driver;
        String bundle = Platform.getInstance().getIOSBundleId();
        apps.terminateApp(bundle);
        Assert.assertEquals("App was not terminated before cold launch",
                ApplicationState.NOT_RUNNING, apps.queryAppState(bundle));
        apps.activateApp(bundle);
        long started = System.nanoTime();
        attachScreen("Cold launch " + attempt + " initial screen");
        int[] polls = {0};
        createWait(observationSeconds + 15).pollingEvery(Duration.ofSeconds(1))
                .withMessage("Startup observation did not complete")
                .until(ignored -> {
                    Assert.assertEquals("Coach left the foreground during cold launch " + attempt,
                            ApplicationState.RUNNING_IN_FOREGROUND, apps.queryAppState(bundle));
                    assertNoCrashDialog();
                    // Requesting the accessibility tree also verifies UI responsiveness.
                    Assert.assertFalse("Startup returned an empty accessibility tree",
                            driver.getPageSource().trim().isEmpty());
                    polls[0]++;
                    return Duration.ofNanos(System.nanoTime() - started).getSeconds() >= observationSeconds;
                });
        Allure.addAttachment("Cold launch " + attempt + " observation",
                "Foreground and responsive for at least " + observationSeconds + " seconds; polls=" + polls[0]);
        attachScreen("Cold launch " + attempt + " before conditional popup handling");
        // Existing handlers close only known optional screens after the crash observation.
        waitForAuthorizedDashboard();
        closeConnectEmailPromptIfPresent();
        openToday();
        waitForElementPresent("id:DailyPlanItem", "Today content is unavailable after cold launch", 15);
        Assert.assertEquals(ApplicationState.RUNNING_IN_FOREGROUND, apps.queryAppState(bundle));
        assertNoCrashDialog();
        attachScreen("Cold launch " + attempt + " Today ready");
    }

    private void assertNoCrashDialog() {
        try {
            String alert = driver.switchTo().alert().getText();
            Assert.assertFalse("TestFlight reported a process crash during startup",
                    alert.contains("Crashed") && alert.contains("share additional information"));
        } catch (NoAlertPresentException absent) {
            // No alert is the expected state; other driver failures propagate.
        }
    }

    private void attachScreen(String title) {
        Allure.addAttachment(title, "image/png",
                new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)), "png");
        Allure.addAttachment(title + " source", "application/xml", driver.getPageSource(), "xml");
    }
}
