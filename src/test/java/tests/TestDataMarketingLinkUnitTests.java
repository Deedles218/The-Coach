package tests;

import lib.TestData;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/** Test the real environment lookup without changing the parent test process. */
public class TestDataMarketingLinkUnitTests {
    @Test public void marketingUrlIsMaskedInLogsAndXmlBeforeItsUidIsMasked() throws Exception {
        Path output = Files.createTempFile("marketing-link-redaction-", ".txt");
        Process process = null;
        try {
            ProcessBuilder command = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    Probe.class.getName());
            command.environment().put("COACH_MARKETING_LINK", "  https://example.invalid/route?uid=fixture-owner&token=fixture-secret  ");
            command.environment().put("COACH_EXPECTED_ACCOUNT_UID", "fixture-owner");
            process = command.redirectErrorStream(true).redirectOutput(output.toFile()).start();
            Assert.assertTrue("Marketing link probe timed out", process.waitFor(10, TimeUnit.SECONDS));
            Assert.assertEquals("The configured marketing link was not protected", 0, process.exitValue());
            Assert.assertEquals("protected", new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(output);
        }
    }

    public static class Probe {
        public static void main(String[] args) {
            String link = System.getenv("COACH_MARKETING_LINK");
            String input = link + " " + link.trim().replace("&", "&amp;") + " https://example.invalid/help";
            if (!"[redacted-link] [redacted-link] https://example.invalid/help".equals(TestData.sanitizeSensitiveData(input))) {
                throw new AssertionError("Raw or XML-escaped marketing link was not redacted");
            }
            System.out.print("protected");
        }
    }
}
