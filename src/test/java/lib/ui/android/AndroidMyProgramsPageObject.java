package lib.ui.android;

import lib.MobileCaseFixture;
import lib.Platform;
import lib.ProgramSelectionState;
import lib.ui.ProgramSwitchFeaturePageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** ON selector resources observed in the preprod Android activation build. */
public final class AndroidMyProgramsPageObject extends AndroidDailyPlanPageObject {
    private final String prefix = Platform.getInstance().getAndroidAppPackage() + ":id/";
    private final MobileCaseFixture fixture;

    public AndroidMyProgramsPageObject(RemoteWebDriver driver) { this(driver, null); }

    public AndroidMyProgramsPageObject(RemoteWebDriver driver, MobileCaseFixture fixture) {
        super(driver);
        this.fixture = fixture;
    }

    @Override public void openProgramSelector() {
        getActiveProgramName();
        ProgramSwitchFeaturePageObject tutorial = new ProgramSwitchFeaturePageObject(driver);
        String sheet = "id:" + prefix + "containerMain";
        tutorial.dismissProgramTooltipsIfPresent();
        if (!isElementVisible(sheet)) waitForElementAndClick(TODAY_PROGRAM_HEADING, "Cannot open Android My Programs", 10);
        createWait(15).withMessage("ON Main Journey container is absent").until(ignored -> {
            if (isElementVisible(sheet)) return true;
            // The first-switch hint can arrive after the header has become tappable.
            if (tutorial.isProgramTooltipVisible()) {
                tutorial.dismissProgramTooltipsIfPresent();
                if (!isElementVisible(sheet)) waitForElementAndClick(TODAY_PROGRAM_HEADING, "Cannot open Android My Programs after the hint", 10);
            }
            return isElementVisible(sheet);
        });
    }

    @Override public void closeProgramSelector() {
        driver.navigate().back();
        waitForElementNotVisible("id:" + prefix + "containerMain", "Android My Programs remained open", 10);
    }

    public void assertMatchesState(ProgramSelectionState state) {
        String titles = "xpath://*[@resource-id='" + prefix + "scrollView']//*[@resource-id='" + prefix + "tvTitle']";
        List<String> actual = driver.findElements(getLocatorByString(titles)).stream()
                .map(WebElement::getText).collect(Collectors.toList());
        Set<String> expected = state.programs().stream().map(p -> (String) p.get("title"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Assert.assertEquals("Android selector contains duplicate rows", actual.size(), new LinkedHashSet<>(actual).size());
        Assert.assertEquals("Android selector has missing or unadded programs", expected, new LinkedHashSet<>(actual));
        Assert.assertEquals("Android Main Journey differs", state.main().get("title"),
                waitForElementVisible("xpath://*[@resource-id='" + prefix + "containerMain']//*[@resource-id='"
                        + prefix + "tvTitle']", "Android Main title is absent", 10).getText());
        if (!state.secondary().isEmpty() && fixture != null) {
            List<String> secondaries = driver.findElements(getLocatorByString(fixture.selector("selector.secondary.titles")))
                    .stream().map(WebElement::getText).collect(Collectors.toList());
            Assert.assertEquals("Android Secondary roles differ", state.secondary().stream().map(p -> p.get("title"))
                    .collect(Collectors.toSet()), new LinkedHashSet<>(secondaries));
        }
        Assert.assertEquals("Android selector must have one active mark", 1, driver.findElements(By.id(prefix + "ivSelected"))
                .stream().filter(WebElement::isDisplayed).count());
        for (Map<String, Object> program : state.programs()) {
            String id = (String) program.get("program_id"), row = row((String) program.get("title"));
            Assert.assertEquals("Android progress label differs from the saved program's unit and position",
                    state.progressLabel(id).toUpperCase(java.util.Locale.ROOT),
                    waitForElementVisible("xpath:" + row + "//*[@resource-id='" + prefix + "tvSubtitle']",
                            "Android program subtitle is absent", 10).getText().toUpperCase(java.util.Locale.ROOT));
            String ring = waitForElementVisible("xpath:" + row + "//*[@resource-id='" + prefix + "progressRing']",
                    "Android program progress is absent", 10).getText();
            state.assertDisplayedPercent(id, ring);
            boolean marked = driver.findElements(By.xpath(row + "//*[@resource-id='" + prefix + "ivSelected']"))
                    .stream().anyMatch(WebElement::isDisplayed);
            Assert.assertEquals("Android active mark is on the wrong row", id.equals(state.active()), marked);
        }
    }

    public void assertPrograms(String main, List<String> secondary, String active) {
        String mainTitle = "xpath://*[@resource-id='" + prefix + "containerMain']//*[@resource-id='" + prefix + "tvTitle']";
        Assert.assertEquals("Wrong Main Journey", main, waitForElementVisible(mainTitle, "Main Journey is absent", 10).getText());
        List<String> names = driver.findElements(By.xpath("//*[@resource-id='" + prefix
                + "scrollView']//*[@resource-id='" + prefix + "tvTitle']"))
                .stream().map(WebElement::getText).collect(Collectors.toList());
        Set<String> expected = new LinkedHashSet<>(); expected.add(main); expected.addAll(secondary);
        Assert.assertEquals("My programs contains duplicates", names.size(), new LinkedHashSet<>(names).size());
        Assert.assertEquals("My programs has missing or unadded programs", expected, new LinkedHashSet<>(names));
        Assert.assertEquals("My programs must have one active mark", 1L,
                driver.findElements(By.id(prefix + "ivSelected")).stream().filter(WebElement::isDisplayed).count());
        Assert.assertTrue("Active mark is on the wrong program", isElementVisible(
                "xpath:" + row(active) + "//*[@resource-id='" + prefix + "ivSelected']"));
    }

    public void assertProgressLabels(Map<String, String> expected) {
        Assert.assertFalse("Prepare independent Daily Plan labels", expected.isEmpty());
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Assert.assertTrue("Daily Plan must expose Day or Stage: " + entry.getValue(),
                    entry.getValue().matches("(?i)(DAY|STAGE) \\d+ OF \\d+"));
            String actual = waitForElementVisible("xpath:" + row(entry.getKey())
                            + "//*[@resource-id='" + prefix + "tvSubtitle']",
                    "My programs subtitle is absent: " + entry.getKey(), 10).getText();
            Assert.assertEquals("My programs unit or counter differs from this program's Daily Plan: " + entry.getKey(),
                    entry.getValue().toUpperCase(java.util.Locale.ROOT), actual.toUpperCase(java.util.Locale.ROOT));
        }
    }

    public void openAllPrograms() {
        waitForElementAndClick("id:" + prefix + "tvAll", "ALL is absent in My programs", 10);
        waitForElementVisible("xpath://*[@resource-id='" + prefix + "tvTitle' and @text='All Programs']",
                "ALL did not open All Programs", 15);
    }

    public void selectSavedProgram(String title, boolean confirmation) {
        waitForElementAndClick("xpath:" + row(title), "Android saved program is absent", 10);
        if (confirmation) {
            ProgramSwitchFeaturePageObject switcher = new ProgramSwitchFeaturePageObject(driver);
            switcher.waitForConfirmation();
            switcher.confirmSwitch();
        }
        waitForElementNotVisible("id:" + prefix + "containerMain", "Android selector did not close", 15);
        ProgramSwitchFeaturePageObject tutorial = new ProgramSwitchFeaturePageObject(driver);
        createWait(20).withMessage("Android opened the wrong Daily Plan").until(ignored -> {
            tutorial.dismissProgramTooltipsIfPresent();
            return isElementVisible(TODAY_PROGRAM_HEADING) && title.equals(getActiveProgramName());
        });
    }

    private String row(String title) {
        return "//*[@resource-id='" + prefix + "scrollView']//*[@resource-id='" + prefix
                + "tvTitle' and @text=" + Quotes.escape(title) + "]/ancestor::*[@clickable='true'][1]";
    }
}
