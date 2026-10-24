package tests;

import lib.Platform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.lang.reflect.Method;

public class AndroidAccessibilityConfigurationUnitTests {
    private static final String PROPERTY = "android.disableSuppressAccessibilityService";
    private String original;

    @Before public void remember() { original = System.getProperty(PROPERTY); }
    @After public void restore() {
        if (original == null) System.clearProperty(PROPERTY);
        else System.setProperty(PROPERTY, original);
    }
    private DesiredCapabilities capabilities(String platform) throws Exception {
        Method method = Platform.class.getDeclaredMethod("get" + platform + "DesiredCapabilities");
        method.setAccessible(true);
        return (DesiredCapabilities) method.invoke(Platform.getInstance());
    }

    @Test public void unsetPropertyUsesEnvironmentOrUnchangedDriverDefault() throws Exception {
        System.clearProperty(PROPERTY);
        String configured = System.getenv("ANDROID_DISABLE_SUPPRESS_ACCESSIBILITY_SERVICE");
        Object actual = capabilities("Android").getCapability("disableSuppressAccessibilityService");
        Assert.assertEquals(configured == null || configured.trim().isEmpty()
                ? null : Boolean.parseBoolean(configured), actual);
    }

    @Test public void explicitOptInPreservesConfiguredAccessibilityServices() throws Exception {
        System.setProperty(PROPERTY, "true");
        Assert.assertEquals(Boolean.TRUE, capabilities("Android").getCapability("disableSuppressAccessibilityService"));
    }

    @Test public void explicitFalseKeepsSuppressionEnabled() throws Exception {
        System.setProperty(PROPERTY, "false");
        Assert.assertEquals(Boolean.FALSE, capabilities("Android").getCapability("disableSuppressAccessibilityService"));
    }

    @Test public void androidOptInDoesNotChangeIOSCapabilities() throws Exception {
        System.setProperty(PROPERTY, "true");
        Assert.assertNull(capabilities("IOS").getCapability("disableSuppressAccessibilityService"));
    }
}
