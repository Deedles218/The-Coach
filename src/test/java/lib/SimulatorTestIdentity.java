package lib;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Records only the UID of the verified account, never its email or OTP. */
public final class SimulatorTestIdentity {
    private SimulatorTestIdentity() { }

    public static void record(String caseKey) throws Exception {
        String uid = currentUid();
        if (!uid.matches("[A-Za-z0-9_-]{20,128}")) {
            throw new IllegalStateException("Verified account has no usable analytics UID");
        }
        if (!caseKey.matches("COA-[0-9]+")) throw new IllegalArgumentException("Invalid case key");
        Path folder = Paths.get("target", "test-model-fixtures");
        Files.createDirectories(folder);
        String json = "{\"case\":\"" + caseKey + "\",\"uid\":\"" + uid
                + "\",\"reloginVerified\":true,\"bundleId\":\"" + Platform.getInstance().getIOSBundleId() + "\"}";
        Files.write(folder.resolve(caseKey + ".json"), json.getBytes(StandardCharsets.UTF_8));
    }

    /** Call only after UI verification of the requested fixture account. */
    public static String currentUid() throws Exception {
        String uid = command("sqlite3", "-readonly", databasePath().toString(),
                "SELECT value FROM store WHERE key='user_id';").trim();
        if (!uid.matches("[A-Za-z0-9_-]{20,128}")) throw new IllegalStateException("Missing analytics UID");
        return uid;
    }

    public static Path databasePath() throws Exception {
        String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        if (udid == null || !udid.matches("[A-Fa-f0-9-]{36}")) {
            throw new IllegalStateException("Fixture identity recording requires an explicit simulator UDID");
        }
        String bundle = Platform.getInstance().getIOSBundleId();
        String container = command("xcrun", "simctl", "get_app_container", udid, bundle, "data").trim();
        return Paths.get(container, "Library", "com.amplitude.database");
    }

    private static String command(String... args) throws Exception {
        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream input = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) out.write(buffer, 0, count);
        }
        if (process.waitFor() != 0) throw new IllegalStateException("Simulator identity command failed: " + args[0]);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
