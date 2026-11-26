package lib.ui.android;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Fallback for a release questionnaire with rendered text missing from WebView accessibility. */
final class AndroidQuestionnaireTextReader {
    private static final Pattern PURCHASE_COPY = Pattern.compile(
            "(?i)subscription|restore purchases|try for free|start free trial|annual plan|monthly plan|subscribe|[$€£]");
    private final RemoteWebDriver driver;
    private final Path executable;

    AndroidQuestionnaireTextReader(RemoteWebDriver driver, Path executable) {
        this.driver = driver;
        this.executable = executable;
    }

    boolean tapQuestionnaireAction() {
        List<Map<String, Object>> lines = read();
        String question = lines.stream().map(AndroidQuestionnaireTextReader::label)
                .collect(Collectors.joining(" "));
        if (PURCHASE_COPY.matcher(question).find()) return false;

        Map<String, Object> action = null;
        if (question.contains("relate to this statement") || question.contains("relate to the following statement")
                || question.contains("regular sexual partner")) {
            action = firstMatching(lines, "yes", "yes, i do");
        } else if (question.contains("how confident do you feel during sex")) {
            action = firstMatching(lines, "super confident");
        } else if (question.contains("how close do you currently feel to your partner")) {
            // This question renders an unlabelled row of five pictograms.
            tap(.50, .415);
            return true;
        } else if (question.contains("rate from 1 to 5")) {
            action = firstMatching(lines, "3");
            if (action == null) {
                Map<String, Object> scale = lines.stream().filter(line -> label(line).contains("rate from 1 to 5"))
                        .findFirst().orElse(null);
                if (scale != null) {
                    // The center response is on the row below this specific scale label.
                    tap(.50, coordinate(scale, "y") + .078);
                    return true;
                }
            }
        }
        if (action == null) {
            action = lines.stream().filter(line -> coordinate(line, "y") > .55)
                    .filter(line -> Arrays.asList("continue", "next", "got it", "let's go", "let’s go")
                            .contains(label(line)))
                    .findFirst().orElse(null);
        }
        if (action == null && (question.contains("?")
                || question.matches(".*\\b(?:how|what|which|when|where|why|do you|have you|are you)\\b.*"))) {
            // These WebView answer cards place their labels to the left of the question.
            action = lines.stream().filter(line -> coordinate(line, "x") < .42
                            && coordinate(line, "y") >= .23 && coordinate(line, "y") < .83
                            && label(line).matches(".*\\p{L}{2}.*"))
                    .min(Comparator.comparingDouble(line -> coordinate(line, "y")))
                    .orElse(null);
        }
        if (action == null) return false;

        tap(coordinate(action, "x"), coordinate(action, "y"));
        return true;
    }

    private void tap(double x, double y) {
        Dimension size = driver.manage().window().getSize();
        Map<String, Integer> tap = new HashMap<>();
        tap.put("x", (int) Math.round(size.width * x));
        tap.put("y", (int) Math.round(size.height * y));
        driver.executeScript("mobile: clickGesture", tap);
    }

    private static Map<String, Object> firstMatching(List<Map<String, Object>> lines, String... choices) {
        return lines.stream().filter(line -> Arrays.asList(choices).contains(label(line))).findFirst().orElse(null);
    }

    private static String label(Map<String, Object> line) {
        return ((String) line.get("text")).trim().toLowerCase(Locale.ROOT);
    }

    private static double coordinate(Map<String, Object> line, String axis) {
        return ((Number) line.get(axis)).doubleValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> read() {
        Path screenshot = null;
        Path output = null;
        Process process = null;
        try {
            screenshot = Files.createTempFile("coach-questionnaire-", ".png");
            output = Files.createTempFile("coach-questionnaire-", ".json");
            Files.write(screenshot, driver.getScreenshotAs(OutputType.BYTES));
            process = new ProcessBuilder(executable.toString(), screenshot.toString())
                    .redirectOutput(output.toFile()).start();
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("Questionnaire text reader did not finish successfully");
            }
            return new Json().toType(new String(Files.readAllBytes(output), StandardCharsets.UTF_8), List.class);
        } catch (IOException error) {
            throw new IllegalStateException("Could not read the questionnaire screenshot", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Questionnaire text reader was interrupted", error);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            deleteTemporary(screenshot);
            deleteTemporary(output);
        }
    }

    private static void deleteTemporary(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException error) {
            throw new IllegalStateException("Could not remove a temporary questionnaire artifact", error);
        }
    }
}
