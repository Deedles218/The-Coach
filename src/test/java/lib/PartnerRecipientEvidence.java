package lib;

import io.qameta.allure.Allure;
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Read-only, UID-verified For Her evidence. No tokens are returned by the device bridge. */
public final class PartnerRecipientEvidence {
    private final String identityHash;
    private final boolean active;

    private PartnerRecipientEvidence(String identityHash, boolean active) {
        this.identityHash = identityHash;
        this.active = active;
    }

    public String identityHash() { return identityHash; }
    public boolean active() { return active; }

    public static PartnerRecipientEvidence capture(RemoteWebDriver driver) throws Exception {
        String device = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertNotNull("Recipient evidence requires an explicit device", device);
        Process process = new ProcessBuilder("python3", "scripts/read_partner_recipient.py", "--udid", device)
                .redirectErrorStream(true).start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        try {
            while (!process.waitFor(10, TimeUnit.SECONDS)) {
                driver.manage().window().getSize();
                Assert.assertTrue("Recipient evidence timed out", System.nanoTime() < deadline);
            }
            Assert.assertEquals("Current recipient membership evidence is unavailable", 0, process.exitValue());
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = process.getInputStream().read(buffer)) != -1) bytes.write(buffer, 0, count);
            String evidence = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            Map<String, Object> value = new Json().toType(evidence, Map.class);
            Assert.assertEquals("Recipient identity is not verified", Boolean.TRUE, value.get("identityVerified"));
            Assert.assertTrue("Recipient identity hash is missing", value.get("identityHash") instanceof String
                    && ((String) value.get("identityHash")).matches("[0-9a-f]{64}"));
            Assert.assertTrue("Recipient membership is unknown", value.get("subscriptionActive") instanceof Boolean);
            Allure.addAttachment("Current recipient identity and membership", "application/json", evidence, "json");
            return new PartnerRecipientEvidence((String) value.get("identityHash"), (Boolean) value.get("subscriptionActive"));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    public static PartnerRecipientEvidence waitForActivation(RemoteWebDriver driver, String identityHash) {
        return new MainPageObject(driver).createWait(40).pollingEvery(Duration.ofSeconds(3))
                .withMessage("The invitation did not grant Premium to the original recipient").until(ignored -> {
            PartnerRecipientEvidence state;
            try { state = capture(driver); }
            catch (Exception failure) { throw new AssertionError("Recipient membership evidence became unavailable"); }
            Assert.assertTrue("Invitation activation changed the recipient identity", identityHash.equals(state.identityHash));
            return state.active ? state : null;
        });
    }

    public static void assertRemainsUnpaid(RemoteWebDriver driver, String identityHash) {
        final long started = System.nanoTime();
        new MainPageObject(driver).createWait(30).pollingEvery(Duration.ofSeconds(3))
                .withMessage("Could not observe the full used-invitation delivery window").until(ignored -> {
            PartnerRecipientEvidence state;
            try { state = capture(driver); }
            catch (Exception failure) { throw new AssertionError("Recipient membership evidence became unavailable"); }
            Assert.assertTrue("Used-link check changed the recipient identity", identityHash.equals(state.identityHash));
            Assert.assertFalse("An already used invitation granted Premium to a second recipient", state.active);
            return System.nanoTime() - started >= TimeUnit.SECONDS.toNanos(10);
        });
    }
}
