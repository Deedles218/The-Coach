package lib;

import org.junit.AssumptionViolatedException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

/** Diagnostic routing, not an automatic product-defect verdict. */
public final class FailureClassifier {
    private FailureClassifier() { }

    public static String type(Throwable failure) {
        String messages = messages(failure);
        if (hasExternalServiceEvidence(failure)) return "external-service";
        if (hasCause(failure, AssumptionViolatedException.class)) {
            return messages.contains("BLOCKED:") ? "blocked-by-precondition" : "not-applicable";
        }
        if (hasCause(failure, AccountVariantMismatchException.class)
                || hasCause(failure, PostAuthQuestionnaireException.class)
                || messages.matches("(?s).*(Required test data|Dedicated account|dedicated account|"
                + "Daily Plan day does not match the deterministic fixture|Current existing-progress Daily Plan state).*")) {
            return "test-data";
        }
        if (messages.matches("(?s).*(localization.fixture|Missing localization fixture key|Localization fixture mismatch|"
                + "onboarding.slides.fixture|coach.kegelUi.fixture|TEST_ISOLATION_MODE must|"
                + "ANDROID_RECOVERY_MODE must|COACH_FIXTURE_RESET_MODE must|"
                + "Push permission suite requires|P0 clean-install smoke requires|Update smoke requires|"
                + "StoreKit suite requires|Enable TestFlight/StoreKit sandbox|"
                + "iOS paywall E2E must attach|iOS paywall E2E requires|"
                + "Configure -D|Supply -D).*")) {
            return "configuration";
        }
        if (hasCause(failure, SessionStateRecoveryException.class)
                || hasCause(failure, SessionNotCreatedException.class)
                || hasCause(failure, NoSuchSessionException.class)) {
            return "environment";
        }
        if (hasCause(failure, InvalidSelectorException.class)
                || hasCause(failure, NoSuchElementException.class)) {
            return "automation-locator";
        }
        if (hasCause(failure, TimeoutException.class)) {
            return "automation-sync";
        }
        if (hasCause(failure, WebDriverException.class)) {
            return "driver";
        }
        return hasCause(failure, AssertionError.class) ? "assertion-mismatch" : "needs-investigation";
    }

    public static String confidence(Throwable failure) {
        String type = type(failure);
        return "test-data".equals(type) || "configuration".equals(type) || "external-service".equals(type)
                || "environment".equals(type) || "blocked-by-precondition".equals(type)
                || "not-applicable".equals(type) ? "high" : "needs-investigation";
    }

    public static void addPageEvidence(Throwable failure, String pageSource) {
        // Require the actual error-page container and heading observed in Safari,
        // not a lone "502" in arbitrary lesson text. Never change the primary failure.
        if (failure != null && hasCause(failure, TimeoutException.class) && pageSource != null
                && pageSource.contains("XCUIElementTypeWebView")
                && pageSource.matches("(?s).*<XCUIElementTypeOther\\b(?=[^>]*name=\"Error 502\")"
                    + "(?=[^>]*visible=\"true\")[^>]*>.*")
                && pageSource.contains("502 - Bad Gateway")
                && !hasExternalServiceEvidence(failure)) {
            failure.addSuppressed(new ExternalServiceException(
                    "Visible iOS browser content matched the observed 502 Bad Gateway page"));
        }
    }

    private static boolean hasExternalServiceEvidence(Throwable failure) {
        if (hasCause(failure, ExternalServiceException.class)) return true;
        for (Throwable current = failure; current != null; current = current.getCause()) {
            for (Throwable suppressed : current.getSuppressed()) {
                if (suppressed instanceof ExternalServiceException) return true;
            }
        }
        return false;
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private static String messages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
