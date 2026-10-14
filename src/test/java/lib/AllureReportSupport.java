package lib;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Shared Allure conventions for readable scenarios and actionable failures. */
public final class AllureReportSupport {
    private AllureReportSupport() {
    }

    public static void prepareTestCase(Description description) {
        try {
            AllureLifecycle lifecycle = Allure.getLifecycle();
            if (!lifecycle.getCurrentTestCase().isPresent()) {
                return;
            }
            final String readableName = readableTestName(description);
            lifecycle.updateTestCase(result -> result.setName(readableName));
        } catch (RuntimeException reportingFailure) {
            reportInfrastructureError("rename test", reportingFailure);
        }
    }

    public static String startScenario(Description description) {
        try {
            AllureLifecycle lifecycle = Allure.getLifecycle();
            if (!lifecycle.getCurrentTestCase().isPresent()) {
                return null;
            }

            String uuid = UUID.randomUUID().toString();
            lifecycle.startStep(uuid, new StepResult()
                    .setName("Scenario: " + readableTestName(description))
                    .setStart(System.currentTimeMillis()));
            return uuid;
        } catch (RuntimeException reportingFailure) {
            reportInfrastructureError("start scenario step", reportingFailure);
            return null;
        }
    }

    public static void finishScenario(String uuid, Throwable failure) {
        if (uuid == null) {
            return;
        }

        AllureLifecycle lifecycle;
        try {
            lifecycle = Allure.getLifecycle();
        } catch (RuntimeException reportingFailure) {
            reportInfrastructureError("access lifecycle", reportingFailure);
            return;
        }
        try {
            lifecycle.updateStep(uuid, step -> {
                step.setStatus(statusFor(failure));
                if (failure != null) {
                    step.setStatusDetails(new StatusDetails()
                            .setMessage(TestData.sanitizeSensitiveData(failure.toString()))
                            .setTrace(sanitizedStackTrace(failure)));
                }
            });
        } catch (RuntimeException reportingFailure) {
            reportInfrastructureError("update scenario step", reportingFailure);
        }
        try {
            lifecycle.stopStep(uuid);
        } catch (RuntimeException reportingFailure) {
            reportInfrastructureError("stop scenario step", reportingFailure);
        }
    }

    public static String readableTestName(Description description) {
        if (description == null) {
            return "Unknown test";
        }

        DisplayName displayName = description.getAnnotation(DisplayName.class);
        if (displayName != null && !displayName.value().trim().isEmpty()) {
            return displayName.value().trim();
        }

        String methodName = description.getMethodName();
        if (methodName == null || methodName.trim().isEmpty()) {
            return humanizeIdentifier(description.getDisplayName());
        }
        return humanizeIdentifier(methodName);
    }

    public static String failureContext(Throwable failure, Description description) {
        StackTraceElement failureLocation = findFailureLocation(failure, null);
        StackTraceElement testLocation = findFailureLocation(
                failure,
                description == null ? null : description.getClassName()
        );

        StringBuilder context = new StringBuilder();
        context.append("Scenario: ").append(readableTestName(description)).append('\n');
        if (description != null) {
            context.append("Test: ").append(description.getClassName())
                    .append('#').append(description.getMethodName()).append('\n');
        }
        context.append("Failure: ")
                .append(failure == null ? "Unknown failure" : TestData.sanitizeSensitiveData(failure.toString()))
                .append('\n');
        if (failureLocation != null) {
            context.append("Failed action: ")
                    .append(humanizeIdentifier(stripAspectJMethodSuffix(failureLocation.getMethodName())))
                    .append('\n');
            context.append("Failed at: ").append(formatLocation(failureLocation)).append('\n');
        }
        if (testLocation != null && !testLocation.equals(failureLocation)) {
            context.append("Called from test: ").append(formatLocation(testLocation)).append('\n');
        }
        context.append("See the red nested Allure step for the last reported user action.");
        return context.toString();
    }

    public static void attachFailureContext(Throwable failure, Description description) {
        try {
            byte[] context = failureContext(failure, description).getBytes(StandardCharsets.UTF_8);
            Allure.addAttachment(
                    "Failure context",
                    "text/plain",
                    new java.io.ByteArrayInputStream(context),
                    "txt"
            );
        } catch (RuntimeException reportingFailure) {
            reportInfrastructureError("attach failure context", reportingFailure);
        }
    }

    static String humanizeIdentifier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown test";
        }

        String readable = stripAspectJMethodSuffix(value.trim())
                .replaceFirst("^test(?=[A-Z0-9_])", "")
                .replaceFirst("^\\d+[_-]*", "")
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Za-z])([0-9])", "$1 $2")
                .replaceAll("([0-9])([A-Za-z])", "$1 $2")
                .replaceAll("\\s+", " ")
                .trim();

        if (!readable.isEmpty() && Character.isLowerCase(readable.charAt(0))) {
            readable = Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
        }

        readable = readable
                .replaceAll("\\bCoa\\b", "COA")
                .replaceAll("\\bOtp\\b", "OTP")
                .replaceAll("\\bIos\\b", "iOS")
                .replaceAll("\\bPdf\\b", "PDF")
                .replaceAll("\\bFaq\\b", "FAQ")
                .replaceAll("\\bApi\\b", "API")
                .replaceAll("\\bUi\\b", "UI");
        return readable.isEmpty() ? "Unknown test" : readable;
    }

    private static Status statusFor(Throwable failure) {
        if (failure == null) {
            return Status.PASSED;
        }
        if (failure instanceof AssumptionViolatedException) {
            return Status.SKIPPED;
        }
        return failure instanceof AssertionError ? Status.FAILED : Status.BROKEN;
    }

    private static String sanitizedStackTrace(Throwable failure) {
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        return TestData.sanitizeSensitiveData(trace.toString());
    }

    private static StackTraceElement findFailureLocation(Throwable failure, String requestedClass) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (requestedClass != null) {
                    if (requestedClass.equals(frame.getClassName())) {
                        return frame;
                    }
                    continue;
                }
                if (isProjectFrame(frame)) {
                    return frame;
                }
            }
        }
        return failure == null || failure.getStackTrace().length == 0
                ? null
                : failure.getStackTrace()[0];
    }

    private static boolean isProjectFrame(StackTraceElement frame) {
        String className = frame.getClassName();
        return (className.startsWith("tests.") || className.startsWith("lib."))
                && !className.equals(CoreTestCase.class.getName())
                && !className.equals(AllureReportSupport.class.getName())
                && !className.contains("$AjcClosure");
    }

    private static String stripAspectJMethodSuffix(String methodName) {
        return methodName == null ? "unknown" : methodName.replaceFirst("_aroundBody\\d+$", "");
    }

    private static String formatLocation(StackTraceElement frame) {
        return frame.getClassName() + '#' + stripAspectJMethodSuffix(frame.getMethodName())
                + " (" + frame.getFileName() + ':' + frame.getLineNumber() + ')';
    }

    private static void reportInfrastructureError(String action, RuntimeException failure) {
        System.err.println("Allure could not " + action + ": " + failure.getClass().getSimpleName());
    }
}
