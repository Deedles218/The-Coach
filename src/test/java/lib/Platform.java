package lib;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Platform {
    private static final String PLATFORM_IOS = "ios";
    private static final String PLATFORM_ANDROID = "android";
    private static final String PLATFORM_MOBILE_WEB = "mobile_web";
    private static final String DEFAULT_PLATFORM = PLATFORM_IOS;
    private static final String DEFAULT_APPIUM_URL = "http://127.0.0.1:4723/wd/hub";
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
        capabilities.setCapability("deviceName", this.getConfig("android.deviceName", "ANDROID_DEVICE_NAME", "AndroidTestDevice"));
        capabilities.setCapability("platformVersion", this.getConfig("android.platformVersion", "ANDROID_PLATFORM_VERSION", "8.0"));
        capabilities.setCapability("automationName", "Appium");
        capabilities.setCapability("appPackage", this.getConfig("android.appPackage", "ANDROID_APP_PACKAGE", "org.wikipedia"));
        capabilities.setCapability("appActivity", this.getConfig("android.appActivity", "ANDROID_APP_ACTIVITY", ".main.MainActivity"));
        this.setCapabilityIfPresent(capabilities, "app", this.getConfig("android.app", "ANDROID_APP", "/Users/deedles/Desktop/JavaAppiumAutomation/apks/org.wikipedia.apk"));
        return capabilities;
    }
    private DesiredCapabilities getIOSDesiredCapabilities()
    {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", "iOS");
        capabilities.setCapability("deviceName", this.getConfig("ios.deviceName", "IOS_DEVICE_NAME", DEFAULT_IOS_DEVICE_NAME));
        capabilities.setCapability("platformVersion", this.getConfig("ios.platformVersion", "IOS_PLATFORM_VERSION", DEFAULT_IOS_PLATFORM_VERSION));
        capabilities.setCapability("automationName","XCUITest");
        capabilities.setCapability("bundleId", this.getConfig("ios.bundleId", "IOS_BUNDLE_ID", DEFAULT_IOS_BUNDLE_ID));
        capabilities.setCapability("udid", this.getConfig("ios.udid", "IOS_UDID", DEFAULT_IOS_UDID));
        capabilities.setCapability("noReset", this.getBooleanConfig("ios.noReset", "IOS_NO_RESET", true));
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

}
