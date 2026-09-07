package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Local iOS-Simulator evidence source; never accepts a pre-existing log file. */
public final class SimulatorAnalyticsEvidence implements AutoCloseable {
    private final Process observer;
    private final Path evidence;

    public SimulatorAnalyticsEvidence(String caseKey, String event) throws Exception {
        this(caseKey, event, System.getenv("COACH_" + caseKey.replace("-", "") + "_UID"));
    }

    public SimulatorAnalyticsEvidence(String caseKey, String event, String uid) throws Exception {
        this(caseKey, event, uid, null);
    }

    /** ProgramModalSelect must carry the fixture's exact destination program_id. */
    public SimulatorAnalyticsEvidence(String caseKey, String event, String uid, String expectedProgramId) throws Exception {
        if (!caseKey.matches("COA-[0-9]+")) throw new IllegalArgumentException("Invalid case key");
        if ("ProgramModalSelect".equals(event)) {
            if (expectedProgramId == null || expectedProgramId.trim().isEmpty()) {
                throw new IllegalArgumentException("ProgramModalSelect requires an exact expected program_id");
            }
        } else if (expectedProgramId != null) {
            throw new IllegalArgumentException("Expected program_id is supported only for ProgramModalSelect");
        }
        Assert.assertNotNull("Fresh analytics verification needs the dedicated fixture UID", uid);
        evidence = Paths.get("target", "test-model-evidence", caseKey + "-" + System.currentTimeMillis(), "analytics.json");
        ProcessBuilder builder = new ProcessBuilder("python3", "scripts/observe_simulator_event.py",
                "--database", SimulatorTestIdentity.databasePath().toString(),
                "--event", event, "--output", evidence.toString()).redirectErrorStream(true);
        if (expectedProgramId != null) {
            builder.command().add("--expected-program-id");
            builder.command().add(expectedProgramId);
        }
        builder.environment().put("COACH_EXPECTED_ANALYTICS_UID", uid);
        observer = builder.start();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            Future<String> ready = reader.submit(() -> new BufferedReader(new InputStreamReader(
                    observer.getInputStream(), StandardCharsets.UTF_8)).readLine());
            Assert.assertEquals("Analytics observer failed to arm", "READY", ready.get(10, TimeUnit.SECONDS));
        } catch (Throwable failure) {
            observer.destroyForcibly();
            throw failure;
        } finally {
            reader.shutdownNow();
        }
    }

    public void assertObserved() throws Exception {
        Assert.assertTrue("Analytics observer did not finish", observer.waitFor(45, TimeUnit.SECONDS));
        if (Files.exists(evidence)) {
            try (java.io.InputStream data = Files.newInputStream(evidence)) {
                Allure.addAttachment("Fresh simulator analytics evidence", "application/json", data, "json");
            }
        }
        Assert.assertEquals("Current-run analytics event was not observed; see " + evidence,
                0, observer.exitValue());
    }

    @Override
    public void close() {
        if (observer.isAlive()) observer.destroyForcibly();
    }
}
