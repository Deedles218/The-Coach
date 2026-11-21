package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ui.MainPageObject;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

/** Native controls observed in COA-9583 Recommendation, Secondary and Summary. */
public final class IOSProgramSelectionPageObject extends MainPageObject {
    public static final String RECOMMENDATION_TITLE = "Based on your answers, we recommend starting here";
    private static final String SECONDARY_TITLE = "id:What else would you like to work on?";
    private static final String SECONDARY_SCREEN = "xpath://XCUIElementTypeStaticText["
            + "@name='What else would you like to work on?' or @name='Edit my programs']";
    private static final String MAIN_PICKER = "id:Choose your main journey";
    private static final String SUMMARY_TITLE = "id:Your plan is ready";
    private static final String SECONDARY_CONTINUE = "xpath://XCUIElementTypeButton["
            + "@name='CONTINUE' or starts-with(@name,'Continue ') or @name='Continue']";
    private static final String CARD_TITLES = "xpath://XCUIElementTypeButton[@name='LEARN MORE']"
            + "/ancestor::XCUIElementTypeOther[.//XCUIElementTypeStaticText["
            + "following-sibling::XCUIElementTypeStaticText and not(preceding-sibling::XCUIElementTypeStaticText)]][1]"
            + "//XCUIElementTypeStaticText[following-sibling::XCUIElementTypeStaticText"
            + " and not(preceding-sibling::XCUIElementTypeStaticText)]";

    public IOSProgramSelectionPageObject(RemoteWebDriver driver) { super(driver); }

    public void assertRecommendation(Map<String, Object> data, String expectedId) {
        Assert.assertEquals("Wrong recommendation for the prepared answers", expectedId, data.get("program_id"));
        Assert.assertEquals("Recommendation is not marked recommended", true, data.get("is_recommended"));
        waitForElementVisible("id:" + RECOMMENDATION_TITLE, "Recommendation is not open", 30);
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
        scrollToVisible("id:CHANGE PROGRAM");
        waitForElementAndClick("id:CHANGE PROGRAM", "Change Program is unavailable", 10);
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
    }

    public void chooseMain(String title) {
        waitForElementVisible(MAIN_PICKER, "Main picker is not open", 10);
        scrollToVisible("id:" + title);
        waitForElementAndClick("id:" + title, "Cannot choose Main Journey", 10);
        waitForFirstElementPresent(new String[]{SECONDARY_TITLE, "id:MAKE IT MY MAIN JOURNEY", "id:CONTINUE"},
                "Main choice opened neither its confirmation nor Secondary", 10);
        if (!isElementVisible(SECONDARY_TITLE)) {
            waitForFirstElementAndClick(new String[]{"id:MAKE IT MY MAIN JOURNEY", "id:CONTINUE"},
                    "Main picker confirmation is unavailable", 10);
        }
        waitForElementVisible(SECONDARY_TITLE, "Choosing Main did not open Secondary", 20);
        Assert.assertFalse("Main Journey is offered as Secondary", isElementPresent("id:" + title));
    }

    public void editSecondary() {
        waitForElementVisible(SUMMARY_TITLE, "Summary is not open", 10);
        waitForElementAndClick("id:Edit My Programs", "Edit My Programs is unavailable", 10);
        waitForElementVisible("id:Edit my programs", "Summary did not reopen Secondary", 15);
    }

    public void start(String main) {
        waitForElementVisible(SUMMARY_TITLE, "Summary is not open", 10);
        waitForElementAndClick("id:Start with " + main, "Start with has the wrong Main Journey", 10);
        waitForElementNotVisible(SUMMARY_TITLE, "Start did not dismiss Summary", 30);
    }

    public void finishFirstVisitIntroduction() {
        // Reuse the normal first-visit flow, stopping as soon as Today appears.
        // In particular, do not consume the selector's first tooltip with its OK button.
        new iOSOnboardingPageObject(driver).closePaywallsWithoutConsumingSlides(new String[]{"Today"});
    }

    public void closeOptionalStartupPrompts() {
        new iOSOnboardingPageObject(driver).closePaywallsWithoutConsumingSlides(new String[]{
                RECOMMENDATION_TITLE, "Your plan is ready", "Tap to explore more programs", "To switch back, tap here"});
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
        waitForElementPresent("id:" + RECOMMENDATION_TITLE, "Recommendation is not open", 30);
        waitForElementPresent("id:" + expectedMain, "Recommendation has the wrong program", 10);
        scrollToVisible("id:SOUNDS GREAT");
        waitForElementAndClick("id:SOUNDS GREAT", "Sounds Great is unavailable", 10);
        waitForElementVisible(SECONDARY_TITLE, "Sounds Great did not open Secondary", 20);
        Assert.assertFalse("Main Journey is offered as Secondary", isElementPresent("id:" + expectedMain));
    }

    @Step("Read available Secondary card titles")
    public List<String> secondaryTitles() {
        waitForElementVisible(SECONDARY_SCREEN, "Secondary Programs is not open", 10);
        return cardTitles();
    }

    private List<String> cardTitles() {
        Set<String> names = new LinkedHashSet<>();
        for (WebElement element : driver.findElements(getLocatorByString(CARD_TITLES))) {
            String name = element.getAttribute("name");
            if (name != null && !name.trim().isEmpty()) names.add(name.trim());
        }
        Assert.assertFalse("Secondary Programs has no readable program cards", names.isEmpty());
        return new ArrayList<>(names);
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
        scrollToVisible("id:" + title);
        waitForElementAndClick("id:" + title, "Cannot toggle the program card", 10);
        createWait(10).ignoring(StaleElementReferenceException.class)
                .withMessage("Program checkmark did not change: " + title)
                .until(ignored -> selected(title) != before);
    }

    @Step("Verify Secondary Continue reflects {expected} selected programs")
    public void assertContinueCount(int expected) {
        String[] controls = expected == 0 ? new String[]{"id:Continue", "id:CONTINUE"}
                : new String[]{"id:Continue " + expected + " selected"};
        WebElement action = waitForFirstElementEnabled(controls,
                "Continue count differs from the selected program cards", 10);
        Assert.assertTrue("Continue is not visible", action.isDisplayed());
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
        waitForElementAndClick(SECONDARY_CONTINUE, "Secondary Continue is unavailable", 10);
        waitForElementVisible(SUMMARY_TITLE, "Secondary selection did not open Summary", 20);
    }

    @Step("Verify Summary contains exactly the chosen programs")
    public void assertSummary(String main, List<String> selected, List<String> available) {
        waitForElementVisible(SUMMARY_TITLE, "Summary is not open", 10);
        waitForElementVisible("id:Main journey", "Summary has no Main Journey badge", 10);
        waitForElementEnabled("id:Start with " + main, "Start with has the wrong Main Journey", 10);
        assertVisibleTitleCount(main, 1);
        Assert.assertEquals("Added by You availability differs from selection",
                !selected.isEmpty(), isElementVisible("id:ADDED BY YOU"));
        for (String title : available) assertVisibleTitleCount(title, selected.contains(title) ? 1 : 0);
    }

    private boolean selected(String title) {
        List<WebElement> marks = driver.findElements(By.xpath(card(title) + "//XCUIElementTypeStaticText[@name='✓']"));
        Assert.assertTrue("A program card has multiple checkmarks", marks.size() <= 1);
        return marks.size() == 1;
    }

    private String card(String title) {
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
            swipeUpQuick();
        }
        Assert.fail("Program selection control remains offscreen: " + locator);
    }
}
