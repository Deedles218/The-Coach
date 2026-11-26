package lib.ui.android;

import io.qameta.allure.Allure;
import lib.Platform;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Selector resources verified from manProd APK and the live bottom sheet. */
public class AndroidProgramSelectorPageObject extends AndroidDailyPlanPageObject {
    private static final String PREFIX = Platform.getInstance().getAndroidAppPackage() + ":id/";
    protected static final String LIST_QUERY = "//*[@resource-id='"+PREFIX+"rvActivePrograms']"
            + " | //*[@resource-id='"+PREFIX+"design_bottom_sheet']//*[@resource-id='"+PREFIX+"scrollView']";
    protected static final String TITLES_QUERY = "("+LIST_QUERY+")//*[@resource-id='"+PREFIX+"tvTitle']";
    private static final String LIST_LOCATOR = "xpath:"+LIST_QUERY;
    private static final By LIST = By.xpath(LIST_QUERY);
    private static final By TITLES = By.xpath(TITLES_QUERY);
    private static final Pattern DAY_LABEL = Pattern.compile("^(Day|Stage) ([1-9][0-9]*)( of ([1-9][0-9]*))?$");
    public AndroidProgramSelectorPageObject(RemoteWebDriver driver) { super(driver); }
    /** Legacy remains the default for builds without program activation onboarding. */
    public static boolean isActivationFlow() {
        String contract=System.getProperty("coach.selector.contract","legacy");
        Assert.assertTrue("coach.selector.contract must be legacy or activation-v3",
                "legacy".equals(contract)||"activation-v3".equals(contract));
        return "activation-v3".equals(contract);
    }
    @Override public void openProgramSelector() {
        new lib.ui.ProgramSwitchFeaturePageObject(driver).dismissProgramTooltipsIfPresent();
        getActiveProgramName(); // Returns the Today list to its header.
        waitForElementAndClick(TODAY_PROGRAM_HEADING, "Program selector entry is absent", 10);
        waitForElementVisible(LIST_LOCATOR, "Program selector did not open", 10);
        waitForStableViewport();
    }
    @Override public void closeProgramSelector() {
        // Native back dismisses the modal without selecting a program.
        driver.navigate().back();
        waitForElementNotVisible(LIST_LOCATOR, "Selector is still displayed", 10);
        assertDailyPlanDaySwitcherIsDisplayed();
    }
    public void dismissSelectorIfOpen() { if (!driver.findElements(LIST).isEmpty()) closeProgramSelector(); }
    private void waitForStableViewport() {
        final String[] previous={null};
        createWait(10).until(d -> {
            String current=viewport().toString();
            boolean stable=current.equals(previous[0]); previous[0]=current; return stable;
        });
    }
    private Map<String,Rectangle> viewport() {
        Map<String,Rectangle> result = new LinkedHashMap<String,Rectangle>();
        for (WebElement title:driver.findElements(TITLES)) if(title.isDisplayed() && !title.getText().trim().isEmpty()) result.put(title.getText(),title.getRect());
        Assert.assertFalse("Selector has no program titles",result.isEmpty());
        return result;
    }
    private boolean scroll(String direction) {
        Map<String,Object> args=new HashMap<String,Object>();
        args.put("elementId",((RemoteWebElement)driver.findElement(LIST)).getId());
        args.put("direction",direction); args.put("percent",.8);
        boolean more=Boolean.TRUE.equals(((JavascriptExecutor)driver).executeScript("mobile: scrollGesture",args));
        waitForStableViewport();
        return more;
    }
    @Override public List<String> getProgramNamesFromSelector() {
        Set<String> titles=new LinkedHashSet<String>();
        for(int n=0;n<15;n++) {
            titles.addAll(viewport().keySet());
            boolean more=scroll("down");
            titles.addAll(viewport().keySet());
            if(!more) return new ArrayList<String>(titles);
        }
        throw new AssertionError("Program list did not reach its end");
    }
    public void selectProgram(String name) { selectProgram(name, false); }
    public void selectProgramWithLoadingCheck(String name) { selectProgram(name, true); }
    private void selectProgram(String name, boolean observeLoading) {
        Assert.assertTrue("Unreviewed selector destination", Arrays.asList("Last Longer","Keep It Hard","Overall Health","Kegel Challenge","Sex Is a Skill","Solving Couple Fights","A Man's Guide to Sexting").contains(name));
        selectProgram(name, observeLoading, false);
    }
    /** Exact observed destination, including the context-dependent Retain program. */
    public void selectExactProgram(String name) { selectProgram(name, false, true); }
    private void selectProgram(String name, boolean observeLoading, boolean handleConfirmation) {
        boolean reachedEnd = false;
        for(int n=0;n<15;n++) {
            for(WebElement title:driver.findElements(TITLES)) if(title.isDisplayed() && name.equalsIgnoreCase(title.getText())) {
                title.click();
                if(observeLoading) {
                    if(!isActivationFlow())
                        waitForElementVisible("id:"+PREFIX+"pbLoading", "Today loading indicator was not observed after selection", 3);
                    waitForElementNotVisible("id:"+PREFIX+"pbLoading", "Today loading did not finish", 20);
                }
                waitForElementNotVisible(LIST_LOCATOR, "Selector did not close on selection",15);
                if (handleConfirmation) {
                    createWait(20).ignoring(StaleElementReferenceException.class)
                            .withMessage("Today did not show the confirmed program: " + name).until(d -> {
                        if (isElementVisible("xpath://*[@resource-id='"+PREFIX+"tvDialogtitle' and @text='Switch program?']")) {
                            waitForElementAndClick("id:"+PREFIX+"btnYes", "Cannot confirm program switch",10);
                            return false;
                        }
                        if (isElementVisible("id:"+PREFIX+"btnOk")) {
                            waitForElementAndClick("id:"+PREFIX+"btnOk", "Cannot close program hint",10);
                            return false;
                        }
                        return isElementVisible(TODAY_PROGRAM_HEADING)
                                && name.equalsIgnoreCase(getActiveProgramName());
                    });
                    waitForElementVisible(CURRENT_DAY_LABEL, "Today day is absent after confirming program selection", 15);
                } else {
                    waitForSelectedProgram(name);
                }
                return;
            }
            if(reachedEnd) break;
            reachedEnd = !scroll("down");
        }
        Assert.fail("Program is absent from selector: "+name);
    }
    public void waitForSelectedProgram(String title) {
        createWait(20).ignoring(StaleElementReferenceException.class).until(d -> title.equalsIgnoreCase(getActiveProgramName()));
        assertDailyPlanDaySwitcherIsDisplayed();
    }

    public void selectObservedDay(int targetDay) {
        String label = getCurrentDayLabel().trim();
        Matcher observed = DAY_LABEL.matcher(label);
        Assert.assertTrue("Unreadable Android Daily Plan day: " + label, observed.matches());
        Assert.assertTrue("Target day is outside the displayed Daily Plan range", targetDay > 0
                && (observed.group(4) == null || targetDay <= Integer.parseInt(observed.group(4))));
        int currentDay = Integer.parseInt(observed.group(2));
        if (currentDay == targetDay) return;
        int step = targetDay < currentDay ? -1 : 1;
        String control = step < 0 ? LEFT_SWITCHER_ARROW : RIGHT_SWITCHER_ARROW;
        for (int day = currentDay + step; day != targetDay + step; day += step) {
            String expected = observed.group(1) + " " + day + (observed.group(3) == null ? "" : observed.group(3));
            waitForElementAndClick(control, "Cannot navigate to " + expected, 10);
            createWait(15).ignoring(StaleElementReferenceException.class).withMessage(
                    "Android Daily Plan did not open " + expected).until(d -> expected.equals(getCurrentDayLabel().trim()));
        }
    }
    public Rectangle firstCardBounds() {
        WebElement title=driver.findElements(TITLES).stream().filter(WebElement::isDisplayed).findFirst().orElseThrow(() -> new AssertionError("No selector card"));
        return driver.findElement(By.xpath("("+TITLES_QUERY+")[1]/ancestor::*[@clickable='true'][1]")).getRect();
    }
    public Rectangle selectorMotionBounds() {
        return isActivationFlow()?waitForElementVisible("id:"+PREFIX+"design_bottom_sheet","Selector surface is absent",10).getRect():firstCardBounds();
    }
    public void assertActiveProgramIsFirst(String expected) {
        Assert.assertEquals("Active program is not first",expected.toLowerCase(Locale.ROOT),viewport().keySet().iterator().next().toLowerCase(Locale.ROOT));
        Assert.assertTrue("Selector is not a bottom sheet",firstCardBounds().y>0);
    }
    public void assertActivationSelection(String main, Collection<String> programs, String active) {
        Assert.assertTrue("The activation selector contract must be explicit",isActivationFlow());
        Assert.assertTrue("The expected Main Journey is absent from the fixture",programs.contains(main));
        List<String> secondary=new ArrayList<String>(programs);secondary.remove(main);
        new AndroidMyProgramsPageObject(driver).assertPrograms(main,secondary,active);
        Assert.assertEquals("Switching Active Program moved Main Journey",main,viewport().keySet().iterator().next());
    }
    public void assertProgramsScrollVertically() {
        assertProgramsScrollVertically(Collections.emptySet());
    }
    public void assertProgramsScrollVertically(Set<String> expectedPrograms) {
        Map<String,Rectangle> before=viewport();
        boolean scrollable=Boolean.parseBoolean(driver.findElement(LIST).getAttribute("scrollable"));
        scroll("down");
        Map<String,Rectangle> after=viewport();
        assertProgramListMotion(before,after,scrollable,expectedPrograms);
        if(isActivationFlow()&&scrollable) {
            Set<String> observed=new LinkedHashSet<String>(before.keySet());
            observed.addAll(after.keySet());observed.addAll(getProgramNamesFromSelector());
            Assert.assertEquals("Scrolling lost saved programs",expectedPrograms,observed);
        }
    }
    protected static void assertProgramListMotion(Map<String,Rectangle> before, Map<String,Rectangle> after,
                                                  boolean scrollable, Set<String> expectedPrograms) {
        if(isActivationFlow()&&!scrollable) {
            Assert.assertFalse("Non-scrolling v3 selector needs the independently verified saved program set",expectedPrograms.isEmpty());
            Assert.assertEquals("A non-scrolling selector hides saved programs",expectedPrograms,before.keySet());
            Assert.assertEquals("A non-scrolling selector changed its visible programs",expectedPrograms,after.keySet());
        }
        boolean moved=false;
        for(String title:before.keySet()) if(after.containsKey(title)) {
            Rectangle a=before.get(title), b=after.get(title);
            Assert.assertTrue("Program moved horizontally",Math.abs(a.x-b.x)<=3);
            moved |= b.y<a.y;
        }
        if(!isActivationFlow()||scrollable)
            Assert.assertTrue("Program list did not scroll vertically",moved || !before.keySet().equals(after.keySet()));
    }
    public void swipeSelectorDown() {
        int x,y;
        if(isElementVisible("id:"+PREFIX+"vSwap")) {
            Rectangle r=waitForElementVisible("id:"+PREFIX+"vSwap", "Selector drag handle absent",10).getRect();
            x=r.x+r.width/2;y=r.y+r.height/2;
        } else {
            Rectangle sheet=waitForElementVisible("id:"+PREFIX+"design_bottom_sheet", "Selector sheet absent",10).getRect();
            Rectangle heading=waitForElementVisible("xpath://*[@resource-id='"+PREFIX+"design_bottom_sheet']//*[@text='My programs']", "Selector heading absent",10).getRect();
            Assert.assertTrue("Selector has no handle region above its heading",heading.y>sheet.y);
            x=sheet.x+sheet.width/2;y=(sheet.y+heading.y)/2;
        }
        Map<String,Object> args=new HashMap<String,Object>();
        args.put("startX",x); args.put("endX",x);
        args.put("startY",y); args.put("endY",driver.manage().window().getSize().height-30);
        args.put("speed",1000);
        ((JavascriptExecutor)driver).executeScript("mobile: dragGesture",args);
        waitForElementNotVisible(LIST_LOCATOR, "Swipe did not close selector",10);
        assertDailyPlanDaySwitcherIsDisplayed();
    }

    @SuppressWarnings("unchecked")
    public void assertSelectedProgramContent(Map<String,Object> content) {
        Assert.assertEquals("Wrong destination catalog","keep_it_hard",content.get("programId"));
        assertActiveProgramMatches((String)content.get("headline"));
        Map<String,Object> metadata=(Map<String,Object>)content.get("sectionMetadata");
        Assert.assertEquals("Wrong module title",metadata.get("module_name"),
                waitForElementVisible("id:"+PREFIX+"tvModuleName","Module title absent",10).getText());
        String stage="Stage "+((Number)metadata.get("module_current_day")).intValue()+" of "+((Number)metadata.get("module_total_days")).intValue();
        Assert.assertEquals("Wrong selected stage",stage,getCurrentDayLabel());
        Set<String> expected=new LinkedHashSet<String>();
        for(Map<String,Object> question:(List<Map<String,Object>>)content.get("questions")) {
            if(((String)question.get("id")).startsWith("lesson_")) expected.add(((String)question.get("headline")).trim());
            if(expected.size()==3)break;
        }
        Assert.assertEquals("Destination needs three distinguishable lesson cards",3,expected.size());
        Set<String> observed=new HashSet<String>();
        try {
            for(int n=0;n<6;n++) {
                for(WebElement title:driver.findElements(By.id(PREFIX+"tvLessonName"))) if(title.isDisplayed())observed.add(title.getText().trim());
                if(observed.containsAll(expected))return;
                mobileSwipeUp();
            }
            Assert.fail("Selected program cards differ from catalog; expected "+expected+"; observed "+observed);
        } finally { openTodayTab(); }
    }
}
