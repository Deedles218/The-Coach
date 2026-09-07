package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/** Opt-in, isolated preprod fixture preparation and backend postcondition checks. */
public final class TestModelFixtureSupport {
    private final Path evidence;
    private final String caseKey;
    private final RemoteWebDriver driver;

    public TestModelFixtureSupport(RemoteWebDriver driver) { this(driver, "COA-8517"); }

    public TestModelFixtureSupport(RemoteWebDriver driver, String caseKey) {
        if (!caseKey.matches("COA-(7949|851[1278])")) throw new IllegalArgumentException("Unsupported fixture case");
        this.driver = driver;
        this.caseKey = caseKey;
        this.evidence = Paths.get("target", "test-model-evidence", caseKey + "-" + System.currentTimeMillis());
    }

    public void requireCustomizationAccess() throws Exception {
        try { command("verify-access"); }
        finally {
            Path result = evidence.resolve("access.json");
            if (Files.exists(result)) {
                try (InputStream data = Files.newInputStream(result)) {
                    Allure.addAttachment("Current customization access prerequisites", "application/json", data, "json");
                }
            }
        }
    }

    public void verifyExistingAccount() throws Exception {
        command("verify-existing-account");
        try (InputStream data = Files.newInputStream(evidence.resolve("account.json"))) {
            Allure.addAttachment("Verified existing account identity", "application/json", data, "json");
        }
    }

    public void prepareRecovery() throws Exception { command("prepare-recovery"); }

    public java.util.Map<String,Object> kegel(String operation) throws Exception {
        Assert.assertTrue("Unsupported Kegel bridge operation", operation.matches("(approve|prepare|verify)-kegel"));
        command(operation);
        Path result = evidence.resolve(operation + ".json");
        try (InputStream data = Files.newInputStream(result)) {
            Allure.addAttachment(operation + " evidence", "application/json", data, "json");
        }
        return new org.openqa.selenium.json.Json().toType(new String(Files.readAllBytes(result),
                java.nio.charset.StandardCharsets.UTF_8), java.util.Map.class);
    }

    public void inspectPremiumAccount() throws Exception {
        try { command("inspect-premium"); }
        finally {
            Path result = evidence.resolve("inspection.json");
            if (Files.exists(result)) {
                try (InputStream data = Files.newInputStream(result)) {
                    Allure.addAttachment("Read-only approved Premium account inspection", "application/json", data, "json");
                }
            }
        }
    }

    public void assertRecoveredOnEveryApplicableDay() throws Exception {
        try {
            command("verify-recovery");
        } catch (Exception verificationFailure) {
            // Also covers driver failure or a killed bridge process, where the
            // Python finally block may not have had a chance to restore day 2.
            resetAfterFailure(verificationFailure);
            throw verificationFailure;
        } catch (AssertionError verificationFailure) {
            resetAfterFailure(verificationFailure);
            throw verificationFailure;
        } finally {
            Path result = evidence.resolve("recovered.json");
            if (Files.exists(result)) {
                try (InputStream data = Files.newInputStream(result)) {
                    Allure.addAttachment("Recovered exercise on every applicable program day", "application/json", data, "json");
                }
            }
        }
    }

    private void resetAfterFailure(Throwable original) {
        try { command("reset-recovery"); }
        catch (Exception resetFailure) { original.addSuppressed(resetFailure); }
        catch (AssertionError resetFailure) { original.addSuppressed(resetFailure); }
    }

    private void command(String operation) throws Exception {
        String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertNotNull("Fixture preparation requires an explicit simulator UDID", udid);
        Process process = new ProcessBuilder("python3", "scripts/test_model_fixture_commands.py", operation,
                "--case", caseKey, "--udid", udid, "--evidence", evidence.toString()).inheritIO().start();
        // The sequential 69-day check exceeded the old 150-second bridge limit.
        // Keep this external-I/O budget separate from UI element timeouts.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(
                "verify-recovery".equals(operation) ? 300 : 150);
        try {
            while (!process.waitFor(10, TimeUnit.SECONDS)) {
                // Harmless native query prevents Appium's 60-second idle expiry.
                if (!"reset-recovery".equals(operation)) driver.manage().window().getSize();
                if (System.nanoTime() >= deadline) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                    throw new AssertionError("Fixture operation timed out: " + operation);
                }
            }
            Assert.assertEquals("Fixture operation failed: " + operation + "; see " + evidence, 0, process.exitValue());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
