package lib.ui.android;

import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.MainPageObject;
import lib.ui.ProgramSwitchFeaturePageObject;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.Quotes;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import java.io.StringReader;

/** UI v3 controls observed on the reviewed Him 1.45.6 / Her 2.20.6 APKs. */
public class AndroidProgramSelectionPageObject extends MainPageObject {
    public static final String FIRST_INTERSTITIAL = "Based on your answers, we've chosen a journey for you.";
    private final String prefix = Platform.getInstance().getAndroidAppPackage() + ":id/";

    public AndroidProgramSelectionPageObject(RemoteWebDriver driver) { super(driver); }
    private String id(String name) { return "id:" + prefix + name; }
    private String text(String value) { return "xpath://*[@text=" + Quotes.escape(value) + "]"; }
    private String cardPath(String title) {
        return "//*[@resource-id='" + prefix + "cardRoot' and .//*[@resource-id='"
                + prefix + "tvTitle' and @text=" + Quotes.escape(title) + "]]";
    }

    @Step("Open the recommendation through the required first interstitial")
    public String showMyJourney() {
        waitForElementVisible(text(FIRST_INTERSTITIAL), "The first onboarding interstitial is absent", 30);
        Assert.assertEquals("Wrong interstitial CTA", "SHOW MY JOURNEY",
                waitForElementEnabled(id("btnAction"), "Show my journey is unavailable", 10).getText());
        waitForElementAndClick(id("btnAction"), "Cannot show the recommended journey", 10);
        waitForElementEnabled(id("btnSoundsGreat"), "Recommendation did not open", 20);
        Assert.assertEquals("Recommendation CTA must be Sounds good", "SOUNDS GOOD",
                waitForElementVisible(id("btnSoundsGreat"), "Sounds good is absent", 10).getText());
        Assert.assertEquals("Wrong change-Main CTA", "CHOOSE A DIFFERENT JOURNEY",
                waitForElementEnabled(id("btnChangeProgram"), "Change journey is unavailable", 10).getText());
        Assert.assertFalse("The removed Recommendation heading remains", isElementVisible(
                text("Based on your answers, we recommend starting here")));
        String title = id("tvProgramTitle");
        return waitForElementVisible(title, "Recommended program has no title", 10).getText();
    }

    @Step("Verify Recommendation actions remain pinned while content scrolls")
    public void assertRecommendationActionsPinned() {
        Rectangle accept = waitForElementVisible(id("btnSoundsGreat"), "Sounds good is absent", 10).getRect();
        Rectangle change = waitForElementVisible(id("btnChangeProgram"), "Change journey is absent", 10).getRect();
        Rectangle content = waitForElementVisible(id("scrollView"), "Recommendation content is absent", 10).getRect();
        Assert.assertTrue("Recommendation actions are not below its scrollable content", accept.y >= content.y + content.height);
        scroll("down");
        Assert.assertEquals("Sounds good moved with the content", accept,
                waitForElementVisible(id("btnSoundsGreat"), "Sounds good scrolled away", 10).getRect());
        Assert.assertEquals("Choose a different journey moved with the content", change,
                waitForElementVisible(id("btnChangeProgram"), "Change journey scrolled away", 10).getRect());
        rewind();
        Assert.assertFalse("Recommendation contains technical MOCK/TEST copy",
                driver.getPageSource().matches("(?s).*(\\[MOCK\\]|\\[TEST\\]).*"));
    }

    public void openMainPicker() {
        waitForElementAndClick(id("btnChangeProgram"), "Choose a different journey is absent", 10);
        waitForElementVisible(text("Choose your main journey"), "Main picker did not open", 15);
    }

    public void assertCurrentMainFirst(String main) {
        List<Card> cards = cards();
        Assert.assertEquals("Current Main must be first", main, cards.get(0).title);
        Assert.assertTrue("Current Main must be selected and Recommended", cards.get(0).selected && cards.get(0).recommended);
        Assert.assertEquals("Only one Main can be selected", 1L, cards.stream().filter(c -> c.selected).count());
        assertElementDisabled(id("btnContinue"), "Change main journey must be disabled for the current Main", 10);
        waitForElementEnabled(id("btnGoBack"), "Go back is absent", 10);
    }

    public void chooseMain(String title) {
        toggle(title);
        waitForElementEnabled(id("btnContinue"), "A different Main did not enable Change main journey", 10);
        Assert.assertEquals("Main picker must remain single-select", 1L, cards().stream().filter(c -> c.selected).count());
    }

    public void goBackFromMainPicker() {
        waitForElementAndClick(id("btnGoBack"), "Go back is unavailable", 10);
        waitForElementEnabled(id("btnSoundsGreat"), "Go back did not return to Recommendation", 15);
    }

    public void confirmMain(String title) {
        waitForElementAndClick(id("btnContinue"), "Change main journey is disabled", 10);
        continueThroughMainSet(title);
    }

    public void acceptRecommendation(String title) {
        waitForElementAndClick(id("btnSoundsGreat"), "Sounds good is unavailable", 10);
        continueThroughMainSet(title);
    }

    @Step("Verify the second interstitial uses the saved Main: {title}")
    private void continueThroughMainSet(String title) {
        waitForElementVisible(text("Your main journey is set:"), "Main-set interstitial is absent", 15);
        Assert.assertEquals("Main-set interstitial displays the wrong program", title,
                waitForElementVisible(id("tvProgramTitle"), "Saved Main title is absent", 10).getText());
        Assert.assertEquals("Wrong second interstitial CTA", "CHOOSE ADDITIONAL JOURNEYS",
                waitForElementEnabled(id("btnAction"), "Choose additional journeys is unavailable", 10).getText());
        waitForElementAndClick(id("btnAction"), "Cannot open Additional journeys", 10);
        waitForElementVisible(text("What else would you like to work on?"), "Secondary picker did not open", 15);
        Assert.assertFalse("Main is offered as Secondary", cardTitles().contains(title));
    }

    public List<String> cardTitles() { return cards().stream().map(c -> c.title).collect(Collectors.toList()); }

    public void selectOnly(List<String> expected) {
        List<Card> snapshot = cards();
        List<String> available = snapshot.stream().map(c -> c.title).collect(Collectors.toList());
        Assert.assertTrue("Requested Secondary is absent", available.containsAll(expected));
        for (Card card : snapshot) if (card.selected != expected.contains(card.title)) toggle(card.title);
        assertSelection(expected);
    }

    public void assertSelection(List<String> expected) {
        assertContinueCount(expected.size());
        Set<String> selected = cards().stream().filter(c -> c.selected).map(c -> c.title)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Assert.assertEquals("Wrong Secondary checkmarks", new LinkedHashSet<>(expected), selected);
    }

    public void toggle(String title) {
        WebElement mark = revealControl(title, "ivSelection");
        boolean before = "true".equals(mark.getAttribute("selected"));
        mark.click();
        createWait(10).ignoring(StaleElementReferenceException.class)
                .withMessage("Program selection did not change: " + title).until(ignored -> selected(title) != before);
    }

    private boolean selected(String title) {
        return "true".equals(revealControl(title, "ivSelection").getAttribute("selected"));
    }

    public void assertContinueCount(int count) {
        WebElement button = waitForElementVisible(id("btnContinue"), "Secondary Continue is absent", 10);
        Assert.assertEquals("Continue must be enabled only with at least one Secondary", count > 0, isElementEnabled(button));
        if (count > 0) waitForElementVisible(text(count + " selected"), "Wrong Secondary counter", 10);
        Assert.assertEquals("Secondary checkmarks disagree with the expected count", (long) count,
                cards().stream().filter(c -> c.selected).count());
    }

    public void openSummary() {
        long count = cards().stream().filter(c -> c.selected).count();
        if (count == 0) waitForElementAndClick(text("SKIP"), "A separate Skip is required for zero Secondary", 10);
        else waitForElementAndClick(id("btnContinue"), "Secondary Continue is unavailable", 10);
        waitForElementVisible(text("Your plan is ready"), "Summary did not open", 20);
    }

    @Step("Verify informational Summary and the selected journeys")
    public void assertSummary(String main, List<String> chosen, List<String> available) {
        rewind();
        waitForElementVisible(text("Your plan is ready"), "Summary is absent", 10);
        createWait(15).ignoring(StaleElementReferenceException.class)
                .withMessage("Summary did not load the expected journey rows").until(ignored -> {
                    return readSummaryRows(driver.getPageSource()).size() >= Math.min(2, 1 + chosen.size());
                });
        SortedMap<Integer, List<String>> rows = new TreeMap<>();
        for (int page = 0; ; page++) {
            collectSummaryRows(rows);
            if (!scroll("down")) { collectSummaryRows(rows); break; }
            Assert.assertTrue("Summary did not reach its end within 15 viewports", page < 14);
        }
        rewind();
        for (int index = 0; index < rows.size(); index++)
            Assert.assertTrue("Summary has a missing row position: " + index, rows.containsKey(index));
        List<String> titles = rows.values().stream().map(row -> row.get(0)).collect(Collectors.toList());
        List<String> expected = new ArrayList<>(); expected.add(main); expected.addAll(chosen);
        Assert.assertEquals("Summary contains missing, extra or duplicate journeys", expected.size(), titles.size());
        Assert.assertEquals("Main Journey must be first in Summary", main, titles.get(0));
        Assert.assertEquals("Summary has the wrong Secondary set", new LinkedHashSet<>(chosen),
                new LinkedHashSet<>(titles.subList(1, titles.size())));
        Assert.assertEquals("Summary contains duplicate journeys", titles.size(), new LinkedHashSet<>(titles).size());
        List<String> badges = rows.values().stream().map(row -> row.get(1)).collect(Collectors.toList());
        List<String> expectedBadges = new ArrayList<>(); expectedBadges.add("Main Journey");
        for (String ignored : chosen) expectedBadges.add("Added");
        Assert.assertEquals("Wrong Summary membership badges", expectedBadges, badges);
        Assert.assertFalse("Summary must not expose Edit My Programs", isElementPresent(text("Edit My Programs")));
        Assert.assertEquals("Wrong Summary CTA", "GO TO MY DAILY PLAN",
                waitForElementEnabled(id("btnAction"), "Daily Plan CTA is absent", 10).getText());
        Assert.assertEquals("Wrong Summary footer", "You can change your journeys at any time.",
                waitForElementVisible(id("tvFooter"), "Summary footer is absent", 10).getText());
    }

    private void collectSummaryRows(Map<Integer, List<String>> rows) {
        for (Map.Entry<Integer, List<String>> row : readSummaryRows(driver.getPageSource()).entrySet()) {
            List<String> previous = rows.putIfAbsent(row.getKey(), row.getValue());
            if (previous != null) Assert.assertEquals("A Summary row changed while scrolling", previous, row.getValue());
        }
    }

    private SortedMap<Integer, List<String>> readSummaryRows(String source) {
        try {
            SortedMap<Integer, List<String>> rows = new TreeMap<>();
            NodeList nodes = readDocument(source).getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element container = (Element) nodes.item(i);
                if (!(prefix + "containerPrograms").equals(container.getAttribute("resource-id"))) continue;
                NodeList children = container.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if (!(children.item(j) instanceof Element)) continue;
                    Element row = (Element) children.item(j);
                    String title = null, badge = null;
                    NodeList values = row.getElementsByTagName("*");
                    for (int k = 0; k < values.getLength(); k++) {
                        Element value = (Element) values.item(k);
                        if ((prefix + "tvTitle").equals(value.getAttribute("resource-id"))) title = value.getAttribute("text");
                        if ((prefix + "tvBadge").equals(value.getAttribute("resource-id"))) badge = value.getAttribute("text");
                    }
                    if (title != null && !title.isEmpty() && badge != null && !badge.isEmpty()) {
                        int index = Integer.parseInt(row.getAttribute("index"));
                        Assert.assertNull("Summary contains duplicate row positions", rows.put(index, Arrays.asList(title, badge)));
                    }
                }
            }
            return rows;
        } catch (Exception invalidSource) {
            throw new IllegalStateException("Cannot parse Android Summary rows", invalidSource);
        }
    }

    public void openDetailsAndReturn(String title) {
        revealControl(title, "tvLearnMore").click();
        waitForElementVisible(id("btnClose"), "Learn more did not open", 15);
        Assert.assertFalse("Informational Learn more must not select or launch a program", isElementPresent(id("btnAddAndStart")));
        waitForElementAndClick(id("btnClose"), "Cannot close Learn more", 10);
        waitForElementVisible(id("rvPrograms"), "Learn more did not return to the picker", 15);
    }

    public void startDailyPlan(String main) {
        waitForElementAndClick(id("btnAction"), "Go to my daily plan is unavailable", 10);
        closeStartupPrompts();
        new ProgramSwitchFeaturePageObject(driver).dismissProgramTooltipsIfPresent();
        if (isElementVisible(id("containerMain"))) driver.navigate().back();
        Assert.assertEquals("Summary opened the wrong Daily Plan", main, new AndroidDailyPlanPageObject(driver).getActiveProgramName());
    }

    public void closeStartupPrompts() {
        new AndroidOnboardingPageObject(driver).closePaywallsAndPopupsUntil(() -> isElementVisible(id("nav_graph_daily")));
        waitForElementVisible(id("tvDaysNumber"), "Daily Plan is absent", 30);
    }

    public void assertAddCount(int count) {
        Assert.assertEquals("All Programs has the wrong add counter", count == 0 ? "ADD PROGRAM" : "ADD PROGRAM (" + count + ")",
                waitForElementVisible(id("tvContinue"), "Add Program counter is absent", 10).getText());
        Assert.assertEquals("Add Program has the wrong enabled state", count > 0,
                isElementEnabled(waitForElementVisible(id("btnContinue"), "Add Program is absent", 10)));
    }

    public void addSelectedPrograms(int count) {
        assertAddCount(count);
        waitForElementAndClick(id("btnContinue"), "Cannot add selected programs", 10);
        waitForElementVisible(id("containerMain"), "Adding programs must open the expanded My programs selector", 20);
    }

    public void assertCatalogMembership(Set<String> added) {
        List<Card> cards = cards();
        List<String> problems = new ArrayList<>();
        Set<String> titles = cards.stream().map(c -> c.title).collect(Collectors.toSet());
        for (String title : added) if (!titles.contains(title)) problems.add(title + ": added program is absent");
        boolean addedSeen = false;
        for (Card card : cards) {
            if (added.contains(card.title)) {
                addedSeen = true;
                if (card.selectionControl) problems.add(card.title + ": added card has a selection control");
                if (!card.added) problems.add(card.title + ": Added badge is absent");
            } else {
                if (addedSeen) problems.add(card.title + ": unadded program appears after an added program");
                if (!card.selectionControl) problems.add(card.title + ": unadded card has no selection control");
                if (card.added) problems.add(card.title + ": unadded program has an Added badge");
            }
        }
        Assert.assertTrue("All Programs v3: " + String.join("; ", problems), problems.isEmpty());
    }

    public List<Card> cards() {
        rewind();
        Map<String, Card> cards = new LinkedHashMap<>();
        for (int page = 0; page < 15; page++) {
            collectCards(cards);
            if (!scroll("down")) { collectCards(cards); rewind(); return new ArrayList<>(cards.values()); }
        }
        Assert.fail("Program grid did not reach its end within 15 viewports"); return Collections.emptyList();
    }

    private void collectCards(Map<String, Card> result) {
        List<Card> visible = createWait(10).until(ignored -> {
            List<Card> values = readCards(driver.getPageSource(), prefix);
            return values.isEmpty() ? null : values;
        });
        for (Card card : visible) result.putIfAbsent(card.title, card);
    }

    /** One atomic source avoids mixing card attributes from different animation frames. */
    public static List<Card> readCards(String source, String resourcePrefix) {
        try {
            NodeList nodes = readDocument(source).getElementsByTagName("*");
            List<Card> cards = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element root = (Element) nodes.item(i);
                if (!(resourcePrefix + "cardRoot").equals(root.getAttribute("resource-id"))
                        || "false".equals(root.getAttribute("displayed"))) continue;
                String title = null;
                boolean mark = false, selected = false, added = false, recommended = false;
                NodeList children = root.getElementsByTagName("*");
                for (int j = 0; j < children.getLength(); j++) {
                    Element child = (Element) children.item(j);
                    if ("false".equals(child.getAttribute("displayed"))) continue;
                    String id = child.getAttribute("resource-id"), text = child.getAttribute("text");
                    if ((resourcePrefix + "tvTitle").equals(id)) title = text;
                    if ((resourcePrefix + "ivSelection").equals(id)) {
                        mark = true; selected = "true".equals(child.getAttribute("selected"));
                    }
                    added |= "Added".equalsIgnoreCase(text); recommended |= "Recommended".equalsIgnoreCase(text);
                }
                if (title != null && !title.trim().isEmpty()) cards.add(new Card(title, mark, selected, added, recommended));
            }
            return cards;
        } catch (Exception invalidSource) {
            throw new IllegalStateException("Cannot parse Android program card snapshot", invalidSource);
        }
    }

    private static org.w3c.dom.Document readDocument(String source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        return builder.parse(new InputSource(new StringReader(source)));
    }

    protected WebElement revealControl(String title, String control) {
        String locator = "xpath:" + cardPath(title) + "//*[@resource-id='" + prefix + control + "']";
        String error = "Program control is absent: " + title + "/" + control;
        // Card roots can be visible while the image checkmark or Learn More is clipped.
        if (isElementVisible(locator)) return waitForElementEnabled(locator, error, 10);
        rewind();
        for (int page = 0; page < 15; page++) {
            if (isElementVisible(locator)) return waitForElementEnabled(locator, error, 10);
            if (!scroll("down")) break;
        }
        return waitForElementEnabled(locator, error, 2);
    }

    private void rewind() { for (int i = 0; i < 15 && scroll("up"); i++) { /* explicit scroll result */ } }
    protected boolean scroll(String direction) {
        WebElement list = waitForElementVisible(id("scrollView"), "Program content has no scroll container", 10);
        Map<String, Object> args = new HashMap<>(); args.put("elementId", ((RemoteWebElement) list).getId());
        args.put("direction", direction); args.put("percent", .5);
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args));
    }

    /** The v3 Explore actions differ from the legacy production Program Page. */
    public List<String> exploreTitles() {
        openExplore();
        for (int n = 0; n < 15 && scrollExplore("left"); n++) { }
        Set<String> titles = new LinkedHashSet<>();
        for (int n = 0; n < 15; n++) {
            collectExploreTitles(titles);
            if (!scrollExplore("right")) { collectExploreTitles(titles); return new ArrayList<>(titles); }
        }
        Assert.fail("Explore program carousel did not reach its end"); return Collections.emptyList();
    }

    private void collectExploreTitles(Set<String> titles) {
        for (WebElement e : driver.findElements(By.xpath("//*[@resource-id='" + prefix
                + "rvActivePrograms']//*[@resource-id='" + prefix + "tvTitle']")))
            if (e.isDisplayed() && !e.getText().isEmpty()) titles.add(e.getText());
        Assert.assertFalse("Explore has no readable program titles", titles.isEmpty());
    }

    private void openExplore() {
        ProgramSwitchFeaturePageObject tutorial = new ProgramSwitchFeaturePageObject(driver);
        tutorial.dismissProgramTooltipsIfPresent();
        waitForElementAndClick(id("nav_graph_explore"), "Explore tab is unavailable", 10);
        createWait(20).withMessage("Explore program catalog is absent").until(ignored -> {
            if (isElementVisible(id("rvActivePrograms"))) return true;
            if (tutorial.isProgramTooltipVisible()) {
                tutorial.dismissProgramTooltipsIfPresent();
                waitForElementAndClick(id("nav_graph_explore"), "Cannot open Explore after the program hint", 10);
            }
            return isElementVisible(id("rvActivePrograms"));
        });
    }

    public void openExploreProgram(String title) {
        openExploreProgramDetails(title);
        waitForElementEnabled(id("btnAddAndStart"), "Explore Program Page action is absent", 15);
    }

    public void openExploreProgramDetails(String title) {
        openExplore();
        for (int n = 0; n < 15 && scrollExplore("left"); n++) { }
        String target = "xpath://*[@resource-id='" + prefix + "rvActivePrograms']//*[@resource-id='"
                + prefix + "tvTitle' and @text=" + Quotes.escape(title) + "]";
        for (int n = 0; n < 15 && !isElementVisible(target); n++) if (!scrollExplore("right")) break;
        waitForElementAndClick(target, "Explore program is absent: " + title, 10);
        waitForElementVisible("xpath://*[@resource-id='" + prefix + "tvProgramTitle' and @text="
                + Quotes.escape(title) + "]", "Explore opened another program", 15);
        waitForElementVisible(id("btnClose"), "Explore Program Page close control is absent", 10);
    }

    public void assertExploreAction(boolean added) {
        Assert.assertEquals("Explore action disagrees with membership", added ? "REMOVE FROM DAILY PLAN" : "ADD TO DAILY PLAN",
                waitForElementEnabled(id("btnAddAndStart"), "Explore action is absent", 10).getText());
    }

    public void addFromExplore(String title) {
        assertExploreAction(false);
        waitForElementAndClick(id("btnAddAndStart"), "Add to Daily Plan is unavailable", 10);
        new ProgramSwitchFeaturePageObject(driver);
        createWait(20).withMessage("Explore addition must open Daily Plan without an add-confirmation dialog").until(ignored -> {
            Assert.assertFalse("Adding a program must not show a confirmation dialog", isElementVisible(id("tvDialogtitle")));
            new ProgramSwitchFeaturePageObject(driver).dismissProgramTooltipsIfPresent();
            return isElementVisible(id("tvDaysNumber"));
        });
        Assert.assertEquals("Explore activated the wrong program", title, new AndroidDailyPlanPageObject(driver).getActiveProgramName());
    }

    public void removeFromExplore(boolean confirm) {
        assertExploreAction(true);
        waitForElementAndClick(id("btnAddAndStart"), "Remove from Daily Plan is unavailable", 10);
        waitForElementVisible(id("tvDialogtitle"), "Removal must require confirmation", 10);
        waitForElementAndClick(id(confirm ? "btnYes" : "btnNo"), "Removal decision is unavailable", 10);
        waitForElementNotVisible(id("tvDialogtitle"), "Removal dialog remained open", 10);
        createWait(15).until(ignored -> (confirm ? "ADD TO DAILY PLAN" : "REMOVE FROM DAILY PLAN")
                .equals(waitForElementVisible(id("btnAddAndStart"), "Explore action is absent", 5).getText()));
    }

    public void closeExploreProgram() { waitForElementAndClick(id("btnClose"), "Cannot close Explore Program Page", 10); }

    @Step("Open Reset Progress from Explore settings for {title}")
    public void openResetProgress(String title) {
        if (isElementVisible(id("bActivate"))) {
            driver.navigate().back();
            Assert.assertEquals("Settings belong to another program", title,
                    waitForElementVisible(id("tvProgramTitle"), "Program page is absent", 10).getText());
        } else {
            openExploreProgramDetails(title);
        }
        waitForElementAndClick(id("btnSettings"), "Program settings gear is absent", 10);
        Assert.assertEquals("Wrong program settings action", "RESET PROGRESS",
                waitForElementEnabled(id("bActivate"), "Reset Progress is absent", 10).getText());
        waitForElementAndClick(id("bActivate"), "Reset Progress is unavailable", 10);
        waitForFirstElementPresent(resetDecisionLocators(true), "Reset confirmation is absent", 10);
        waitForFirstElementPresent(resetDecisionLocators(false), "Reset cancellation is absent", 10);
    }

    public void decideResetProgress(boolean confirm) {
        WebElement decision = waitForFirstElementEnabled(resetDecisionLocators(confirm),
                "Reset decision is unavailable", 10);
        decision.click();
        for (String locator : resetDecisionLocators(true)) {
            waitForElementNotVisible(locator, "Reset dialog remained open", 10);
        }
    }

    private String[] resetDecisionLocators(boolean confirm) {
        return new String[]{id(confirm ? "btnYes" : "btnNo"), "id:android:id/" + (confirm ? "button1" : "button2")};
    }

    private boolean scrollExplore(String direction) {
        WebElement list = waitForElementVisible(id("rvActivePrograms"), "Explore carousel is absent", 10);
        Map<String, Object> args = new HashMap<>(); args.put("elementId", ((RemoteWebElement) list).getId());
        args.put("direction", direction); args.put("percent", .6);
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args));
    }

    public static final class Card {
        public final String title;
        public final boolean selectionControl, selected, added, recommended;
        Card(String title, boolean selectionControl, boolean selected, boolean added, boolean recommended) {
            this.title = title; this.selectionControl = selectionControl; this.selected = selected;
            this.added = added; this.recommended = recommended;
        }
    }
}
