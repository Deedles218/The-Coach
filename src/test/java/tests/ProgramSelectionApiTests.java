package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.ProgramSelectionApi;
import org.junit.Ignore;
import org.junit.Test;

/** Each method uses the matching prepared preprod profile described in COA-9799. */
@Epic("COA-9583 Program activation")
@Feature("Program selection API validation")
@Ignore("COA-9799: API rejection checks deferred by the owner; no real-account execution")
public class ProgramSelectionApiTests {
    @Test
    @Issue("COA-9799")
    public void mainJourneyCannotBeSavedAsSecondary() throws Exception {
        ProgramSelectionApi.assertRejectedTransition("secondary-includes-main");
    }

    @Test
    @Issue("COA-9799")
    public void anUnaddedCatalogProgramCannotBecomeActive() throws Exception {
        ProgramSelectionApi.assertRejectedTransition("activate-unadded");
    }

    @Test
    @Issue("COA-9799")
    public void onboardingCannotCompleteWithoutMainJourney() throws Exception {
        ProgramSelectionApi.assertRejectedTransition("complete-without-main");
    }
}
