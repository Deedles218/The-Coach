package lib.ui.android;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.*;
import java.util.*;

/** Selector resources verified from manProd APK and the live bottom sheet. */
public class AndroidProgramSelectorPageObject extends AndroidDailyPlanPageObject {
    private static final String PREFIX = "com.vamapps.thecoach:id/";
    private static final By LIST = By.id(PREFIX + "rvActivePrograms");
    private static final By TITLES = By.xpath("//*[@resource-id='"+PREFIX+"rvActivePrograms']//*[@resource-id='"+PREFIX+"tvTitle']");
    public AndroidProgramSelectorPageObject(RemoteWebDriver driver) { super(driver); }
    @Override public void openProgramSelector() {
        getActiveProgramName(); // Returns the Today list to its header.
        waitForElementAndClick("xpath://*[@resource-id='"+PREFIX+"headerContainer']//*[@resource-id='"+PREFIX+"tvGroupName']", "Program selector entry is absent", 10);
        waitForElementVisible("id:"+PREFIX+"rvActivePrograms", "Program selector did not open", 10);
        waitForStableViewport();
    }
    @Override public void closeProgramSelector() {
        // Native back dismisses the modal without selecting a program.
        driver.navigate().back();
        waitForElementNotVisible("id:"+PREFIX+"rvActivePrograms", "Selector is still displayed", 10);
        assertDailyPlanDaySwitcherIsDisplayed();
    }
    public void dismissSelectorIfOpen() { if (!driver.findElements(By.id(PREFIX+"vSwap")).isEmpty()) closeProgramSelector(); }
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
        for(int n=0;n<15;n++) {
            for(WebElement title:driver.findElements(TITLES)) if(title.isDisplayed() && name.equalsIgnoreCase(title.getText())) {
                title.click();
                if(observeLoading) {
                    waitForElementVisible("id:"+PREFIX+"pbLoading", "Today loading indicator was not observed after selection", 3);
                    waitForElementNotVisible("id:"+PREFIX+"pbLoading", "Today loading did not finish", 20);
                }
                waitForElementNotVisible("id:"+PREFIX+"rvActivePrograms", "Selector did not close on selection",15);
                waitForSelectedProgram(name); return;
            }
            if(!scroll("down")) break;
        }
        Assert.fail("Program is absent from selector: "+name);
    }
    public void waitForSelectedProgram(String title) {
        createWait(20).ignoring(StaleElementReferenceException.class).until(d -> title.equalsIgnoreCase(getActiveProgramName()));
        assertDailyPlanDaySwitcherIsDisplayed();
    }
    public Rectangle firstCardBounds() {
        WebElement title=driver.findElements(TITLES).stream().filter(WebElement::isDisplayed).findFirst().orElseThrow(() -> new AssertionError("No selector card"));
        return driver.findElement(By.xpath("(//*[@resource-id='"+PREFIX+"rvActivePrograms']//*[@resource-id='"+PREFIX+"tvTitle'])[1]/ancestor::*[@clickable='true'][1]")).getRect();
    }
    public void assertActiveProgramIsFirst(String expected) {
        Assert.assertEquals("Active program is not first",expected.toLowerCase(Locale.ROOT),viewport().keySet().iterator().next().toLowerCase(Locale.ROOT));
        Assert.assertTrue("Selector is not a bottom sheet",firstCardBounds().y>0);
    }
    public void assertProgramsScrollVertically() {
        Map<String,Rectangle> before=viewport();
        scroll("down");
        Map<String,Rectangle> after=viewport();
        boolean moved=false;
        for(String title:before.keySet()) if(after.containsKey(title)) {
            Rectangle a=before.get(title), b=after.get(title);
            Assert.assertTrue("Program moved horizontally",Math.abs(a.x-b.x)<=3);
            moved |= b.y<a.y;
        }
        Assert.assertTrue("Program list did not scroll vertically",moved || !before.keySet().equals(after.keySet()));
    }
    public void swipeSelectorDown() {
        WebElement handle=waitForElementVisible("id:"+PREFIX+"vSwap", "Selector drag handle absent",10);
        Rectangle r=handle.getRect();
        Map<String,Object> args=new HashMap<String,Object>();
        args.put("startX",r.x+r.width/2); args.put("endX",r.x+r.width/2);
        args.put("startY",r.y+r.height/2); args.put("endY",driver.manage().window().getSize().height-30);
        args.put("speed",1000);
        ((JavascriptExecutor)driver).executeScript("mobile: dragGesture",args);
        waitForElementNotVisible("id:"+PREFIX+"rvActivePrograms", "Swipe did not close selector",10);
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
