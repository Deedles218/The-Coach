package lib;

import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Bounded adb operations on the explicitly selected test device. */
public final class AndroidDevice {
    private AndroidDevice() { }
    public static String serial() {
        String serial = System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        Assert.assertTrue("An explicit test emulator is required", serial != null && serial.matches("emulator-[0-9]+"));
        return serial;
    }
    /** Readers verify the exact debug APK and SDK UID before reading app-private evidence. */
    public static String appPrivateEvidenceSerial() {
        return appPrivateEvidenceSerial(System.getenv("COACH_ANDROID_EVIDENCE_PACKAGE"));
    }
    private static String appPrivateEvidenceSerial(String debugPackage) {
        if(debugPackage==null||debugPackage.isEmpty())return serial();
        Assert.assertEquals("Debug evidence package differs from the app under test",
                Platform.getInstance().getAndroidAppPackage(),debugPackage);
        return uiSettingsSerial();
    }
    public static String adb(String... arguments) throws Exception {
        return adbForSerial(serial(), arguments);
    }

    public static String emailRegressionSerial() {
        String selected = System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        if (selected != null && selected.matches("emulator-[0-9]+")) return selected;
        Assert.assertTrue("Use an explicit disposable emulator or identify the approved USB test installation",
                selected != null && selected.matches("[A-Za-z0-9_-]+")
                        && selected.equals(System.getProperty("coach.emailRelease.dedicatedDevice")));
        return selected;
    }

    public static String adbForEmailRegression(String selected, String... arguments) throws Exception {
        Assert.assertEquals("ADB must use the selected email regression device", emailRegressionSerial(), selected);
        if (!selected.matches("emulator-[0-9]+")) {
            Assert.assertEquals("The selected regression device must be the connected USB test device",
                    selected, runAdb(Collections.singletonList("-d"), "get-serialno").trim());
        }
        return adbForSerial(selected, arguments);
    }

    public static String uiSettingsSerial() {
        String selected = System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        Assert.assertTrue("Select an emulator or opt in to UI settings on an explicit USB device",
                selected != null && (selected.matches("emulator-[0-9]+")
                        || (Boolean.getBoolean("coach.android.physicalUiSettings") && selected.matches("[A-Za-z0-9_-]+"))));
        return selected;
    }

    static String uiSettingsSerial(RemoteWebDriver driver) throws Exception {
        String selected = uiSettingsSerial();
        if (selected.matches("emulator-[0-9]+")) return selected;
        Object actual = driver.getCapabilities().getCapability("appium:udid");
        if (actual == null) actual = driver.getCapabilities().getCapability("udid");
        Assert.assertTrue("UI settings must target the same Android device as Appium", selected.equals(actual));
        String usb = runAdb(Collections.singletonList("-d"), "get-serialno").trim();
        Assert.assertTrue("UI settings require the selected USB device", selected.equals(usb));
        return selected;
    }

    static String adbForSerial(String serial, String... arguments) throws Exception {
        return runAdb(Arrays.asList("-s", serial), arguments);
    }

    private static String runAdb(List<String> target, String... arguments) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(System.getenv("ADB") == null ? "adb" : System.getenv("ADB"));
        command.addAll(target);
        command.addAll(Arrays.asList(arguments));
        Path log = Files.createTempFile("coach-adb-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            Assert.assertTrue("adb operation timed out", process.waitFor(90, TimeUnit.SECONDS));
            String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            Assert.assertEquals("adb operation failed: " + TestData.sanitizeSensitiveData(output), 0, process.exitValue());
            return output;
        } finally {
            if(process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
    }
    public static String version() throws Exception {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("versionName=([^\\s]+)")
                .matcher(adb("shell", "dumpsys", "package", Platform.getInstance().getAndroidAppPackage()));
        Assert.assertTrue("Installed application version is unavailable", m.find());
        return m.group(1);
    }
    public static long versionCode() throws Exception {
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("versionCode=([0-9]+)")
                .matcher(adb("shell","dumpsys","package",Platform.getInstance().getAndroidAppPackage()));
        Assert.assertTrue("Installed version code is unavailable",m.find());
        return Long.parseLong(m.group(1));
    }

}
