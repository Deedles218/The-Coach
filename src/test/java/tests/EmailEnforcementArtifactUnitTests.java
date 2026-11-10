package tests;

import lib.CoreTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/** Exercise the actual artifact path: both OTP forms must mask digits and omit the screenshot. */
public class EmailEnforcementArtifactUnitTests {
    private static class FakeDriver extends RemoteWebDriver implements TakesScreenshot {
        private final String source;
        FakeDriver(String marker) {
            source = "<App><XCUIElementTypeStaticText name=\"" + marker + "\"/>"
                    + "<XCUIElementTypeStaticText value=\"7\" label=\"7\" text=\"7\"/>"
                    + "<XCUIElementTypeStaticText value=\"3\" label=\"3\"/></App>";
        }
        @Override public String getPageSource() { return source; }
        @Override public <X> X getScreenshotAs(OutputType<X> type) {
            return type.convertFromBase64Png("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWQAAAABJRU5ErkJggg==");
        }
    }

    private static class Harness extends CoreTestCase {
        Harness(String marker) { driver = new FakeDriver(marker); }
        void capture(String name) {
            captureFailureArtifacts(new AssertionError("Expected test failure"),
                    Description.createTestDescription(EmailEnforcementArtifactUnitTests.class, name));
        }
    }

    private void assertSafeArtifacts(String marker) throws Exception {
        String name = "email-otp-unit-" + UUID.randomUUID();
        new Harness(marker).capture(name);
        Path source = Paths.get("target", "page-source", name + ".xml");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        Assert.assertTrue("OTP digits were not masked", text.contains("value=\"*\""));
        Assert.assertFalse("An individual OTP digit leaked", text.contains("label=\"7\"") || text.contains("value=\"3\"") || text.contains("text=\"7\""));
        Assert.assertFalse("OTP screenshot leaked", Files.exists(Paths.get("target", "screenshots", name + ".png")));
        Assert.assertFalse("OTP screenshot reached the login-only redactor",
                Files.exists(Paths.get("target", "screenshots", name + "-redacted.png")));
    }

    @Test public void dailyEmailOtpArtifactsHideDigits() throws Exception {
        assertSafeArtifacts("An email with a code has been sent to: test@example.test");
    }

    @Test public void existingWelcomeOtpProtectionIsPreserved() throws Exception {
        assertSafeArtifacts("ENTER SECURITY CODE");
    }

    @Test public void androidCustomOtpArtifactsHideDigits() throws Exception {
        assertSafeArtifacts("com.vamapps.thecoach:id/smsVerifyView");
    }
}
