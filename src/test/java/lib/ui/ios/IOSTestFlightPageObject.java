package lib.ui.ios;

import io.appium.java_client.InteractsWithApps;
import lib.UpdateEvidence;
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IOSTestFlightPageObject extends MainPageObject {
    private static final String TEST_FLIGHT = "com.apple.TestFlight";
    private static final String REQUEST_FAILED = "xpath://XCUIElementTypeAlert[@visible='true' and "
            + "@name='Your request couldn’t be completed. Try again.']";
    private static final Pattern BUILD = Pattern.compile("([0-9]+(?:\\.[0-9]+){1,2}) \\(([0-9]+(?:\\.[0-9]+){0,2})\\)");
    private final InteractsWithApps apps;
    private final String bundle;
    private final String storeId;
    private final String release;
    private final Map<String, String> expectedBuild;

    public IOSTestFlightPageObject(RemoteWebDriver driver, String bundle, String release) {
        super(driver);
        String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertTrue("TestFlight installation needs an explicit physical test iPhone",
                udid != null && udid.matches("(?i)[0-9a-f]{8}-[0-9a-f]{16}"));
        UpdateEvidence.assertAppiumDevice(driver, udid);
        Assert.assertTrue("TestFlight installation requires an Appium application-management driver",
                driver instanceof InteractsWithApps);
        this.apps = (InteractsWithApps) driver;
        this.bundle = bundle;
        this.storeId = appStoreId(bundle);
        this.release = release;
        this.expectedBuild = parseBuild(release);
    }

    public Map<String, String> expectedBuild() {
        return new HashMap<>(expectedBuild);
    }

    public void verifyOfferedBuild() {
        try {
            waitForOfferedBuild();
        } catch (TimeoutException firstAttempt) {
            if (!isElementVisible(REQUEST_FAILED) || !apps.terminateApp(TEST_FLIGHT)) throw firstAttempt;
            try {
                waitForOfferedBuild();
            } catch (TimeoutException retry) {
                retry.addSuppressed(firstAttempt);
                throw retry;
            }
        }
    }

    private void waitForOfferedBuild() {
        apps.activateApp(TEST_FLIGHT);
        String appsTab = "id:Apps";
        if (!isElementVisible("id:AppList.ScrollView") && isElementVisible(appsTab)) {
            waitForElementAndClick(appsTab, "Cannot open TestFlight Apps", 10);
        }
        waitForElementVisible(offerLocator(storeId, release, null),
                "TestFlight does not list the requested Coach build: " + release, 15);
    }

    public void install(boolean update) {
        verifyOfferedBuild();
        String action = update ? "Update" : "Install";
        waitForElementAndClick(offerLocator(storeId, release, action),
                "TestFlight does not offer " + action + " for Coach " + release, 25);
        new FluentWait<>(apps).withTimeout(Duration.ofMinutes(3)).pollingEvery(Duration.ofSeconds(2))
                .withMessage("TestFlight did not install Coach " + release)
                .until(ignored -> installedExpectedBuild());
    }

    private boolean installedExpectedBuild() {
        if (!apps.isAppInstalled(bundle)) return false;
        try {
            return expectedBuild.equals(UpdateEvidence.readInstalledIOSBuild(driver));
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot verify installed TestFlight build", failure);
        }
    }

    static Map<String, String> parseBuild(String release) {
        Matcher match = BUILD.matcher(release);
        Assert.assertTrue("TestFlight build must be configured as version (build)", match.matches());
        Map<String, String> result = new HashMap<>();
        result.put("version", match.group(1));
        result.put("build", match.group(2));
        return result;
    }

    static String offerLocator(String storeId, String release, String action) {
        String row = "//XCUIElementTypeOther[@name='" + storeId + "' and @visible='true']"
                + "[XCUIElementTypeStaticText[@visible='true' and (@label='" + release
                + "' or starts-with(@label,'" + release + " · '))]]";
        String button = "/XCUIElementTypeButton[@name='AppList.InstallButton." + storeId
                + "' and @visible='true' and @enabled='true']";
        String actionLabel = action == null ? "(@label='Install' or @label='Update' or @label='Open')"
                : "@label='" + action + "'";
        return "xpath:" + row + button + "[descendant::XCUIElementTypeStaticText[@visible='true' and " + actionLabel + "]]";
    }

    private static String appStoreId(String bundle) {
        if ("com.vamapps.The-Coach".equals(bundle)) return "1497286047";
        if ("com.vamapps.The-Coach-for-her".equals(bundle)) return "6747802021";
        throw new IllegalArgumentException("Unsupported TestFlight production Coach bundle: " + bundle);
    }
}
