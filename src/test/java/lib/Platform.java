package lib;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Platform {
    private static final String PLATFORM_IOS = "ios";
    private static final String PLATFORM_ANDROID = "android";
    private static final String PLATFORM_MOBILE_WEB = "mobile_web";
    private static final String DEFAULT_PLATFORM = PLATFORM_IOS;
    // Appium 2 is started by ci-scripts/appium.sh with --base-path /.
    // Keep the URL configurable for older Appium 1/iOS environments.
    private static final String DEFAULT_APPIUM_URL = "http://127.0.0.1:4723/";
    private static final String DEFAULT_ANDROID_DEVICE_NAME = "TheCoach_API_30_ARM";
    private static final String DEFAULT_ANDROID_PLATFORM_VERSION = "11";
    private static final String DEFAULT_ANDROID_APP_PACKAGE = "com.vamapps.thecoach";
    private static final String DEFAULT_ANDROID_APP_ACTIVITY = "com.vamapps.thecoach.MainActivity";
    private static final String DEFAULT_IOS_DEVICE_NAME = "iPhone Daria";
    private static final String DEFAULT_IOS_PLATFORM_VERSION = "26.5";
    private static final String DEFAULT_IOS_BUNDLE_ID = "com.vamapps.preprod.The-Coach";
    private static final String DEFAULT_IOS_UDID = "00008150-00084CDE0CF0401C";
    private static Platform instance;
    private Platform(){}
    public static Platform getInstance()
    {
        if (instance == null) {
            instance= new Platform();
        }
        return instance;
    }

    public boolean isAndroid()
    {
        return isPlatform(PLATFORM_ANDROID);
    }
    public boolean isIOS()
    {
        return isPlatform(PLATFORM_IOS);
    }
    public boolean isMw()
    {
        return isPlatform(PLATFORM_MOBILE_WEB);
    }
    public RemoteWebDriver getDriver() throws Exception
    {
        URL URL= new URL(this.getAppiumUrl());
        if(this.isAndroid()) {
            return new AndroidDriver(URL, this.getAndroidDesiredCapabilities());
        }else if (this.isIOS()) {
            return new IOSDriver(URL, this.getIOSDesiredCapabilities());
        }else if (this.isMw()) {
            return new ChromeDriver(this.getMwChromeOptions());
        }else {
            throw new Exception("Cannot detect type of the Driver. Platform value " + this.getPlatformVar());
        }
    }

    private DesiredCapabilities getAndroidDesiredCapabilities()
    {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("deviceName", this.getConfig("android.deviceName", "ANDROID_DEVICE_NAME", DEFAULT_ANDROID_DEVICE_NAME));
        capabilities.setCapability("platformVersion", this.getConfig("android.platformVersion", "ANDROID_PLATFORM_VERSION", DEFAULT_ANDROID_PLATFORM_VERSION));
        capabilities.setCapability("automationName", "UiAutomator2");
        this.setCapabilityIfPresent(
                capabilities,
                "udid",
                this.getConfig("android.udid", "ANDROID_UDID", null)
        );
        capabilities.setCapability("appPackage", this.getAndroidAppPackage());
        capabilities.setCapability("appActivity", this.getAndroidAppActivity());
        this.setCapabilityIfPresent(capabilities, "app", this.getAndroidAppPath());
        capabilities.setCapability(
                "autoGrantPermissions",
                this.getBooleanConfig("android.autoGrantPermissions", "ANDROID_AUTO_GRANT_PERMISSIONS", true)
        );
        capabilities.setCapability(
                "noReset",
                this.getBooleanConfig("android.noReset", "ANDROID_NO_RESET", true)
        );
        capabilities.setCapability(
                "fullReset",
                this.getBooleanConfig("android.fullReset", "ANDROID_FULL_RESET", false)
        );
        return capabilities;
    }
    private DesiredCapabilities getIOSDesiredCapabilities()
    {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", "iOS");
        capabilities.setCapability("deviceName", this.getConfig("ios.deviceName", "IOS_DEVICE_NAME", DEFAULT_IOS_DEVICE_NAME));
        capabilities.setCapability("platformVersion", this.getConfig("ios.platformVersion", "IOS_PLATFORM_VERSION", DEFAULT_IOS_PLATFORM_VERSION));
        capabilities.setCapability("automationName","XCUITest");
        capabilities.setCapability("bundleId", this.getIOSBundleId());
        capabilities.setCapability("udid", this.getConfig("ios.udid", "IOS_UDID", DEFAULT_IOS_UDID));
        capabilities.setCapability("noReset", this.getBooleanConfig("ios.noReset", "IOS_NO_RESET", true));
        capabilities.setCapability("fullReset", this.getBooleanConfig("ios.fullReset", "IOS_FULL_RESET", false));
        this.setCapabilityIfPresent(capabilities, "app", this.getConfig("ios.app", "IOS_APP", null));
        this.setBooleanCapabilityIfPresent(capabilities, "autoAcceptAlerts", this.getConfig("ios.autoAcceptAlerts", "IOS_AUTO_ACCEPT_ALERTS", null));
        this.setBooleanCapabilityIfPresent(capabilities, "autoDismissAlerts", this.getConfig("ios.autoDismissAlerts", "IOS_AUTO_DISMISS_ALERTS", null));
        this.setCapabilityIfPresent(capabilities, "useNewWDA", this.getConfig("ios.useNewWDA", "IOS_USE_NEW_WDA", null));
        this.setCapabilityIfPresent(capabilities, "xcodeOrgId", this.getConfig("ios.xcodeOrgId", "IOS_XCODE_ORG_ID", null));
        this.setCapabilityIfPresent(capabilities, "xcodeSigningId", this.getConfig("ios.xcodeSigningId", "IOS_XCODE_SIGNING_ID", null));
        return capabilities;
    }
    private ChromeOptions getMwChromeOptions()
    {
       Map<String,Object> deviceMetrics = new HashMap<String,Object>();
       deviceMetrics.put("width",360);
        deviceMetrics.put("height",640);
        deviceMetrics.put("pixelRatio",3.0);

        Map<String,Object> mobileEmulation = new HashMap<String,Object>();
        mobileEmulation.put("deviceMetrics",deviceMetrics);
        mobileEmulation.put("userAgent", "Mozilla/5.0 (Linux; Android 4.2.1; en-us; Nexus 5 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("window-size=340,640");
        return chromeOptions;
    }

    private boolean isPlatform(String my_platform)
    {
        String platform = this.getPlatformVar();
        return my_platform.equals(platform);
    }

    public String getPlatformVar()
    {
        return this.getConfig("platform", "PLATFORM", DEFAULT_PLATFORM);
    }

    public String getIOSBundleId()
    {
        return this.getConfig("ios.bundleId", "IOS_BUNDLE_ID", DEFAULT_IOS_BUNDLE_ID);
    }

    public String getIOSAppPath()
    {
        return this.getConfig("ios.app", "IOS_APP", null);
    }

    public String getAndroidAppPath()
    {
        return this.getConfig("android.app", "ANDROID_APP", null);
    }

    public String getAndroidAppPackage()
    {
        return this.getConfig("android.appPackage", "ANDROID_APP_PACKAGE", DEFAULT_ANDROID_APP_PACKAGE);
    }

    public String getAndroidAppActivity()
    {
        return this.getConfig("android.appActivity", "ANDROID_APP_ACTIVITY", DEFAULT_ANDROID_APP_ACTIVITY);
    }

    public String getOnboardingGoal()
    {
        return this.getConfig(
                "onboarding.goal",
                "ONBOARDING_GOAL",
                "Boost overall health"
        );
    }

    public boolean isFreshAndroidInstallConfigured()
    {
        return this.isAndroid()
                && this.getBooleanConfig("android.fullReset", "ANDROID_FULL_RESET", false)
                && !this.getBooleanConfig("android.noReset", "ANDROID_NO_RESET", true)
                && this.getAndroidAppPath() != null
                && !this.getAndroidAppPath().trim().isEmpty();
    }

    public String getIOSUpdateAppPath()
    {
        return this.getConfig("ios.update.app", "IOS_UPDATE_APP", null);
    }

    public boolean isUpdateTestConfigured()
    {
        return !this.getBooleanConfig("ios.fullReset", "IOS_FULL_RESET", false)
                && this.getBooleanConfig("ios.noReset", "IOS_NO_RESET", true)
                && this.getIOSAppPath() != null
                && !this.getIOSAppPath().trim().isEmpty()
                && this.getIOSUpdateAppPath() != null
                && !this.getIOSUpdateAppPath().trim().isEmpty();
    }

    public boolean isCleanInstallConfigured()
    {
        return this.getBooleanConfig("ios.fullReset", "IOS_FULL_RESET", false)
                && !this.getBooleanConfig("ios.noReset", "IOS_NO_RESET", true);
    }

    public boolean isStoreKitSandboxEnabled()
    {
        return this.getBooleanConfig("storekit.sandbox", "STOREKIT_SANDBOX", false);
    }

    public boolean isStoreKitPurchaseAllowed()
    {
        return this.getBooleanConfig("storekit.allowPurchases", "STOREKIT_ALLOW_PURCHASES", false);
    }

    /**
     * The questionnaire is product-owned and its number of screens varies by
     * Firebase configuration. The purchase and clean-install suites therefore
     * receive the exact accessibility ids from CI instead of guessing XPath.
     */
    public String[] getIOSOnboardingStepLocators()
    {
        String configuredSteps = this.getConfig("ios.onboarding.steps", "IOS_ONBOARDING_STEPS", "");
        if (configuredSteps == null || configuredSteps.trim().isEmpty()) {
            return new String[0];
        }

        List<String> locators = new ArrayList<String>();
        for (String locator : configuredSteps.split(",")) {
            if (locator != null && !locator.trim().isEmpty()) {
                locators.add(locator.trim());
            }
        }
        return locators.toArray(new String[locators.size()]);
    }

    private String getAppiumUrl()
    {
        return this.getConfig("appium.url", "APPIUM_URL", DEFAULT_APPIUM_URL);
    }

    private String getConfig(String propertyName, String envName, String defaultValue)
    {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }

        return defaultValue;
    }

    private boolean getBooleanConfig(String propertyName, String envName, boolean defaultValue)
    {
        String value = this.getConfig(propertyName, envName, null);
        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private void setCapabilityIfPresent(DesiredCapabilities capabilities, String capabilityName, String value)
    {
        if (value != null && !value.trim().isEmpty()) {
            capabilities.setCapability(capabilityName, value);
        }
    }

    private void setBooleanCapabilityIfPresent(DesiredCapabilities capabilities, String capabilityName, String value)
    {
        if (value != null && !value.trim().isEmpty()) {
            capabilities.setCapability(capabilityName, Boolean.parseBoolean(value));
        }
    }

}
