package tests;

import io.qameta.allure.*;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ios.iOSOnboardingPageObject;
import lib.ui.ios.iOSOnboardingSlidesPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;

import java.util.ArrayList;
import java.util.List;

@Epic("Product onboarding")
@Feature("COA-9145 / COA-9604")
public class OnboardingSlidesTests extends CoreTestCase {
    @Test
    @Issues({@Issue("COA-9428"), @Issue("COA-9427"), @Issue("COA-9429"),
            @Issue("COA-9430"), @Issue("COA-9432"), @Issue("COA-9604")})
    @DisplayName("Product slides: configured content, swipe navigation, media, CTA and one-time completion")
    @Description("Uses the active goal-filtered configuration. Requirements COA-9145 supersede conflicting Skip/cyclic expectations in TMS, as confirmed by the owner. A prepared run requires the first uncompleted slide; a normal run starts from a fresh install.")
    public void testNewUserSlidesNavigationMediaAndCompletion() throws Exception {
        requireIOSPlatform();
        try (iOSOnboardingSlidesPageObject slides = new iOSOnboardingSlidesPageObject(driver)) {
            iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
            if (!Boolean.getBoolean("onboarding.slides.prepared")) {
                onboarding.completeNewUserJourneyToPaywall(slides.goal());
            }
            slides.startMediaRecording();
            onboarding.closePaywallsBeforeSlides(slides.header(1));
            Rectangle firstButton = slides.assertSlide(1);
            List<Throwable> mediaFailures = new ArrayList<>();
            for (int page = 1; page <= slides.count(); page++) {
                Assert.assertEquals("CTA position changed on slide " + page, firstButton, slides.assertSlide(page));
                try {
                    slides.assertMedia(page);
                } catch (AssertionError | TimeoutException failure) {
                    mediaFailures.add(failure);
                    slides.captureMediaFailure(page);
                    Allure.addAttachment("Media failure on slide " + page, failure.toString());
                }
                if (page < slides.count()) slides.swipe("left", page + 1);
            }
            slides.finishMediaRecording();
            for (int page = slides.count() - 1; page >= 1; page--) slides.swipe("right", page);
            // Exercise the CTA separately from swipes; only the final CTA may close the carousel.
            for (int page = 1; page <= slides.count(); page++) {
                slides.assertSlide(page);
                slides.tapButton(page);
                if (page < slides.count()) slides.assertSlide(page + 1);
            }
            onboarding.waitForToday();
            slides.assertAbsent();
            closeAndReopenCoachApplication();
            onboarding.closePaywallsWithoutConsumingSlides(slides.headers());
            slides.assertAbsent();
            onboarding.waitForToday();
            slides.assertAbsent();
            if (!mediaFailures.isEmpty()) {
                AssertionError failure = new AssertionError(mediaFailures.size() + " media checks failed; "
                        + "navigation and completion continued. First failure: " + mediaFailures.get(0).getMessage());
                for (Throwable mediaFailure : mediaFailures) failure.addSuppressed(mediaFailure);
                throw failure;
            }
        }
    }

    @Test
    @Issue("COA-9431")
    @DisplayName("Existing user with Daily Plan history sees Today without product onboarding")
    public void testExistingProgressUserDoesNotSeeSlides() throws Exception {
        requireIOSPlatform();
        iOSOnboardingSlidesPageObject slides = new iOSOnboardingSlidesPageObject(driver);
        TestData.TestAccount account = TestData.existingProgressAccount();
        new iOSOnboardingPageObject(driver).closePaywallsAndPopups();
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        coach.openToday();
        new iOSOnboardingPageObject(driver).waitForToday();
        slides.assertAbsent();
    }

    @Override
    protected void cleanupSessionForNextTest() {
        // Preserve the newly completed anonymous identity so restart persistence can be inspected.
        // The existing-user method performs its normal project cleanup.
        if (driver != null && !Boolean.getBoolean("onboarding.slides.preserveSession")) {
            super.cleanupSessionForNextTest();
        }
    }
}
