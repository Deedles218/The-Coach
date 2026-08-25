package lib;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Allure;
import io.appium.java_client.remote.SupportsRotation;
import io.qameta.allure.Step;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.Properties;


public class CoreTestCase {
    protected RemoteWebDriver driver;

    @Rule
    public TestRule sessionLifecycle = new TestRule() {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        base.evaluate();
                    } catch (Throwable failure) {
                        // Capture before teardown closes the Appium session. The
                        // previous TestWatcher ran after @After and saw driver=null.
                        captureFailureArtifacts(failure, description);
                        throw failure;
                    } finally {
                        tearDown();
                    }
                }
            };
        }
    };

    @Before
    @Step("Run driver and session")
    public void setUp() throws Exception {
        Assume.assumeTrue(unsupportedPlatformMessage(), isPlatformSupported());
        TestData.validateTestIsolationMode();
        if (Platform.getInstance().isIOS()
                && "reinstall".equals(TestData.testIsolationMode())) {
            Assert.assertTrue(
                    "TEST_ISOLATION_MODE=reinstall requires -Dios.app=... or IOS_APP",
                    Platform.getInstance().getIOSAppPath() != null
                            && !Platform.getInstance().getIOSAppPath().trim().isEmpty()
            );
        }
        driver = createDriver();
        createAllurePropertyFile();
        rotateScreenPortrait();
    }

    @Step("Remove driver and session")
    public void tearDown() {
        if (driver != null) {
            try {
                cleanupSessionForNextTest();
                reinstallApplicationIfConfigured();
            } finally {
                try {
                    driver.quit();
                } finally {
                    driver = null;
                }
            }
        }
    }

    protected void cleanupSessionForNextTest() {
        if (driver == null
                || !Platform.getInstance().isIOS()
                || (!"logout".equals(TestData.testIsolationMode())
                && !"reinstall".equals(TestData.testIsolationMode()))) {
            return;
        }

        try {
            CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
            if (coachFlow != null) {
                coachFlow.cleanupForNextTest();
            }
        } catch (Exception cleanupFailure) {
            System.err.println(
                    "Could not normalize app session before teardown: " + cleanupFailure.getMessage()
            );
        }
    }

    protected void reinstallApplicationIfConfigured() {
        if (driver == null
                || !Platform.getInstance().isIOS()
                || !"reinstall".equals(TestData.testIsolationMode())
                || !(driver instanceof InteractsWithApps)) {
            return;
        }

        String bundleId = Platform.getInstance().getIOSBundleId();
        String appPath = Platform.getInstance().getIOSAppPath();
        InteractsWithApps apps = (InteractsWithApps) driver;
        if (apps.isAppInstalled(bundleId) && !apps.removeApp(bundleId)) {
            throw new IllegalStateException("Could not remove app before reinstall: " + bundleId);
        }
        apps.installApp(appPath);
    }

    protected boolean isPlatformSupported() {
        return true;
    }

    protected String unsupportedPlatformMessage() {
        return "Test does not support platform " + Platform.getInstance().getPlatformVar();
    }

    protected void requireIOSPlatform() {
        Assert.assertTrue(
                "This The Coach mobile suite is iOS-only; run it with -Dplatform=ios",
                Platform.getInstance().isIOS()
        );
    }

    protected RemoteWebDriver createDriver() throws Exception {
        return Platform.getInstance().getDriver();
    }

    @Step("rotate Screen to Portrait mode")
    protected void rotateScreenPortrait() {
        if (driver instanceof SupportsRotation) {
            SupportsRotation driver = (SupportsRotation) this.driver;
            driver.rotate(ScreenOrientation.PORTRAIT);
        } else {
            System.out.println("Method rotateScreenPortrait() does nothing for platform " + Platform.getInstance().getPlatformVar());
        }

    }
    @Step("rotate Screen to Landscape mode")
    protected void rotateScreenLandscape() {
        if (driver instanceof SupportsRotation) {
            SupportsRotation driver = (SupportsRotation) this.driver;
            driver.rotate(ScreenOrientation.LANDSCAPE);
        } else {
            System.out.println("Method rotateScreenLandscape() does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    @Step("Send mobile app to background")
    protected void backgroundApp(int seconds) {
        if (driver instanceof InteractsWithApps) {
            InteractsWithApps driver = (InteractsWithApps) this.driver;
            driver.runAppInBackground(Duration.ofSeconds(seconds));
        } else {
            System.out.println("Method backgroundApp() does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }
    protected void openWikiWebPageForMobileWeb() {
        if (Platform.getInstance().isMw()) {
            driver.get("https://en.m.wikipedia.org");
        } else {
            System.out.println("Method openWikiWebPageForMobileWeb() does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    private void captureFailureArtifacts(Throwable throwable, Description description) {
        if (driver == null) {
            return;
        }

        String testName = safeArtifactName(description == null ? "unknown-test" : description.getMethodName());
        Path screenshotsDirectory = Paths.get("target", "screenshots");
        Path pageSourceDirectory = Paths.get("target", "page-source");
        Path logsDirectory = Paths.get("target", "appium-logs");
        try {
            Files.createDirectories(screenshotsDirectory);
            Files.createDirectories(pageSourceDirectory);
            Files.createDirectories(logsDirectory);

            if (driver instanceof TakesScreenshot) {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path screenshotPath = screenshotsDirectory.resolve(testName + ".png");
                Files.copy(screenshot.toPath(), screenshotPath, StandardCopyOption.REPLACE_EXISTING);
                byte[] screenshotBytes = Files.readAllBytes(screenshotPath);
                addAllureAttachment("Failure screenshot", "image/png", screenshotBytes, "png");
            }

            String pageSource = TestData.sanitizeSensitiveData(driver.getPageSource());
            Path pageSourcePath = pageSourceDirectory.resolve(testName + ".xml");
            Files.write(pageSourcePath, pageSource.getBytes(StandardCharsets.UTF_8));
            addAllureAttachment(
                    "Failure page source",
                    "text/xml",
                    pageSource.getBytes(StandardCharsets.UTF_8),
                    "xml"
            );

            String logs = collectDriverLogs();
            String failure = TestData.sanitizeSensitiveData(throwable == null ? "unknown failure" : throwable.toString());
            Path logPath = logsDirectory.resolve(testName + ".log");
            byte[] logBytes = (failure + "\n" + logs).getBytes(StandardCharsets.UTF_8);
            Files.write(logPath, logBytes);
            addAllureAttachment("Failure Appium/driver log", "text/plain", logBytes, "log");
        } catch (Exception artifactFailure) {
            System.err.println("Cannot capture failure artifacts: " + artifactFailure.getMessage());
        }
    }

    private void addAllureAttachment(
            String name,
            String contentType,
            byte[] content,
            String fileExtension
    ) {
        Allure.addAttachment(
                name,
                contentType,
                new ByteArrayInputStream(content),
                fileExtension
        );
    }

    private String collectDriverLogs() {
        StringBuilder logs = new StringBuilder();
        try {
            Set<String> logTypes = driver.manage().logs().getAvailableLogTypes();
            for (String logType : logTypes) {
                // UiAutomator2 exposes a very large bugreport stream. On the
                // local Android emulator it can remain open indefinitely after
                // a failed command, preventing Surefire from finishing. The
                // useful failure context is already covered by logcat/server;
                // keep bugreport collection for the existing non-Android flow.
                if (Platform.getInstance().isAndroid() && "bugreport".equalsIgnoreCase(logType)) {
                    continue;
                }
                logs.append("[log type: ").append(logType).append("]\n");
                logs.append(driver.manage().logs().get(logType).toString()).append("\n");
            }
        } catch (Exception logFailure) {
            logs.append("Driver log collection unavailable: ")
                    .append(logFailure.getClass().getSimpleName())
                    .append("\n");
        }
        return TestData.sanitizeSensitiveData(logs.toString());
    }

    private String safeArtifactName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown-test";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void createAllurePropertyFile() {
        String resultsDirectory = System.getProperty("allure.results.directory");
        if (resultsDirectory == null || resultsDirectory.trim().isEmpty()) {
            return;
        }

        File directory = new File(resultsDirectory);
        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("Cannot create Allure results directory: " + directory.getAbsolutePath());
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("Environment", Platform.getInstance().getPlatformVar());
        String fixtureId = TestData.configuredFixtureId();
        if (fixtureId != null) {
            properties.setProperty("Smoke fixture", fixtureId);
        }
        String fixtureEnvironment = TestData.configuredFixtureEnvironment();
        if (fixtureEnvironment != null) {
            properties.setProperty("Fixture environment", fixtureEnvironment);
        }
        properties.setProperty("Test isolation", TestData.testIsolationMode());
        File environmentFile = new File(directory, "environment.properties");
        try (FileOutputStream output = new FileOutputStream(environmentFile)) {
            properties.store(output, "Test environment");
        } catch (IOException e) {
            System.err.println("Cannot write Allure environment file: " + e.getMessage());
        }
    }
}
