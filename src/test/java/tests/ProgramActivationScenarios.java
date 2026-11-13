package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ui.ios.IOSMyProgramsPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

@Epic("COA-9583 Program activation")
@Feature("My Programs in the ON group")
public class ProgramActivationScenarios extends CoreTestCase {
    private IOSMyProgramsPageObject today;

    @Before
    @Override
    public void setUp() throws Exception {
        requireIOSPlatform();
        Assert.assertTrue("Attach to an installed iOS build by bundle ID with noReset=true and fullReset=false",
                Platform.getInstance().isTestFlightAttachConfigured());
        PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
        today = new IOSMyProgramsPageObject(driver);
        today.openTodayTab();
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (today != null) today.returnToToday();
    }

    @Test
    @Issue("COA-9805")
    public void dismissingMyProgramsPreservesTheDailyPlan() {
        String activeProgram = today.getActiveProgramName();
        List<String> initialContent = dailyPlanContent();

        today.openProgramSelector();
        today.assertActiveProgramMarked(activeProgram);
        today.swipeSelectorDown();
        assertDailyPlanUnchanged(activeProgram, initialContent);

        today.openProgramSelector();
        today.assertActiveProgramMarked(activeProgram);
        today.closeProgramSelector();
        assertDailyPlanUnchanged(activeProgram, initialContent);
    }

    @Test
    @Issue("COA-9805")
    public void openingCatalogPreservesTheActiveProgram() {
        String activeProgram = today.getActiveProgramName();
        List<String> initialContent = dailyPlanContent();

        today.openProgramSelector();
        today.assertActiveProgramMarked(activeProgram);
        today.openCatalogFromSelector();
        today.returnToToday();
        assertDailyPlanUnchanged(activeProgram, initialContent);
    }

    private List<String> dailyPlanContent() {
        String firstLesson = today.getFirstDailyLessonTitle();
        Assert.assertFalse("The first lesson must have a readable title", firstLesson.isEmpty());
        return Arrays.asList(today.getCurrentDayLabel(), firstLesson);
    }

    private void assertDailyPlanUnchanged(String activeProgram, List<String> initialContent) {
        today.assertTodayTabIsSelected();
        Assert.assertEquals("The active program changed", activeProgram, today.getActiveProgramName());
        Assert.assertEquals("The stage or first lesson changed", initialContent, dailyPlanContent());
    }
}
