package lib;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/** Frozen, platform/build-specific expectations, never learned during an assertion. */
public final class LocalizationFixture {
    private final Properties data;
    public static final List<String> TEXT_KEYS = Collections.unmodifiableList(Arrays.asList(
            "welcome.start", "welcome.login", "login.title", "login.continue",
            "tab.today", "tab.explore", "tab.shop", "profile.account", "profile.support",
            "profile.terms", "profile.logout"));

    public LocalizationFixture(Properties data) {
        this.data = new Properties();
        this.data.putAll(data);
    }

    public static LocalizationFixture load() throws IOException {
        return load(locales(), TEXT_KEYS);
    }

    public static LocalizationFixture load(String language, String scenario) throws IOException {
        return load(Collections.singletonList(language), keysForScenario(scenario));
    }

    public static List<String> keysForScenario(String scenario) {
        if ("welcome".equals(scenario)) return TEXT_KEYS.subList(0, 4);
        if ("dashboard".equals(scenario) || "switch".equals(scenario)) return TEXT_KEYS.subList(4, TEXT_KEYS.size());
        throw new IllegalArgumentException("Unsupported localization scenario: " + scenario);
    }

    private static LocalizationFixture load(List<String> locales, List<String> keys) throws IOException {
        String path = System.getProperty("localization.fixture");
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Set -Dlocalization.fixture=<frozen UTF-8 properties file>; see docs/localization-smoke.md");
        }
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            data.load(reader);
        }
        LocalizationFixture fixture = new LocalizationFixture(data);
        fixture.validate(Platform.getInstance().getPlatformVar(), variant(),
                Platform.getInstance().isIOS() ? Platform.getInstance().getIOSBundleId()
                        : Platform.getInstance().getAndroidAppPackage(), locales, keys);
        return fixture;
    }

    public static String variant() {
        String variant = System.getProperty("localization.variant", "male");
        supportedLocales(variant);
        return variant;
    }

    public static List<String> supportedLocales(String variant) {
        switch (variant) {
            case "male": return Arrays.asList("en", "fr", "de", "it", "es");
            case "female": return Arrays.asList("en", "es", "fr");
            case "female-legacy": return Collections.singletonList("en");
            default: throw new IllegalArgumentException("localization.variant must be male, female or female-legacy");
        }
    }

    public static List<String> locales() {
        return selectLocales(variant(), System.getProperty("localization.locales"));
    }

    public static List<String> selectLocales(String variant, String configured) {
        List<String> supported = supportedLocales(variant);
        if (configured == null) return new ArrayList<>(supported);
        List<String> selected = new ArrayList<>();
        for (String value : configured.split(",", -1)) {
            String language = value.trim();
            if (!supported.contains(language) || selected.contains(language)) {
                throw new IllegalArgumentException("Unsupported/duplicate locale '" + language + "' for " + variant);
            }
            selected.add(language);
        }
        return selected;
    }

    public void validate(String platform, String variant, String appId, List<String> locales) {
        validate(platform, variant, appId, locales, TEXT_KEYS);
    }

    public void validate(String platform, String variant, String appId, List<String> locales, List<String> keys) {
        for (String[] metadata : new String[][]{{"platform", platform}, {"variant", variant}, {"appId", appId}}) {
            if (!metadata[1].equals(required(metadata[0]))) {
                throw new IllegalArgumentException("Localization fixture mismatch: " + metadata[0]);
            }
        }
        required("source.version");
        required("source.origin");
        // English is required too: existing authentication helpers bootstrap in English.
        for (String language : locales) for (String key : keys) text(language, key);
        for (String key : TEXT_KEYS) text("en", key);
    }

    public String required(String key) {
        String value = data.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            String reason = data.getProperty("missing." + key);
            throw new IllegalArgumentException("Missing localization fixture key: " + key
                    + (reason == null ? "" : " (" + reason + ")"));
        }
        return value;
    }

    public String optional(String key) { return data.getProperty(key); }
    public String text(String language, String key) { return required(language + "." + key); }
    public String region(String language) {
        switch (language) {
            case "en": return "US";
            case "fr": return "FR";
            case "de": return "DE";
            case "it": return "IT";
            case "es": return "ES";
            default: throw new IllegalArgumentException("Unsupported locale: " + language);
        }
    }

    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\p{Z}]+", " ").trim();
    }
}
