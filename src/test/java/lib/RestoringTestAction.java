package lib;

import java.util.Objects;

/** Runs a mutating scenario with mandatory restoration and preserves its original failure. */
public final class RestoringTestAction {
    private RestoringTestAction() {
    }

    @FunctionalInterface
    public interface CheckedAction {
        void run() throws Exception;
    }

    public static void run(CheckedAction scenario, CheckedAction restore) throws Exception {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(restore, "restore");
        Throwable scenarioFailure = null;
        try {
            scenario.run();
        } catch (Exception | Error failure) {
            scenarioFailure = failure;
            throw failure;
        } finally {
            try {
                restore.run();
            } catch (Exception | Error restoreFailure) {
                if (scenarioFailure == null) throw restoreFailure;
                // A reused exception must not replace the scenario failure through self-suppression.
                if (scenarioFailure != restoreFailure) scenarioFailure.addSuppressed(restoreFailure);
            }
        }
    }
}
