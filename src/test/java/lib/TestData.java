package lib;

import java.util.Locale;

/**
 * Test-only data access. Secrets are supplied by the runner or a secret
 * manager and are never stored in source code, Allure metadata, or log output.
 */
public final class TestData {
    private TestData() {
    }

    public static TestAccount existingProgressAccount() {
        return new TestAccount(
                required("coach.existingProgress.email", "COACH_EXISTING_PROGRESS_EMAIL"),
                required("coach.existingProgress.otp", "COACH_EXISTING_PROGRESS_OTP")
        );
    }

    public static TestAccount testModelAccount(String caseKey) {
        String key = caseKey.replace("-", "").toUpperCase(Locale.ROOT);
        String email = configured("coach." + key.toLowerCase(Locale.ROOT) + ".email", "COACH_" + key + "_EMAIL");
        String otp = configured("coach." + key.toLowerCase(Locale.ROOT) + ".otp", "COACH_" + key + "_OTP");
        if (email == null && otp == null && ("COA8235".equals(key) || "COA7949".equals(key))) {
            return existingProgressAccount();
        }
        if (email == null || otp == null) {
            throw new IllegalStateException("Dedicated account required for " + caseKey
                    + ": configure COACH_" + key + "_EMAIL and COACH_" + key + "_OTP");
        }
        return new TestAccount(email, otp);
    }

    public static TestAccount kegelPlayerAccount() {
        String configuredEmail = configured("coach.kegelPlayer.email", "COACH_KEGEL_PLAYER_EMAIL");
        String configuredOtp = configured("coach.kegelPlayer.otp", "COACH_KEGEL_PLAYER_OTP");
        if (configuredEmail == null && configuredOtp == null) {
            return existingProgressAccount();
        }
        if (configuredEmail == null || configuredOtp == null) {
            throw new IllegalStateException(
                    "Configure both coach.kegelPlayer.email/otp or neither; the quick Smoke path reuses the existing-progress account"
            );
        }
        return new TestAccount(
                configuredEmail,
                configuredOtp
        );
    }

    public static TestAccount dedicatedKegelPlayerAccount() {
        return new TestAccount(
                required("coach.kegelPlayer.email", "COACH_KEGEL_PLAYER_EMAIL"),
                required("coach.kegelPlayer.otp", "COACH_KEGEL_PLAYER_OTP")
        );
    }

    public static TestAccount noPdfEntitlementAccount() {
        return new TestAccount(
                required("coach.noPdfEntitlement.email", "COACH_NO_PDF_ENTITLEMENT_EMAIL"),
                required("coach.noPdfEntitlement.otp", "COACH_NO_PDF_ENTITLEMENT_OTP")
        );
    }

    public static String validEmailWithoutProgress() {
        return required("coach.validEmailWithoutProgress", "COACH_VALID_EMAIL_WITHOUT_PROGRESS");
    }

    public static String invalidEmail() {
        String configured = configured("coach.invalidEmail", "COACH_INVALID_EMAIL");
        return configured == null ? "not-an-email" : configured;
    }

    public static Fixture deterministicFixture() {
        return new Fixture(
                configuredOrDefault("coach.fixture.id", "COACH_FIXTURE_ID", "prebuilt"),
                configuredOrDefault("coach.fixture.environment", "COACH_FIXTURE_ENVIRONMENT", "prebuilt"),
                configuredOrDefault("coach.fixture.dailyPlan.id", "COACH_DAILY_PLAN_FIXTURE_ID", "prebuilt-daily-plan"),
                required("coach.fixture.dailyPlan.day", "COACH_DAILY_PLAN_DAY"),
                configuredOrDefault("coach.fixture.kegel.id", "COACH_KEGEL_FIXTURE_ID", "prebuilt-kegel"),
                configured("coach.fixture.noPdfEntitlement.id", "COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID"),
                required("coach.fixture.reset.mode", "COACH_FIXTURE_RESET_MODE")
        );
    }

    public static String noPdfEntitlementFixtureId() {
        return required("coach.fixture.noPdfEntitlement.id", "COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID");
    }

    public static String configuredFixtureId() {
        return configured("coach.fixture.id", "COACH_FIXTURE_ID");
    }

    public static String configuredFixtureEnvironment() {
        return configured("coach.fixture.environment", "COACH_FIXTURE_ENVIRONMENT");
    }

    public static String testIsolationMode() {
        String mode = configured("test.isolation.mode", "TEST_ISOLATION_MODE");
        return mode == null ? "logout" : mode.trim().toLowerCase(Locale.ROOT);
    }

    public static void validateTestIsolationMode() {
        String mode = testIsolationMode();
        if (!"logout".equals(mode) && !"reinstall".equals(mode)) {
            throw new IllegalStateException(
                    "TEST_ISOLATION_MODE must be one of: logout, reinstall"
            );
        }
    }

    public static String sanitizeSensitiveData(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text;
        for (String key : new String[]{"COA7949", "COA8235", "COA8231", "COA8232", "COA8511", "COA8512", "COA8517", "COA8518"}) {
            for (String field : new String[]{"email", "otp"}) {
                String value = configured("coach." + key.toLowerCase(Locale.ROOT) + "." + field,
                        "COACH_" + key + "_" + field.toUpperCase(Locale.ROOT));
                if (value != null) sanitized = sanitized.replace(value, "<redacted-" + field + ">");
            }
        }
        String email = configured("coach.existingProgress.email", "COACH_EXISTING_PROGRESS_EMAIL");
        String otp = configured("coach.existingProgress.otp", "COACH_EXISTING_PROGRESS_OTP");
        String noProgressEmail = configured("coach.validEmailWithoutProgress", "COACH_VALID_EMAIL_WITHOUT_PROGRESS");
        String invalidEmail = configured("coach.invalidEmail", "COACH_INVALID_EMAIL");
        String kegelEmail = configured("coach.kegelPlayer.email", "COACH_KEGEL_PLAYER_EMAIL");
        String kegelOtp = configured("coach.kegelPlayer.otp", "COACH_KEGEL_PLAYER_OTP");
        String noPdfEmail = configured("coach.noPdfEntitlement.email", "COACH_NO_PDF_ENTITLEMENT_EMAIL");
        String noPdfOtp = configured("coach.noPdfEntitlement.otp", "COACH_NO_PDF_ENTITLEMENT_OTP");
        if (email != null) {
            sanitized = sanitized.replace(email, "<redacted-email>");
        }
        if (otp != null) {
            sanitized = sanitized.replace(otp, "<redacted-otp>");
        }
        if (noProgressEmail != null) {
            sanitized = sanitized.replace(noProgressEmail, "<redacted-email>");
        }
        if (invalidEmail != null) {
            sanitized = sanitized.replace(invalidEmail, "<redacted-email>");
        }
        if (kegelEmail != null) {
            sanitized = sanitized.replace(kegelEmail, "<redacted-email>");
        }
        if (kegelOtp != null) {
            sanitized = sanitized.replace(kegelOtp, "<redacted-otp>");
        }
        if (noPdfEmail != null) {
            sanitized = sanitized.replace(noPdfEmail, "<redacted-email>");
        }
        if (noPdfOtp != null) {
            sanitized = sanitized.replace(noPdfOtp, "<redacted-otp>");
        }
        return sanitized;
    }

    private static String required(String propertyName, String environmentName) {
        String value = configured(propertyName, environmentName);
        if (value == null) {
            throw new IllegalStateException(
                    "Required test data is missing. Configure " + propertyName + " or " + environmentName + " in the secret-backed test environment."
            );
        }
        return value;
    }

    private static String configuredOrDefault(String propertyName, String environmentName, String defaultValue) {
        String value = configured(propertyName, environmentName);
        return value == null ? defaultValue : value;
    }

    private static String configured(String propertyName, String environmentName) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue.trim();
        }

        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }

        return null;
    }

    public static final class TestAccount {
        private final String email;
        private final String otp;

        private TestAccount(String email, String otp) {
            this.email = email;
            this.otp = otp;
        }

        public String getEmail() {
            return email;
        }

        public String getOtp() {
            return otp;
        }
    }

    public static final class Fixture {
        private final String fixtureId;
        private final String environment;
        private final String dailyPlanFixtureId;
        private final String dailyPlanDay;
        private final String kegelFixtureId;
        private final String noPdfEntitlementFixtureId;
        private final String resetMode;

        private Fixture(
                String fixtureId,
                String environment,
                String dailyPlanFixtureId,
                String dailyPlanDay,
                String kegelFixtureId,
                String noPdfEntitlementFixtureId,
                String resetMode
        ) {
            this.fixtureId = fixtureId;
            this.environment = environment;
            this.dailyPlanFixtureId = dailyPlanFixtureId;
            this.dailyPlanDay = dailyPlanDay;
            this.kegelFixtureId = kegelFixtureId;
            this.noPdfEntitlementFixtureId = noPdfEntitlementFixtureId;
            this.resetMode = resetMode;
            validateResetMode(resetMode);
        }

        public String getFixtureId() {
            return fixtureId;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getDailyPlanFixtureId() {
            return dailyPlanFixtureId;
        }

        public String getDailyPlanDay() {
            return dailyPlanDay;
        }

        public String getKegelFixtureId() {
            return kegelFixtureId;
        }

        public String getNoPdfEntitlementFixtureId() {
            return noPdfEntitlementFixtureId;
        }

        public String getResetMode() {
            return resetMode;
        }

        private static void validateResetMode(String mode) {
            if (!"backend_api".equals(mode)
                    && !"prebuilt".equals(mode)
                    && !"clean_install".equals(mode)) {
                throw new IllegalStateException(
                        "COACH_FIXTURE_RESET_MODE must be one of: backend_api, prebuilt, clean_install"
                );
            }
        }
    }
}
