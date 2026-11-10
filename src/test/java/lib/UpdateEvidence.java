package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/** Read-only account evidence. The reader emits only UID and subscriptionActive. */
public final class UpdateEvidence {
    private UpdateEvidence() { }

    public static Map<String, Object> account() throws Exception {
        String uid = System.getenv("COACH_EXPECTED_ACCOUNT_UID");
        Assert.assertTrue("Update needs the approved account UID", uid != null && uid.matches("[A-Za-z0-9_-]{20,128}"));
        List<String> command = new ArrayList<>(Arrays.asList("python3", "scripts/read_update_account.py", "--uid", uid));
        if (Platform.getInstance().isAndroid()) {
            command.addAll(Arrays.asList("--platform", "android", "--device", AndroidDevice.serial()));
        } else {
            command.addAll(Arrays.asList("--platform", "ios", "--device", simulatorUdid(),
                    "--bundle", Platform.getInstance().getIOSBundleId()));
        }
        Map<String, Object> state = json(command);
        Assert.assertEquals("Update account identity differs from fixture", uid, state.get("uid"));
        Assert.assertEquals("Update requires a currently active subscription", Boolean.TRUE, state.get("subscriptionActive"));
        Allure.addAttachment("Read-only update account state", "application/json", new Json().toJson(state), "json");
        return state;
    }

    public static Map<String, Object> iosBuild(String appPath) throws Exception {
        Path info = Paths.get(appPath, "Info.plist");
        Assert.assertTrue("iOS update requires an existing .app/Info.plist", Files.isRegularFile(info));
        Map<String, Object> metadata = json(Arrays.asList("plutil", "-convert", "json", "-o", "-", info.toString()));
        validateIOSBuild(metadata, Platform.getInstance().getIOSBundleId());
        return metadata;
    }

    public static Map<String, Object> installedIOSBuild() throws Exception {
        Map<String,Object> info = installedIOSMetadata();
        validateIOSBuild(info, Platform.getInstance().getIOSBundleId());
        return info;
    }

    private static Map<String, Object> installedIOSMetadata() throws Exception {
        String app = command(Arrays.asList("xcrun", "simctl", "get_app_container", simulatorUdid(),
                Platform.getInstance().getIOSBundleId(), "app")).trim();
        return json(Arrays.asList("plutil", "-convert", "json", "-o", "-", Paths.get(app, "Info.plist").toString()));
    }

    public static void attachInstalledBuild() throws Exception {
        if (Platform.getInstance().isAndroid()) {
            attachBuild("android", AndroidDevice.version(), Long.toString(AndroidDevice.versionCode()));
        } else {
            String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
            if (udid != null && udid.matches("(?i)(?:[0-9a-f]{8}-[0-9a-f]{16}|[0-9a-f]{40})")) {
                String bundle = Platform.getInstance().getIOSBundleId();
                Map<String,Object> response = json(Arrays.asList("xcrun", "devicectl", "device", "info", "apps",
                        "--device", udid, "--bundle-id", bundle, "--quiet", "--json-output", "-", "--timeout", "15"));
                Map<?,?> result = (Map<?,?>) response.get("result");
                List<?> apps = (List<?>) result.get("apps");
                Assert.assertEquals("Expected one installed app matching the tested bundle", 1, apps.size());
                Map<?,?> app = (Map<?,?>) apps.get(0);
                Assert.assertEquals(bundle, app.get("bundleIdentifier"));
                String version = (String) app.get("version"), build = (String) app.get("bundleVersion");
                Assert.assertTrue("Installed version and build are required", version != null && !version.isEmpty()
                        && build != null && build.matches("[0-9]+(?:\\.[0-9]+){0,2}"));
                attachBuild("ios", version, build);
                return;
            }
            Map<String,Object> info = installedIOSMetadata();
            attachBuild("ios", (String)info.get("CFBundleShortVersionString"), (String)info.get("CFBundleVersion"));
        }
    }

    /** Read-only metadata for new mobile cases, including explicitly selected real devices. */
    public static void attachInstalledBuild(RemoteWebDriver driver) throws Exception {
        Platform platform = Platform.getInstance();
        String udid = platform.isIOS() ? System.getProperty("ios.udid", System.getenv("IOS_UDID"))
                : System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        Assert.assertTrue("Installed-build evidence requires an explicitly selected device", udid != null && !udid.trim().isEmpty());
        Object actual = driver.getCapabilities().getCapability("appium:udid");
        if (actual == null) actual = driver.getCapabilities().getCapability("udid");
        Assert.assertEquals("Metadata must come from the actual Appium device", udid, actual);
        if (platform.isIOS() && udid.matches("[A-Fa-f0-9-]{36}")) {
            attachInstalledBuild();
        } else if (platform.isAndroid()) {
            String adb = System.getenv("ADB") == null ? "adb" : System.getenv("ADB");
            String dump = command(Arrays.asList(adb, "-s", udid, "shell", "dumpsys", "package", platform.getAndroidAppPackage()));
            Map<String,String> info = androidInstalledBuild(dump);
            attachBuild("android", info.get("version"), info.get("build"));
        } else {
            Path output = Files.createTempFile("coach-installed-ios-app-", ".json");
            try {
                command(Arrays.asList("xcrun", "devicectl", "device", "info", "apps", "--device", udid,
                        "--bundle-id", platform.getIOSBundleId(), "--include-default-apps", "--json-output", output.toString()));
                Map<String,Object> response = new Json().toType(new String(Files.readAllBytes(output), StandardCharsets.UTF_8), Map.class);
                Map<String,String> info = iosDeviceInstalledBuild(response, platform.getIOSBundleId());
                attachBuild("ios", info.get("version"), info.get("build"));
            } finally {
                Files.deleteIfExists(output);
            }
        }
    }

    public static Map<String,String> iosDeviceInstalledBuild(Map<String,Object> response, String expectedBundle) {
        Assert.assertTrue("Device app metadata has no result object", response.get("result") instanceof Map);
        Object apps = ((Map<?,?>) response.get("result")).get("apps");
        Assert.assertTrue("Device app metadata must identify exactly one requested app", apps instanceof List && ((List<?>) apps).size() == 1);
        Object app = ((List<?>) apps).get(0);
        Assert.assertTrue("Device app metadata is not an app object", app instanceof Map);
        Map<?,?> fields = (Map<?,?>) app;
        Assert.assertEquals("Installed metadata belongs to another app", expectedBundle, fields.get("bundleIdentifier"));
        return installedBuildValues(fields.get("version"), fields.get("bundleVersion"));
    }

    public static Map<String,String> androidInstalledBuild(String dump) {
        java.util.regex.Matcher version = java.util.regex.Pattern.compile("versionName=([^\\s]+)").matcher(dump);
        java.util.regex.Matcher build = java.util.regex.Pattern.compile("versionCode=([0-9]+)").matcher(dump);
        Assert.assertTrue("Installed Android version name is absent", version.find());
        Assert.assertTrue("Installed Android version code is absent", build.find());
        return installedBuildValues(version.group(1), build.group(1));
    }

    private static Map<String,String> installedBuildValues(Object version, Object build) {
        Assert.assertTrue("Installed application version is absent", version instanceof String && !((String)version).trim().isEmpty());
        Assert.assertTrue("Installed application build is absent", build instanceof String && !((String)build).trim().isEmpty());
        Map<String,String> result = new HashMap<>();
        result.put("version", (String)version); result.put("build", (String)build);
        return result;
    }

    public static void validateIOSBuild(Map<String, Object> info, String expectedBundle) {
        Assert.assertEquals("Update artifacts must have the same configured bundle ID", expectedBundle, info.get("CFBundleIdentifier"));
        Assert.assertEquals("Update evidence requires an iOS Simulator build", "iphonesimulator", info.get("DTPlatformName"));
        Object build = info.get("CFBundleVersion");
        Assert.assertTrue("CFBundleVersion must be a real build number, not a CI placeholder",
                build instanceof String && ((String) build).matches("[0-9]+(?:\\.[0-9]+){0,2}"));
    }

    public static void assertAccountPreserved(Map<String, Object> before, Map<String, Object> after) {
        Assert.assertEquals("Update changed UID", before.get("uid"), after.get("uid"));
        Assert.assertEquals("Update fixture subscription must be active", Boolean.TRUE, before.get("subscriptionActive"));
        Assert.assertEquals("Update changed subscription", before.get("subscriptionActive"), after.get("subscriptionActive"));
    }

    public static void attachBuild(String platform, String version, String build) {
        Allure.addAttachment("Application build", platform + "; version=" + version + "; build=" + build);
    }

    private static String simulatorUdid() {
        String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertTrue("Update evidence requires an explicit iOS Simulator UDID",
                udid != null && udid.matches("[A-Fa-f0-9-]{36}"));
        return udid;
    }

    private static Map<String, Object> json(List<String> args) throws Exception {
        return new Json().toType(command(args), Map.class);
    }

    private static String command(List<String> args) throws Exception {
        Path result = Files.createTempFile("coach-update-evidence-", ".json");
        Process process = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(result.toFile()).start();
        try {
            Assert.assertTrue("Update evidence reader timed out", process.waitFor(35, TimeUnit.SECONDS));
            Assert.assertEquals("Cannot verify update evidence; check the fixture/device and authenticated session", 0, process.exitValue());
            return new String(Files.readAllBytes(result), StandardCharsets.UTF_8);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(result);
        }
    }
}
