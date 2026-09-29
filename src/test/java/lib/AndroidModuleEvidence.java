package lib;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Assert;
import io.qameta.allure.Allure;
import org.openqa.selenium.json.Json;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** App-private evidence on a rooted test emulator. No token leaves the reader. */
public final class AndroidModuleEvidence {
    private AndroidModuleEvidence() { }

    public static String currentUid() throws Exception {
        String serial = System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        Assert.assertNotNull("Android module evidence requires an explicit serial", serial);
        Process p = new ProcessBuilder("python3", "scripts/read_android_module_content.py",
                "--serial", serial, "--identity").redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream in = p.getInputStream()) {
            byte[] bytes = new byte[1024];
            int n;
            while ((n = in.read(bytes)) != -1) output.write(bytes, 0, n);
        }
        Assert.assertEquals("Android identity unavailable; use a rooted test emulator", 0, p.waitFor());
        String uid = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
        Assert.assertTrue("Invalid Android identity", uid.matches("[A-Za-z0-9_-]{20,128}"));
        return uid;
    }

    public static Map<String,Object> readIncompleteModule(String program, ModuleStage expected,
                                                         long boundary) throws Exception {
        Assert.assertEquals("Android fixture must stay in the first module", 1, expected.module);
        String serial = System.getProperty("android.udid", System.getenv("ANDROID_UDID"));
        Assert.assertNotNull("Android evidence requires an explicit serial", serial);
        String uid = System.getenv("COACH_COA9044_UID");
        Assert.assertNotNull("Android evidence requires approved UID", uid);
        Files.createDirectories(Paths.get("target"));
        Path folder = Files.createTempDirectory(Paths.get("target"), "android-module-content-");
        Path result = folder.resolve("content.json"), log = folder.resolve("reader.log");
        ProcessBuilder builder = new ProcessBuilder("python3", "scripts/read_android_module_content.py",
                "--serial", serial, "--program-id", program, "--day", Integer.toString(expected.stage),
                "--not-before-ms", Long.toString(boundary), "--output", result.toString());
        builder.environment().put("COACH_EXPECTED_PROGRAM_UID", uid);
        Process reader = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            Assert.assertTrue("Android catalog evidence timed out", reader.waitFor(40, TimeUnit.SECONDS));
            Allure.addAttachment("Android module evidence reader", new String(Files.readAllBytes(log), StandardCharsets.UTF_8));
            Assert.assertEquals("Android evidence rejected; see " + log, 0, reader.exitValue());
            String json = new String(Files.readAllBytes(result), StandardCharsets.UTF_8);
            Allure.addAttachment("Android UID-verified catalog after UI transition", "application/json", json, "json");
            return new Json().toType(json, Map.class);
        } finally {
            if (reader.isAlive()) reader.destroyForcibly();
        }
    }
}
