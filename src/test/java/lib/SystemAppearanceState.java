package lib;

import org.openqa.selenium.remote.RemoteWebDriver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scoped OS appearance change on an explicitly selected test device. */
public final class SystemAppearanceState implements AutoCloseable {
    private final boolean android;
    private final String udid;
    private final String original;

    private SystemAppearanceState(boolean android, String udid, String original) {
        this.android = android;
        this.udid = udid;
        this.original = original;
    }

    public static SystemAppearanceState capture(RemoteWebDriver driver) throws Exception {
        boolean android = Platform.getInstance().isAndroid();
        String udid = android ? AndroidDevice.uiSettingsSerial(driver) : System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        if (!android && (udid == null || !udid.matches("[a-fA-F0-9-]{36}"))) {
            throw new IllegalStateException("Appearance checks require an explicit iOS Simulator UDID");
        }
        return new SystemAppearanceState(android, udid, read(android, udid));
    }

    public void setDark(boolean dark) throws Exception {
        write(android ? (dark ? "yes" : "no") : (dark ? "dark" : "light"));
    }

    @Override public void close() throws Exception { write(original); }

    private void write(String value) throws Exception {
        if (android) AndroidDevice.adbForSerial(udid, "shell", "cmd", "uimode", "night", value);
        else simctl(udid, value);
        if (!value.equals(read(android, udid))) throw new IllegalStateException("OS appearance setting did not take effect");
    }

    private static String read(boolean android, String udid) throws Exception {
        if (!android) {
            String value = simctl(udid).trim().toLowerCase(Locale.ROOT);
            if ("light".equals(value) || "dark".equals(value)) return value;
        } else {
            Matcher mode = Pattern.compile("Night mode: (yes|no|auto)\\b")
                    .matcher(AndroidDevice.adbForSerial(udid, "shell", "cmd", "uimode", "night"));
            if (mode.find()) return mode.group(1);
        }
        throw new IllegalStateException("Cannot snapshot the original OS appearance; no setting was changed");
    }

    private static String simctl(String udid, String... setting) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        java.util.Collections.addAll(command, "xcrun", "simctl", "ui", udid, "appearance");
        java.util.Collections.addAll(command, setting);
        Path output = Files.createTempFile("coach-appearance-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            if (!process.waitFor(20, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("Could not read/change the selected Simulator appearance");
            }
            return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }
}
