package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.json.Json;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Reads the UID-verified response produced by this UI action; makes no API requests. */
public final class SimulatorProgramContent {
    private SimulatorProgramContent() { }

    public static Map<String,Object> read(String caseKey, String programId, long actionBoundary) throws Exception {
        Assert.assertTrue("Unexpected selector fixture", "COA-8232".equals(caseKey) || "COA-8231".equals(caseKey));
        String uid = System.getenv("COACH_" + caseKey.replace("-", "") + "_UID");
        Assert.assertNotNull("Current program content requires a dedicated fixture UID", uid);
        Path folder = Files.createTempDirectory(Paths.get("target"), caseKey + "-content-");
        Path result = folder.resolve("content.json"), log = folder.resolve("reader.log");
        ProcessBuilder builder = new ProcessBuilder("python3", "scripts/read_simulator_program_content.py",
                "--udid", System.getProperty("ios.udid"), "--program-id", programId,
                "--not-before-ms", Long.toString(actionBoundary), "--output", result.toString());
        builder.environment().put("COACH_EXPECTED_PROGRAM_UID", uid);
        Process reader = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            Assert.assertTrue("Current Today response reader timed out", reader.waitFor(20, TimeUnit.SECONDS));
            Allure.addAttachment("Current Today response reader", new String(Files.readAllBytes(log), StandardCharsets.UTF_8));
            Assert.assertEquals("No verified response from current UI action; see " + log, 0, reader.exitValue());
            try (InputStream in = Files.newInputStream(result)) {
                Allure.addAttachment("UID-verified current Today content", "application/json", in, "json");
            }
            return new Json().toType(new String(Files.readAllBytes(result), StandardCharsets.UTF_8), Map.class);
        } finally {
            if (reader.isAlive()) reader.destroyForcibly();
        }
    }
}
