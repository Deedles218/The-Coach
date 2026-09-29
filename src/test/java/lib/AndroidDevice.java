package lib;

import org.junit.Assert;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Bounded adb operations, always scoped to the selected test emulator. */
public final class AndroidDevice {
    private AndroidDevice() { }
    public static String serial() {
        String serial = System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        Assert.assertTrue("An explicit test emulator is required", serial != null && serial.matches("emulator-[0-9]+"));
        return serial;
    }
    public static String adb(String... arguments) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(System.getenv("ADB") == null ? "adb" : System.getenv("ADB"));
        command.add("-s"); command.add(serial()); command.addAll(Arrays.asList(arguments));
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
                .matcher(adb("shell", "dumpsys", "package", "com.vamapps.thecoach"));
        Assert.assertTrue("Installed application version is unavailable", m.find());
        return m.group(1);
    }
    public static long versionCode() throws Exception {
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("versionCode=([0-9]+)")
                .matcher(adb("shell","dumpsys","package","com.vamapps.thecoach"));
        Assert.assertTrue("Installed version code is unavailable",m.find());
        return Long.parseLong(m.group(1));
    }

}
