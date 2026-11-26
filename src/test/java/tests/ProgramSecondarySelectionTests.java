package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ProgramSelectionApi;
import lib.ui.ios.IOSProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Run each method on its own new QA profile after a successful test Paywall. */
@Epic("COA-9583 Program activation")
@Feature("Secondary selection and Summary")
public class ProgramSecondarySelectionTests extends CoreTestCase {
    private IOSProgramSelectionPageObject selection;
    private final String main = System.getProperty("coach.programSelection.main", "Sex Is a Skill");

    @Before
    @Override
    public void setUp() throws Exception {
        requireIOSPlatform();
        String bundle = Platform.getInstance().getIOSBundleId();
        boolean production = "com.vamapps.The-Coach".equals(bundle)
                || "com.vamapps.The-Coach-for-her".equals(bundle);
        Assert.assertTrue("Use preprod or explicitly approve the production QA run", bundle != null
                && (bundle.startsWith("com.vamapps.preprod.")
                || production && Boolean.getBoolean("coach.programSelection.allowProduction")));
        Assert.assertTrue("Preserve the installed iOS app", Platform.getInstance().isTestFlightAttachConfigured());
        PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
        selection = new IOSProgramSelectionPageObject(driver);
        selection.activatePreparedApplication();
        Assert.assertEquals("Bind the dedicated Secondary test profile to COA-9790", "COA-9790",
                System.getenv("COACH_PROGRAM_SELECTION_CASE"));
        String uid = System.getenv("COACH_PROGRAM_SELECTION_UID");
        Assert.assertTrue("A dedicated QA UID binding is required before saving Secondary",
                uid != null && uid.matches("[A-Za-z0-9_-]{20,128}"));
        String entry = System.getProperty("coach.programSelection.entry", "recommendation");
        Assert.assertTrue("Program selection entry must be recommendation or secondary",
                "recommendation".equals(entry) || "secondary".equals(entry));
        if ("recommendation".equals(entry)) {
            ProgramSelectionApi.assertFreshOnboardingProfile();
            selection.acceptRecommendation(main);
        } else {
            Assert.assertFalse("Prepared Secondary entry is preprod-only", production);
            ProgramSelectionApi.assertDedicatedProfile("COA-9790");
            ProgramSelectionApi.assertFreshSecondaryDraft(System.getProperty("coach.programSelection.mainId"));
            selection.waitForElementVisible("id:What else would you like to work on?", "Prepared Secondary chooser is absent", 15);
            selection.assertSelection(main, Collections.emptyList());
        }
    }

    @Override
    protected void cleanupSessionForNextTest() {
        // Preserve the new profile and its Summary for inspection. Each method
        // requires an independent fixture; teardown must not complete onboarding.
    }

    @Test
    @Issue("COA-9790")
    public void zeroSecondaryProgramsOmitAddedByYou() {
        List<String> available = selection.secondaryTitles();
        selection.selectOnly(Collections.emptyList());
        selection.openSummary();
        selection.assertSummary(main, Collections.emptyList(), available);
    }

    @Test
    @Issue("COA-9790")
    public void deselectingOneOfTwoProgramsUpdatesCounterAndSummary() {
        List<String> available = requireTwoSecondaryPrograms();
        String first = available.get(0), second = available.get(1);
        selection.selectOnly(Collections.emptyList());
        selection.toggle(first);
        selection.assertContinueCount(1);
        selection.toggle(second);
        selection.assertContinueCount(2);
        selection.toggle(second);
        selection.assertContinueCount(1);
        selection.openSummary();
        selection.assertSummary(main, Collections.singletonList(first), available);
    }

    @Test
    @Issue("COA-9790")
    public void twoSecondaryProgramsAppearExactlyOnceInSummary() {
        List<String> available = requireTwoSecondaryPrograms();
        List<String> chosen = Arrays.asList(available.get(0), available.get(1));
        selection.selectOnly(chosen);
        selection.openSummary();
        selection.assertSummary(main, chosen, available);
    }

    private List<String> requireTwoSecondaryPrograms() {
        List<String> available = selection.secondaryTitles();
        Assert.assertTrue("COA-9790 requires two Secondary programs besides Main Journey", available.size() >= 2);
        return available;
    }
}
