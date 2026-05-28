package lib;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Platform {
    private static final String PLATFORM_IOS = "ios";
    private static final String PLATFORM_ANDROID = "android";
    private static final String PLATFORM_MOBILE_WEB = "mobile_web";
    private static final String DEFAULT_PLATFORM = PLATFORM_IOS;
    private static final String DEFAULT_APPIUM_URL = "http://127.0.0.1:4723/";
    private static final String DEFAULT_IOS_PLATFORM_VERSION = "26.5";
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

    private UiAutomator2Options getAndroidDesiredCapabilities()
    {
        UiAutomator2Options capabilities = new UiAutomator2Options();
        capabilities.setPlatformName("Android");
        capabilities.setDeviceName(this.getConfig("android.deviceName", "ANDROID_DEVICE_NAME", "AndroidTestDevice"));
        capabilities.setPlatformVersion(this.getConfig("android.platformVersion", "ANDROID_PLATFORM_VERSION", "8.0"));
        capabilities.setAppPackage(this.getConfig("android.appPackage", "ANDROID_APP_PACKAGE", "org.wikipedia"));
        capabilities.setAppActivity(this.getConfig("android.appActivity", "ANDROID_APP_ACTIVITY", ".main.MainActivity"));
        this.setCapabilityIfPresent(capabilities, "app", this.getConfig("android.app", "ANDROID_APP", "/Users/deedles/Desktop/JavaAppiumAutomation/apks/org.wikipedia.apk"));
        return capabilities;
    }
    private XCUITestOptions getIOSDesiredCapabilities()
    {
        XCUITestOptions capabilities = new XCUITestOptions();
        capabilities.setPlatformName("iOS");
        capabilities.setDeviceName(this.getConfig("ios.deviceName", "IOS_DEVICE_NAME", "iPhone Simulator"));
        capabilities.setBundleId(this.getConfig("ios.bundleId", "IOS_BUNDLE_ID", "com.vamapps.develop.The-Coach"));
        capabilities.setPlatformVersion(this.getConfig("ios.platformVersion", "IOS_PLATFORM_VERSION", DEFAULT_IOS_PLATFORM_VERSION));
        capabilities.setNewCommandTimeout(Duration.ofSeconds(Integer.parseInt(this.getConfig("appium.newCommandTimeout", "APPIUM_NEW_COMMAND_TIMEOUT", "120"))));
        this.setCapabilityIfPresent(capabilities, "udid", this.getConfig("ios.udid", "IOS_UDID", null));
        this.setBooleanCapabilityIfPresent(capabilities, "useNewWDA", this.getConfig("ios.useNewWDA", "IOS_USE_NEW_WDA", null));
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

    private void setCapabilityIfPresent(org.openqa.selenium.MutableCapabilities capabilities, String capabilityName, String value)
    {
        if (value != null && !value.trim().isEmpty()) {
            capabilities.setCapability(capabilityName, value);
        }
    }

    private void setBooleanCapabilityIfPresent(org.openqa.selenium.MutableCapabilities capabilities, String capabilityName, String value)
    {
        if (value != null && !value.trim().isEmpty()) {
            capabilities.setCapability(capabilityName, Boolean.parseBoolean(value));
        }
    }

}
