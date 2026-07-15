package lib;

import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.remote.SupportsRotation;
import io.qameta.allure.Step;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;


public class CoreTestCase {
    protected RemoteWebDriver driver;

    @Before
    @Step("Run driver and session")
    public void setUp() throws Exception {
        Assume.assumeTrue(unsupportedPlatformMessage(), isPlatformSupported());
        driver = createDriver();
        createAllurePropertyFile();
        rotateScreenPortrait();
    }

    @After
    @Step("Remove driver and session")
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }

    protected boolean isPlatformSupported() {
        return true;
    }

    protected String unsupportedPlatformMessage() {
        return "Test does not support platform " + Platform.getInstance().getPlatformVar();
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
        File environmentFile = new File(directory, "environment.properties");
        try (FileOutputStream output = new FileOutputStream(environmentFile)) {
            properties.store(output, "Test environment");
        } catch (IOException e) {
            System.err.println("Cannot write Allure environment file: " + e.getMessage());
        }
    }
}
