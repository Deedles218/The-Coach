package lib;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Allure;
import io.appium.java_client.remote.SupportsRotation;
import io.qameta.allure.Step;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
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
                    Throwable primaryFailure = null;
                    boolean reportingEnabled = isAllureReportingEnabled();
                    if (reportingEnabled) {
                        AllureReportSupport.prepareTestCase(description);
                    }
                    String scenarioStep = reportingEnabled
                            ? AllureReportSupport.startScenario(description)
                            : null;
                    try {
                        base.evaluate();
                    } catch (Throwable failure) {
                        primaryFailure = failure;
                        // Capture before teardown closes the Appium session. The
                        // previous TestWatcher ran after @After and saw driver=null.
                        if (reportingEnabled && !(failure instanceof AssumptionViolatedException)) {
                            captureFailureArtifacts(failure, description);
                        }
                        throw failure;
                    } finally {
                        if (reportingEnabled) {
                            AllureReportSupport.finishScenario(scenarioStep, primaryFailure);
                        }
                        try {
                            tearDown();
                        } catch (Throwable teardownFailure) {
                            if (primaryFailure == null) {
                                if (reportingEnabled) {
                                    AllureReportSupport.attachFailureContext(teardownFailure, description);
                                }
                                throw teardownFailure;
                            }
                            primaryFailure.addSuppressed(teardownFailure);
                        }
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

    protected boolean isAllureReportingEnabled() {
        return true;
    }

    /** Opt in only for a scenario known not to display credential inputs. */
    protected boolean isFailureScreenshotSafeWithoutPageSource() {
        return false;
    }

    protected String unsupportedPlatformMessage() {
        return "Test does not support platform " + Platform.getInstance().getPlatformVar();
    }

    protected void requireMobilePlatform() {
        Assert.assertTrue("The Coach requires an iOS or Android driver",
                Platform.getInstance().isIOS() || Platform.getInstance().isAndroid());
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

    @Step("Close and reopen The Coach application")
    protected void closeAndReopenCoachApplication() {
        Assert.assertTrue(
                "Closing and reopening the application requires an Appium mobile driver",
                driver instanceof InteractsWithApps
        );

        String applicationId;
        if (Platform.getInstance().isIOS()) {
            applicationId = Platform.getInstance().getIOSBundleId();
        } else if (Platform.getInstance().isAndroid()) {
            applicationId = Platform.getInstance().getAndroidAppPackage();
        } else {
            Assert.fail("Close/reopen is only supported for a native mobile platform");
            return;
        }

        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(applicationId);
        apps.activateApp(applicationId);
    }

    protected void openWikiWebPageForMobileWeb() {
        if (Platform.getInstance().isMw()) {
            driver.get("https://en.m.wikipedia.org");
        } else {
            System.out.println("Method openWikiWebPageForMobileWeb() does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    protected final void captureFailureArtifacts(Throwable throwable, Description description) {
        AllureReportSupport.attachFailureContext(throwable, description);
        String testName = safeArtifactName(description == null ? "unknown-test" : description.getMethodName());
        Path screenshotsDirectory = Paths.get("target", "screenshots");
        Path pageSourceDirectory = Paths.get("target", "page-source");
        Path logsDirectory = Paths.get("target", "appium-logs");
        if (driver == null) {
            addAllureAttachment(
                    "Failure artifact capture note",
                    "text/plain",
                    "Appium session was not available; screenshot, page source and driver logs could not be captured."
                            .getBytes(StandardCharsets.UTF_8),
                    "txt"
            );
            return;
        }

        try {
            Files.createDirectories(screenshotsDirectory);
            Files.createDirectories(pageSourceDirectory);
            Files.createDirectories(logsDirectory);
        } catch (Exception directoryFailure) {
            attachArtifactCaptureError("artifact directories", directoryFailure);
        }

        String rawPageSource = null;
        String pageSource = null;
        byte[] rawScreenshot = null;
        try {
            if (!(driver instanceof TakesScreenshot)) {
                throw new IllegalStateException("Driver does not support screenshots");
            }
            rawScreenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception screenshotFailure) {
            attachArtifactCaptureError("failure screenshot", screenshotFailure);
        }

        try {
            rawPageSource = driver.getPageSource();
            pageSource = TestData.sanitizeSensitiveData(rawPageSource);
            if (rawPageSource.contains("ENTER SECURITY CODE")) {
                // OTP may be exposed as four independent accessibility nodes.
                pageSource = pageSource.replaceAll("((?:name|label|value)=\")[0-9](\")", "$1*$2");
            }
            pageSource = pageSource.replace("<redacted-email>", "&lt;redacted-email&gt;")
                    .replace("<redacted-otp>", "&lt;redacted-otp&gt;");
            Path pageSourcePath = pageSourceDirectory.resolve(testName + ".xml");
            Files.write(pageSourcePath, pageSource.getBytes(StandardCharsets.UTF_8));
            addAllureAttachment(
                    "Failure page source",
                    "text/xml",
                    pageSource.getBytes(StandardCharsets.UTF_8),
                    "xml"
            );
        } catch (Exception sourceFailure) {
            attachArtifactCaptureError("page source", sourceFailure);
        }

        if (rawScreenshot != null) {
            try {
                captureFailureScreenshot(
                        rawScreenshot,
                        rawPageSource,
                        pageSource,
                        screenshotsDirectory,
                        testName
                );
            } catch (Exception screenshotFailure) {
                attachArtifactCaptureError("failure screenshot", screenshotFailure);
            }
        }

        try {
            String logs = collectDriverLogs();
            String failure = TestData.sanitizeSensitiveData(throwable == null ? "unknown failure" : throwable.toString());
            Path logPath = logsDirectory.resolve(testName + ".log");
            byte[] logBytes = (failure + "\n" + logs).getBytes(StandardCharsets.UTF_8);
            Files.write(logPath, logBytes);
            addAllureAttachment("Failure Appium/driver log", "text/plain", logBytes, "log");
        } catch (Exception logFailure) {
            attachArtifactCaptureError("Appium/driver logs", logFailure);
        }
    }

    private void captureFailureScreenshot(
            byte[] rawScreenshot,
            String rawPageSource,
            String sanitizedPageSource,
            Path screenshotsDirectory,
            String testName
    ) throws Exception {
        if (rawPageSource == null || sanitizedPageSource == null) {
            if (isFailureScreenshotSafeWithoutPageSource()) {
                Files.write(screenshotsDirectory.resolve(testName + ".png"), rawScreenshot);
                addAllureAttachment("Failure screenshot (page source unavailable)", "image/png", rawScreenshot, "png");
                return;
            }
            throw new IllegalStateException("Page source is unavailable for the credential safety check");
        }

        boolean credentialScreen = !rawPageSource.equals(sanitizedPageSource)
                || rawPageSource.contains("ENTER SECURITY CODE");
        byte[] screenshotBytes;
        String attachmentName;
        String fileName;
        if (!credentialScreen) {
            screenshotBytes = rawScreenshot;
            attachmentName = "Failure screenshot";
            fileName = testName + ".png";
        } else if ((rawPageSource.contains("ENTER THE MAIL THAT IS LINKED TO YOUR ACCOUNT")
                || rawPageSource.contains("ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS"))
                && !rawPageSource.contains("ENTER SECURITY CODE")) {
            screenshotBytes = LoginFailureScreenshot.redact(
                    rawScreenshot,
                    rawPageSource
            );
            attachmentName = "Failure screenshot (email and keyboard masked)";
            fileName = testName + "-redacted.png";
        } else {
            addAllureAttachment(
                    "Failure screenshot security note",
                    "text/plain",
                    "Screenshot was intentionally omitted because the current screen contains credentials that cannot be safely masked."
                            .getBytes(StandardCharsets.UTF_8),
                    "txt"
            );
            return;
        }

        Path screenshotPath = screenshotsDirectory.resolve(fileName);
        Files.write(screenshotPath, screenshotBytes);
        addAllureAttachment(attachmentName, "image/png", screenshotBytes, "png");
    }

    private void attachArtifactCaptureError(String artifact, Exception failure) {
        String message = "Could not capture " + artifact + ": "
                + failure.getClass().getSimpleName() + ": "
                + TestData.sanitizeSensitiveData(failure.getMessage());
        System.err.println(message);
        addAllureAttachment(
                "Failure artifact capture note",
                "text/plain",
                message.getBytes(StandardCharsets.UTF_8),
                "txt"
        );
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
        properties.setProperty("Platform", Platform.getInstance().getPlatformVar());
        properties.setProperty("Java", System.getProperty("java.version"));
        setEnvironmentCapability(properties, "Device", "appium:deviceName", "deviceName");
        setEnvironmentCapability(properties, "OS version", "appium:platformVersion", "platformVersion");
        setEnvironmentCapability(properties, "Automation driver", "appium:automationName", "automationName");
        if (Platform.getInstance().isIOS()) {
            properties.setProperty("Application", Platform.getInstance().getIOSBundleId());
        } else if (Platform.getInstance().isAndroid()) {
            properties.setProperty("Application", Platform.getInstance().getAndroidAppPackage());
        }
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

    private void setEnvironmentCapability(Properties properties, String propertyName, String... capabilityNames) {
        if (driver == null) {
            return;
        }
        for (String capabilityName : capabilityNames) {
            Object value = driver.getCapabilities().getCapability(capabilityName);
            if (value != null && !value.toString().trim().isEmpty()) {
                properties.setProperty(propertyName, value.toString());
                return;
            }
        }
    }
}
