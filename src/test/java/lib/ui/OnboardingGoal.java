package lib.ui;

import java.util.Locale;

/**
 * Product-owned mapping between the goal selected in onboarding and the
 * program identifier displayed on Today.
 */
public enum OnboardingGoal {
    BEAT_PREMATURE_EJACULATION("Beat premature ejaculation", "LL"),
    BEAT_ERECTILE_DYSFUNCTION("Beat erectile dysfunction", "KIH"),
    BOOST_OVERALL_HEALTH("Boost overall health", "OH"),
    OVERCOME_PORN_ADDICTION("Overcome porn addiction", "Unhooked"),
    IMPROVE_SEX_SKILLS("Improve sex skills", "SIAS");

    private final String displayName;
    private final String expectedProgram;

    OnboardingGoal(String displayName, String expectedProgram) {
        this.displayName = displayName;
        this.expectedProgram = expectedProgram;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExpectedProgram() {
        return expectedProgram;
    }

    public static OnboardingGoal fromConfiguredValue(String value) {
        String normalized = normalize(value);
        for (OnboardingGoal goal : values()) {
            if (normalize(goal.displayName).equals(normalized)
                    || normalize(goal.name()).equals(normalized)) {
                return goal;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported onboarding goal '" + value + "'. Supported values: "
                        + "Beat premature ejaculation, Beat erectile dysfunction, "
                        + "Boost overall health, Overcome porn addiction, Improve sex skills"
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.US)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
