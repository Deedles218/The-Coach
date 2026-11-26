package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ui.MainPageObject;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

/** Native controls observed in COA-9583 Recommendation, Secondary and Summary. */
public final class IOSProgramSelectionPageObject extends MainPageObject {
    public static final String RECOMMENDATION_TITLE = "SHOW MY JOURNEY";
    private static final String SECONDARY_TITLE = "id:What else would you like to work on?";
    private static final String SECONDARY_SCREEN = SECONDARY_TITLE;
    private static final String MAIN_PICKER = "id:Choose your main journey";
    private static final String SUMMARY_TITLE = "id:Your plan is ready";
    private static final String SECONDARY_CONTINUE = "xpath://XCUIElementTypeButton["
            + "starts-with(@name,'Continue ') or @name='Continue']";
    private static final String CARD_TITLES = "xpath://XCUIElementTypeButton[@name='LEARN MORE']"
            + "/ancestor::XCUIElementTypeOther[.//XCUIElementTypeStaticText["
            + "following-sibling::XCUIElementTypeStaticText and not(preceding-sibling::XCUIElementTypeStaticText)]][1]"
            + "//XCUIElementTypeStaticText[following-sibling::XCUIElementTypeStaticText"
            + " and not(preceding-sibling::XCUIElementTypeStaticText)]";

    public IOSProgramSelectionPageObject(RemoteWebDriver driver) { super(driver); }

    public void activatePreparedApplication() {
        new iOSCoachFlowPageObject(driver).activateAppIfPossible();
        createWait(15).withMessage("Prepared Coach application did not reach the foreground").until(ignored -> {
            Map<?, ?> active = (Map<?, ?>) driver.executeScript("mobile: activeAppInfo", java.util.Collections.emptyMap());
            return lib.Platform.getInstance().getIOSBundleId().equals(active.get("bundleId"));
        });
    }

    public void assertRecommendation(Map<String, Object> data, String expectedId) {
        Assert.assertEquals("Wrong recommendation for the prepared answers", expectedId, data.get("program_id"));
        Assert.assertEquals("Recommendation is not marked recommended", true, data.get("is_recommended"));
        openRecommendation();
        waitForElementVisible("id:" + data.get("title"), "Recommendation is not open", 30);
        Assert.assertFalse("The old recommendation header must be on the preceding interstitial",
                isElementPresent("id:Based on your answers, we recommend starting here"));
        assertRecommendationActionsFixed();
        assertText((String) data.get("title"));
        for (String key : new String[]{"tagline", "description"}) {
            Object text = data.get(key);
            if (text instanceof String && !((String) text).trim().isEmpty()) assertText((String) text);
        }
        if (data.get("benefits") instanceof List) {
            for (Object text : (List<?>) data.get("benefits")) if (text instanceof String && !((String) text).trim().isEmpty()) assertText((String) text);
        }
        if (data.get("stats") instanceof Map) {
            Map<?, ?> stats = (Map<?, ?>) data.get("stats");
            for (String key : new String[]{"duration_days", "lessons_count", "min_per_day_label"}) {
                Object value = stats.get(key);
                if (value != null && !value.toString().trim().isEmpty()) {
                    String text = value instanceof Number && ((Number) value).doubleValue() == ((Number) value).longValue()
                            ? String.valueOf(((Number) value).longValue()) : value.toString();
                    assertText(text);
                }
            }
        }
        if (data.get("coach_note") instanceof Map) {
            Map<?, ?> note = (Map<?, ?>) data.get("coach_note");
            for (String key : new String[]{"quote", "coach_name", "coach_title"}) {
                Object value = note.get(key);
                if (value instanceof String && !((String) value).trim().isEmpty()) assertText((String) value);
            }
        }
    }

    public void openMainPicker() {
        openRecommendation();
        scrollToVisible("id:CHOOSE A DIFFERENT JOURNEY");
        waitForElementAndClick("id:CHOOSE A DIFFERENT JOURNEY", "Choose a different journey is unavailable", 10);
        waitForElementVisible(MAIN_PICKER, "Change Program did not open the main picker", 15);
    }

    public void assertMainCatalog(List<Map<String, Object>> catalog, String recommendedId) {
        waitForElementVisible(MAIN_PICKER, "Main picker is not open", 10);
        Set<String> expectedTitles = new LinkedHashSet<>();
        for (Map<String, Object> row : catalog) {
            String title = (String) row.get("title");
            expectedTitles.add(title);
            Assert.assertEquals("Catalog recommendation differs from the prepared answers",
                    recommendedId.equals(row.get("program_id")), row.get("is_recommended"));
            String badge = "xpath:" + card(title) + "//XCUIElementTypeStaticText[@name='RECOMMENDED']";
            Assert.assertEquals("Wrong Recommended badge on " + title,
                    recommendedId.equals(row.get("program_id")), isElementPresent(badge));
        }
        Assert.assertEquals("Main picker differs from the catalog", expectedTitles, new LinkedHashSet<>(cardTitles()));
        String recommended = (String) catalog.stream().filter(p -> recommendedId.equals(p.get("program_id")))
                .findFirst().orElseThrow(() -> new AssertionError("Recommended program is absent")).get("title");
        Assert.assertEquals("The recommendation must be the first card", recommended, cardTitles().get(0));
        Assert.assertTrue("The recommendation must be preselected", selected(recommended));
        assertElementDisabled("id:CHANGE MAIN JOURNEY", "Change must be disabled for the recommendation", 10);
        waitForElementEnabled("id:GO BACK", "Main picker has no Go back action", 10);
    }

    public void chooseMain(String title) {
        waitForElementVisible(MAIN_PICKER, "Main picker is not open", 10);
        String target = "xpath:" + card(title) + "//XCUIElementTypeStaticText[@name=" + Quotes.escape(title) + "]";
        scrollToVisible(target);
        waitForElementAndClick(target, "Cannot choose Main Journey", 10);
        waitForElementAndClick("id:CHANGE MAIN JOURNEY", "Main picker confirmation is unavailable", 10);
        openSecondaryAfterMain(title);
    }

    public void returnToRecommendation() {
        waitForElementAndClick("id:GO BACK", "Main picker has no Go back action", 10);
        waitForElementNotVisible(MAIN_PICKER, "Go back did not dismiss the main picker", 10);
        createWait(10).withMessage("Go back did not restore Recommendation")
                .until(ignored -> isElementPresent("id:SOUNDS GOOD") && isElementPresent("id:CHOOSE A DIFFERENT JOURNEY"));
    }

    public void start(String main) {
        waitForElementVisible(SUMMARY_TITLE, "Summary is not open", 10);
        waitForElementVisible("id:" + main, "Summary has the wrong Main Journey", 10);
        waitForElementAndClick("id:GO TO MY DAILY PLAN", "Summary has no Go to my Daily Plan action", 10);
        waitForElementNotVisible(SUMMARY_TITLE, "Start did not dismiss Summary", 30);
    }

    public void finishFirstVisitIntroduction() {
        // Reuse the normal first-visit flow, stopping as soon as Today appears.
        // In particular, do not consume the selector's first tooltip with its OK button.
        new iOSOnboardingPageObject(driver).closePaywallsWithoutConsumingSlides(new String[]{"Today"});
    }

    public void closeOptionalStartupPrompts() {
        activatePreparedApplication();
        new iOSOnboardingPageObject(driver).closePaywallsWithoutConsumingSlides(new String[]{
                RECOMMENDATION_TITLE, "SOUNDS GOOD", "CHOOSE ADDITIONAL JOURNEYS",
                "Your plan is ready", "Tap to explore more programs", "To switch back, tap here"});
    }

    public void assertProgramSelectionAbsent() {
        for (String screen : new String[]{"SHOW MY JOURNEY", "SOUNDS GOOD", "Choose your main journey",
                "CHOOSE ADDITIONAL JOURNEYS", "What else would you like to work on?", "Your plan is ready"}) {
            Assert.assertFalse("Program selection repeated unexpectedly: " + screen, isElementPresent("id:" + screen));
        }
    }

    public void waitForFirstDailyPlan(String main) {
        final boolean[] enrolled = {false};
        boolean expectsEnrollment = System.getenv("COACH_PROGRAM_SELECTION_NEW_EMAIL") != null
                && !System.getenv("COACH_PROGRAM_SELECTION_NEW_EMAIL").trim().isEmpty();
        createWait(90).withMessage("First Daily Plan is blocked by an unprepared onboarding fixture").until(ignored -> {
            String mandatory = "id:ENTER YOUR EMAIL TO SYNC YOUR PROGRESS";
            if (isElementVisible(mandatory)) {
                Assert.assertFalse("Mandatory email reappeared after successful confirmation", enrolled[0]);
                String email = System.getenv("COACH_PROGRAM_SELECTION_NEW_EMAIL");
                String otp = System.getenv("COACH_PROGRAM_SELECTION_NEW_OTP");
                Assert.assertTrue("Supply the approved free QA email and OTP for first-visit enrollment",
                        email != null && !email.trim().isEmpty() && otp != null && !otp.trim().isEmpty());
                Assert.assertTrue("New-address enrollment cannot replace a registered fixture email",
                        System.getenv("COACH_PROGRAM_SELECTION_EMAIL") == null
                                || System.getenv("COACH_PROGRAM_SELECTION_EMAIL").trim().isEmpty());
                try {
                    ProgramSelectionApi.assertDedicatedProfile(System.getenv("COACH_PROGRAM_SELECTION_CASE"));
                    ProgramSelectionState before = ProgramSelectionApi.read();
                    iOSEmailEnforcementPageObject.bindNewAddress(driver, email, otp);
                    ProgramSelectionState after = ProgramSelectionApi.read();
                    after.assertSameRoles(before);
                    after.assertSameProgress(before);
                    Assert.assertEquals("QA enrollment changed Active Program", before.active(), after.active());
                    Assert.assertEquals("QA enrollment changed completed content", before.raw().get("achievements"), after.raw().get("achievements"));
                    enrolled[0] = true;
                } catch (Exception failure) {
                    throw new IllegalStateException("Cannot verify first-visit QA enrollment", failure);
                }
                return false;
            }
            if (enrolled[0] && isElementVisible("id:My programs")) {
                new IOSMyProgramsPageObject(driver).closeProgramSelector();
            }
            return (!expectsEnrollment || enrolled[0]) && (isElementVisible("id:" + main) && isElementVisible("id:Today")
                    || isElementVisible("id:Tap to explore more programs"));
        });
    }

    @Step("Accept the recommendation and open Secondary Programs")
    public void acceptRecommendation(String expectedMain) {
        openRecommendation();
        waitForElementPresent("id:" + expectedMain, "Recommendation has the wrong program", 10);
        scrollToVisible("id:SOUNDS GOOD");
        waitForElementAndClick("id:SOUNDS GOOD", "Sounds Good is unavailable", 10);
        openSecondaryAfterMain(expectedMain);
    }

    private void openRecommendation() {
        if (isElementVisible("id:SHOW MY JOURNEY")) {
            waitForElementAndClick("id:SHOW MY JOURNEY", "First interstitial has no action", 10);
            waitForElementNotVisible("id:SHOW MY JOURNEY", "First interstitial remained open", 15);
        }
    }

    private void openSecondaryAfterMain(String main) {
        waitForElementVisible("id:CHOOSE ADDITIONAL JOURNEYS", "Main confirmation did not open interstitial 2", 20);
        waitForElementVisible("xpath://XCUIElementTypeStaticText[contains(@name,'Your main journey is set:')"
                + " and contains(@name," + Quotes.escape(main) + ")]", "Interstitial 2 has the wrong Main Journey", 10);
        waitForElementAndClick("id:CHOOSE ADDITIONAL JOURNEYS", "Interstitial 2 has no action", 10);
        waitForElementVisible(SECONDARY_TITLE, "Interstitial 2 did not open Secondary", 20);
        Assert.assertFalse("Main Journey is offered as Secondary", cardTitles().contains(main));
    }

    public void assertRecommendationActionsFixed() {
        Rectangle accept = waitForElementVisible("id:SOUNDS GOOD", "Recommendation CTA is below the fold", 10).getRect();
        Rectangle change = waitForElementVisible("id:CHOOSE A DIFFERENT JOURNEY", "Alternative CTA is below the fold", 10).getRect();
        int bottom = driver.manage().window().getSize().height;
        Assert.assertTrue("Recommendation actions are not anchored at the bottom",
                accept.y > bottom / 2 && change.y >= accept.y + accept.height
                        && change.y + change.height <= bottom && bottom - change.y - change.height <= 120);
        swipeUpQuick();
        Assert.assertEquals("Sounds Good scrolls with the page", accept,
                waitForElementVisible("id:SOUNDS GOOD", "Sounds Good disappeared on scroll", 10).getRect());
        Assert.assertEquals("Choose a different journey scrolls with the page", change,
                waitForElementVisible("id:CHOOSE A DIFFERENT JOURNEY", "Alternative CTA disappeared on scroll", 10).getRect());
    }

    @Step("Read available Secondary card titles")
    public List<String> secondaryTitles() {
        waitForElementVisible(SECONDARY_SCREEN, "Secondary Programs is not open", 10);
        return cardTitles();
    }

    List<String> cardTitles() {
        List<String> names = new ArrayList<>();
        for (WebElement element : driver.findElements(getLocatorByString(CARD_TITLES))) {
            String name = element.getAttribute("name");
            if (name != null && !name.trim().isEmpty()) names.add(name.trim());
        }
        Assert.assertFalse("Secondary Programs has no readable program cards", names.isEmpty());
        Assert.assertEquals("Program list contains duplicate titles", names.size(), new LinkedHashSet<>(names).size());
        return names;
    }

    @Step("Select only the expected Secondary Programs")
    public void selectOnly(List<String> expected) {
        List<String> available = secondaryTitles();
        Assert.assertTrue("Expected Secondary Programs are absent", available.containsAll(expected));
        for (String title : available) {
            if (selected(title) != expected.contains(title)) toggle(title);
        }
        assertContinueCount(expected.size());
    }

    @Step("Toggle Secondary Program: {title}")
    public void toggle(String title) {
        boolean before = selected(title);
        String target = "xpath:" + card(title) + "//XCUIElementTypeStaticText[@name=" + Quotes.escape(title) + "]";
        scrollToVisible(target);
        waitForElementAndClick(target, "Cannot toggle the program card", 10);
        createWait(10).ignoring(StaleElementReferenceException.class)
                .withMessage("Program checkmark did not change: " + title)
                .until(ignored -> selected(title) != before);
    }

    @Step("Verify Secondary Continue reflects {expected} selected programs")
    public void assertContinueCount(int expected) {
        String[] controls = expected == 0 ? new String[]{"id:Continue", "id:CONTINUE"}
                : new String[]{"id:Continue " + expected + " selected"};
        WebElement action = waitForFirstElementPresent(controls,
                "Continue count differs from the selected program cards", 10);
        Assert.assertTrue("Continue is not visible", action.isDisplayed());
        Assert.assertEquals("Continue enabled state differs from selection", expected > 0, isElementEnabled(action));
        waitForElementEnabled("id:SKIP", "Secondary has no separate Skip action", 10);
        int selected = 0;
        for (String title : secondaryTitles()) if (selected(title)) selected++;
        Assert.assertEquals("Card checkmarks disagree with Continue", expected, selected);
    }

    public void assertSelection(String main, List<String> expected) {
        List<String> available = secondaryTitles();
        Assert.assertFalse("Main Journey appears in Secondary", available.contains(main));
        Assert.assertTrue("Expected Secondary is absent", available.containsAll(expected));
        for (String title : available) {
            Assert.assertEquals("Wrong checkmark on " + title, expected.contains(title), selected(title));
        }
        assertContinueCount(expected.size());
    }

    public void openCardDetails(String title) { cardAction(title, "LEARN MORE"); }
    public void openCardTrailer(String title) { cardAction(title, "▶  WATCH TRAILER"); }

    private void cardAction(String title, String action) {
        String locator = "xpath:" + card(title) + "//XCUIElementTypeButton[@name=" + Quotes.escape(action) + "]";
        scrollToVisible(locator);
        waitForElementAndClick(locator, "Cannot open " + action + " for " + title, 10);
    }

    @Step("Save Secondary selection and open Summary")
    public void openSummary() {
        int selected = 0;
        for (String title : secondaryTitles()) if (selected(title)) selected++;
        assertContinueCount(selected);
        waitForElementAndClick(selected == 0 ? "id:SKIP" : SECONDARY_CONTINUE,
                "Secondary save action is unavailable", 10);
        waitForElementVisible(SUMMARY_TITLE, "Secondary selection did not open Summary", 20);
    }

    @Step("Verify Summary contains exactly the chosen programs")
    public void assertSummary(String main, List<String> selected, List<String> available) {
        waitForElementVisible(SUMMARY_TITLE, "Summary is not open", 10);
        waitForElementVisible("id:MAIN JOURNEY", "Summary has no Main Journey badge", 10);
        waitForElementEnabled("id:GO TO MY DAILY PLAN", "Summary has no Go to my Daily Plan action", 10);
        Assert.assertFalse("Summary must not contain Edit My Programs", isElementPresent("id:Edit My Programs"));
        assertVisibleTitleCount(main, 1);
        Assert.assertEquals("Summary must have one Added badge per Additional Journey", selected.size(),
                driver.findElements(By.xpath("//XCUIElementTypeStaticText[@name='ADDED']")).size());
        for (String title : available) assertVisibleTitleCount(title, selected.contains(title) ? 1 : 0);
    }

    boolean selected(String title) {
        // Main uses a radio dot: a separate image overlay precedes the marker,
        // whose direct child is the dot. An unselected radio has nested fill layers.
        String marker = isElementPresent(MAIN_PICKER)
                ? "//XCUIElementTypeImage/following-sibling::XCUIElementTypeOther[1][not(*)]"
                    + "/following-sibling::XCUIElementTypeOther[1]/XCUIElementTypeOther[not(*)]"
                : "//XCUIElementTypeStaticText[@name='✓']";
        List<WebElement> marks = driver.findElements(By.xpath(card(title) + marker));
        Assert.assertTrue("A program card has multiple checkmarks", marks.size() <= 1);
        return marks.size() == 1;
    }

    static String card(String title) {
        return "//XCUIElementTypeStaticText[@name=" + Quotes.escape(title)
                + "]/ancestor::XCUIElementTypeOther[.//XCUIElementTypeButton[@name='LEARN MORE']][1]";
    }

    public void assertText(String text) {
        Assert.assertTrue("Expected program text is empty", text != null && !text.trim().isEmpty());
        scrollToVisible("id:" + text);
        waitForElementVisible("id:" + text, "Program text is absent", 10);
    }

    private void assertVisibleTitleCount(String title, int expected) {
        Assert.assertEquals("Unexpected Summary entry count for " + title, expected,
                driver.findElements(By.xpath("//XCUIElementTypeStaticText[@name="
                        + Quotes.escape(title) + " and @visible='true']")).size());
    }

    private void scrollToVisible(String locator) {
        for (int attempt = 0; attempt < 12; attempt++) {
            if (isElementVisible(locator)) return;
            List<WebElement> targets = driver.findElements(getLocatorByString(locator));
            if (!targets.isEmpty() && targets.get(0).getRect().y < 100) {
                driver.executeScript("mobile: swipe", java.util.Collections.singletonMap("direction", "down"));
            } else swipeUpQuick();
        }
        Assert.fail("Program selection control remains offscreen: " + locator);
    }
}
