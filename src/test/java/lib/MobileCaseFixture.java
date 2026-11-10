package lib;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/** Reviewed, non-secret UI/data contract for one case and one application build. */
public final class MobileCaseFixture {
    private final Properties values = new Properties();
    private final String prefix;

    public MobileCaseFixture(Properties values, String caseKey) {
        if (!caseKey.matches("COA-[0-9]+")) throw new IllegalArgumentException("Invalid case key");
        this.values.putAll(values);
        prefix = caseKey + ".";
    }

    public static MobileCaseFixture load(String caseKey) throws IOException {
        String path = System.getProperty("coach.mobileCases.fixture");
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalStateException("Set -Dcoach.mobileCases.fixture=<reviewed UTF-8 properties>; see docs/mobile-platform-parity.md");
        }
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            values.load(reader);
        }
        MobileCaseFixture fixture = new MobileCaseFixture(values, caseKey);
        Platform platform = Platform.getInstance();
        fixture.validate(platform.getPlatformVar(), platform.isIOS()
                ? platform.getIOSBundleId() : platform.getAndroidAppPackage());
        return fixture;
    }

    public void validate(String platform, String appId) {
        if (!platform.equals(global("platform")) || !appId.equals(global("app.id"))) {
            throw new IllegalStateException("Mobile case fixture belongs to another platform/application");
        }
        global("source.version");
        global("environment");
        required("program");
        required("day");
    }

    private String global(String key) { return nonempty(values.getProperty(key), key); }

    public String required(String key) { return nonempty(values.getProperty(prefix + key), prefix + key); }

    public String optional(String key) {
        String value = values.getProperty(prefix + key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public String selector(String key) {
        String value = required(key);
        if (!(value.startsWith("id:") || value.startsWith("xpath:"))
                || value.substring(value.indexOf(':') + 1).trim().isEmpty()) {
            throw new IllegalArgumentException("Supply an observed id:/xpath: selector for " + prefix + key);
        }
        return value;
    }

    public String relativeSelector(String key) {
        String value = selector(key);
        if (value.startsWith("xpath:") && (!value.startsWith("xpath:.//") || value.contains("|")
                || value.contains("..") || value.matches(".*(?:ancestor|parent|following|preceding)::.*"))) {
            throw new IllegalArgumentException("Card-child XPath must be relative for " + prefix + key);
        }
        return value;
    }

    public int positiveInt(String key) {
        try {
            int value = Integer.parseInt(required(key));
            if (value > 0) return value;
        } catch (NumberFormatException invalid) {
            // Do not echo fixture contents into diagnostics.
        }
        throw new IllegalArgumentException("A positive integer is required for " + prefix + key);
    }

    private static String nonempty(String value, String key) {
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("unknown")
                || value.contains("<observed") || value.contains("<prepared") || value.contains("TODO")) {
            throw new IllegalStateException("Reviewed mobile fixture value missing: " + key);
        }
        return value.trim();
    }
}
