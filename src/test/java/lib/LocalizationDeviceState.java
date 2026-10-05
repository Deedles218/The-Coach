package lib;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Host-side snapshot survives a failed Appium session and restores only language preferences. */
public final class LocalizationDeviceState implements AutoCloseable {
    private final Path snapshot;

    private LocalizationDeviceState(Path snapshot) { this.snapshot = snapshot; }

    public static LocalizationDeviceState capture() throws Exception {
        Platform platform = Platform.getInstance();
        String udid = platform.isAndroid() ? AndroidDevice.serial()
                : System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        if (udid == null || !udid.matches("[A-Fa-f0-9-]{36}|emulator-[0-9]+")) {
            throw new IllegalArgumentException("Localization system changes require an explicit iOS Simulator UDID or Android emulator serial");
        }
        Path snapshot = Files.createTempFile("coach-localization-", ".json");
        try {
            run("snapshot", snapshot.toString(), platform.getPlatformVar(), udid,
                    platform.isIOS() ? platform.getIOSBundleId() : platform.getAndroidAppPackage());
            return new LocalizationDeviceState(snapshot);
        } catch (Exception | Error failure) {
            Files.deleteIfExists(snapshot);
            throw failure;
        }
    }

    @Override public void close() throws Exception {
        // Keep the recovery file if restoration fails; the error names its path.
        try {
            run("restore", snapshot.toString());
        } catch (Exception failure) {
            throw new IllegalStateException("Could not restore localization preferences; recovery file: " + snapshot, failure);
        }
        Files.deleteIfExists(snapshot);
    }

    public void prepare() throws Exception {
        // Snapshot is already owned by the test lifecycle before the first mutation.
        run("prepare", snapshot.toString());
    }

    public void prepareLanguageMenu() throws Exception {
        run("prepare-menu", snapshot.toString());
    }

    public static String currentAndroidLocale() throws Exception {
        return run("locale", "android", AndroidDevice.serial()).trim();
    }

    public static void setAndroidLocale(String language, String country) throws Exception {
        run("set-locale", "android", AndroidDevice.serial(), language, country);
    }

    private static String run(String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python3"); command.add("scripts/localization_device.py");
        command.addAll(Arrays.asList(arguments));
        Path log = Files.createTempFile("coach-localization-command-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(45, TimeUnit.SECONDS)) throw new IllegalStateException("Localization device operation timed out");
            if (process.exitValue() != 0) throw new IllegalStateException(TestData.sanitizeSensitiveData(
                    new String(Files.readAllBytes(log), StandardCharsets.UTF_8)));
            return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
    }
}
