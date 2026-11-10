package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.json.Json;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Explicit physical-device TestFlight operations, verified by the owning installer. */
public final class TestFlightHandoff {
    private TestFlightHandoff() { }
    private static String configuredDirectory() {
        return System.getProperty("coach.ios.testFlightHandoff",
                System.getProperty("coach.bookmarks.testFlightHandoff"));
    }
    public static boolean enabled() {
        String directory = configuredDirectory();
        return directory != null && !directory.trim().isEmpty();
    }
    public static void perform(org.openqa.selenium.remote.RemoteWebDriver driver, String phase) {
        Assert.assertTrue("TestFlight handoff requires an iOS bundle-ID attach session",
                Platform.getInstance().isIOS() && Platform.getInstance().isTestFlightAttachConfigured());
        String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertTrue("TestFlight handoff requires an explicit physical iPhone",
                udid != null && udid.matches("(?i)(?:[0-9a-f]{8}-[0-9a-f]{16}|[0-9a-f]{40})"));
        Assert.assertTrue("Unsupported TestFlight operation", java.util.Arrays.asList(
                "prepare-clean-rc", "install-previous", "install-rc", "reinstall-rc", "ensure-rc").contains(phase));
        try {
            exchange(driver, phase);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TestFlight operation interrupted: " + phase, error);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot verify TestFlight operation: " + phase, error);
        }
    }
    public static void ensureRCIfEnabled(org.openqa.selenium.remote.RemoteWebDriver driver) {
        if (enabled() && driver != null) perform(driver, "ensure-rc");
    }

    private static void exchange(org.openqa.selenium.remote.RemoteWebDriver driver, String phase) throws Exception {
        String configuredDirectory = configuredDirectory();
        Assert.assertTrue("Configure the task-specific TestFlight handoff directory",
                configuredDirectory != null && !configuredDirectory.trim().isEmpty());
        Path directory = Paths.get(configuredDirectory);
        Files.createDirectories(directory);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("phase", phase);
        request.put("sessionId", driver.getSessionId().toString());
        request.put("bundle", lib.Platform.getInstance().getIOSBundleId());
        request.put("requestedBuild", System.getProperty("coach.run.appBuild"));
        String previousBuild = System.getProperty("coach.ios.previousBuild",
                System.getProperty("coach.bookmarks.previousBuild"));
        if ("install-previous".equals(phase)) {
            Assert.assertTrue("Configure a verified previous TestFlight build",
                    previousBuild != null && previousBuild.matches("[0-9]+"));
            Assert.assertNotEquals("Update needs different previous and target builds",
                    System.getProperty("coach.run.appBuild"), previousBuild);
            request.put("previousBuild", previousBuild);
        }
        request.put("time", System.currentTimeMillis());
        Path reply = directory.resolve(phase + ".done");
        Assert.assertFalse("A previous handoff result must not be reused", Files.exists(reply));
        Files.write(directory.resolve(phase + ".request.json"), new Json().toJson(request).getBytes(StandardCharsets.UTF_8));
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(30);
        long heartbeat = 0;
        while (!Files.exists(reply) && System.nanoTime() < deadline) {
            Assert.assertFalse("TestFlight coordinator failed; inspect its saved diagnostic",
                    Files.exists(directory.resolve(phase + ".coordinator-error")));
            if (System.nanoTime() >= heartbeat) {
                driver.manage().timeouts().getImplicitWaitTimeout();
                heartbeat = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            }
            Thread.sleep(500);
        }
        Assert.assertTrue("TestFlight operation did not finish: " + phase, Files.exists(reply));
        String result = new String(Files.readAllBytes(reply), StandardCharsets.UTF_8);
        Allure.addAttachment("Physical TestFlight operation " + phase, result);
        Assert.assertTrue("TestFlight operation failed: " + phase, result.startsWith("done"));
        Map<?, ?> proof = new Json().toType(result.substring(result.indexOf('\n') + 1), Map.class);
        Assert.assertEquals("TestFlight proof belongs to another phase", phase, proof.get("phase"));
        java.util.List<?> after = (java.util.List<?>) proof.get("after");
        Assert.assertEquals("TestFlight must install exactly one matching app", 1, after.size());
        Map<?, ?> installed = (Map<?, ?>) after.get(0);
        Assert.assertEquals("Installed TestFlight bundle differs from the test target",
                lib.Platform.getInstance().getIOSBundleId(), installed.get("bundleIdentifier"));
        Assert.assertEquals("TestFlight operation installed the wrong build",
                "install-previous".equals(phase) ? previousBuild : System.getProperty("coach.run.appBuild"),
                installed.get("bundleVersion"));
    }
}
