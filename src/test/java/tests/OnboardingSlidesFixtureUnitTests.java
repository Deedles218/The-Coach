package tests;

import lib.ui.android.AndroidOnboardingSlidesPageObject;
import lib.ui.ios.iOSOnboardingSlidesPageObject;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class OnboardingSlidesFixtureUnitTests {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final Map<String, String> previous = new HashMap<>();
    private void property(String name, String value) {
        if (!previous.containsKey(name)) previous.put(name, System.getProperty(name));
        System.setProperty(name, value);
    }
    @After public void restoreProperties() {
        for (Map.Entry<String, String> entry : previous.entrySet()) {
            if (entry.getValue() == null) System.clearProperty(entry.getKey());
            else System.setProperty(entry.getKey(), entry.getValue());
        }
    }
    private void fixture(String identity, String extra) throws Exception {
        java.io.File file = folder.newFile();
        String value = identity + "\ngoal=BOOST OVERALL HEALTH\ncount=2\n"
                + "1.header=First\n1.buttonText=NEXT\n1.imageUrl=https://example.invalid/one.png\n"
                + "2.header=Second\n2.buttonText=GOT IT\n2.imageUrl=https://example.invalid/two.png\n" + extra;
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
        property("onboarding.slides.fixture", file.getAbsolutePath());
    }
    @Test public void androidAcceptsOwnFixtureAndReadsConfiguredOrder() throws Exception {
        property("platform", "android");
        property("android.appPackage", "com.vamapps.thecoach");
        fixture("appPackage=com.vamapps.thecoach", "");
        AndroidOnboardingSlidesPageObject slides = new AndroidOnboardingSlidesPageObject(null);
        Assert.assertArrayEquals(new String[]{"First", "Second"}, slides.headers());
    }
    @Test(expected = AssertionError.class) public void androidRejectsIosFixture() throws Exception {
        property("platform", "android");
        fixture("bundleId=com.vamapps.The-Coach", "");
        new AndroidOnboardingSlidesPageObject(null);
    }
    @Test public void iosStillAcceptsExistingFixtureFormat() throws Exception {
        property("platform", "ios");
        property("ios.bundleId", "com.vamapps.The-Coach");
        fixture("bundleId=com.vamapps.The-Coach", "");
        Assert.assertEquals(2, new iOSOnboardingSlidesPageObject(null).count());
    }
    @Test(expected = AssertionError.class) public void invalidActiveConfigIsRejectedBeforeDriverUse() throws Exception {
        property("platform", "android");
        property("android.appPackage", "com.vamapps.thecoach");
        fixture("appPackage=com.vamapps.thecoach", "validationError=missing goal-specific orders\n");
        new AndroidOnboardingSlidesPageObject(null);
    }
    @Test(expected = AssertionError.class) public void gifCannotSilentlyLoseMotionRequirement() throws Exception {
        property("platform", "android");
        property("android.appPackage", "com.vamapps.thecoach");
        fixture("appPackage=com.vamapps.thecoach", "1.imageUrl=https://example.invalid/animated.gif\n");
        new AndroidOnboardingSlidesPageObject(null);
    }
}
