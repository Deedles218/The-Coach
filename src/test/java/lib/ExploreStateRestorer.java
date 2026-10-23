package lib;

import io.qameta.allure.Allure;
import lib.ui.DailyPlanPageObject;
import lib.ui.android.AndroidProgramSelectorPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Objects;
import java.util.function.Consumer;

/** Preserves the caller's Today selection without completing activities or resetting progress. */
public final class ExploreStateRestorer {
    private final DailyPlanPageObject today;
    private final Consumer<String> selectProgram;
    private final Consumer<String> selectDay;
    private final String program;
    private final String day;
    private final String progress;

    private ExploreStateRestorer(DailyPlanPageObject today, Consumer<String> selectProgram,
                                 Consumer<String> selectDay) {
        this.today = Objects.requireNonNull(today, "today");
        this.selectProgram = Objects.requireNonNull(selectProgram, "selectProgram");
        this.selectDay = Objects.requireNonNull(selectDay, "selectDay");
        today.openTodayTab();
        program = requireValue("program", today.getActiveProgramName());
        day = requireValue("day", today.getCurrentDayLabel());
        progress = requireValue("progress", today.getProgramProgressValue());
        Allure.addAttachment("Explore original Today state", state(program, day, progress));
    }

    public static ExploreStateRestorer capture(RemoteWebDriver driver) {
        if (Platform.getInstance().isIOS()) {
            iOSProgramSelectorPageObject today = new iOSProgramSelectorPageObject(driver);
            return capture(today, today::ensureProgramSelected, today::selectObservedDay);
        }
        AndroidProgramSelectorPageObject today = new AndroidProgramSelectorPageObject(driver);
        return capture(today, title -> {
            today.openProgramSelector();
            today.selectProgram(title);
        }, ignored -> {
            // Keep Android's existing contract: navigation must not change its selected stage.
        });
    }

    /** Explicit selection actions keep restoration on the existing platform Page Objects. */
    public static ExploreStateRestorer capture(DailyPlanPageObject today,
                                               Consumer<String> selectProgram,
                                               Consumer<String> selectDay) {
        return new ExploreStateRestorer(today, selectProgram, selectDay);
    }

    public void restoreAfter(RestoringTestAction.CheckedAction diagnostics,
                             RestoringTestAction.CheckedAction closeDestination) throws Exception {
        RestoringTestAction.run(() -> RestoringTestAction.run(diagnostics, closeDestination), this::restore);
    }

    public void restore() {
        today.openTodayTab();
        if (!program.equals(today.getActiveProgramName())) selectProgram.accept(program);
        // Do not navigate days in the wrong program if program restoration failed silently.
        Assert.assertEquals("Explore did not restore the original program", program, today.getActiveProgramName());
        if (!day.equals(today.getCurrentDayLabel())) selectDay.accept(day);
        String restoredProgram = today.getActiveProgramName();
        String restoredDay = today.getCurrentDayLabel();
        String restoredProgress = today.getProgramProgressValue();
        Allure.addAttachment("Explore restored Today state", state(restoredProgram, restoredDay, restoredProgress));
        Assert.assertEquals("Explore did not preserve the original program", program, restoredProgram);
        Assert.assertEquals("Explore did not restore the original day/stage", day, restoredDay);
        Assert.assertEquals("Explore changed the original progress; never reset progress to hide this failure",
                progress, restoredProgress);
    }

    private static String requireValue(String field, String value) {
        Assert.assertTrue("Cannot isolate Explore: Today " + field + " is empty",
                value != null && !value.trim().isEmpty());
        return value;
    }

    private static String state(String program, String day, String progress) {
        return "program=" + program + "; day=" + day + "; progress=" + progress;
    }
}
