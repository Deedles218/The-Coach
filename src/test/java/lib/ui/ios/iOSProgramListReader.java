package lib.ui.ios;

import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads recycled cells while making bounded, overlapping scrolls. */
final class iOSProgramListReader extends MainPageObject {
    iOSProgramListReader(RemoteWebDriver driver) { super(driver); }

    List<String> collect(String titles, String container, boolean horizontal) {
        Set<String> names = new LinkedHashSet<String>();
        String previous = null;
        for (int scroll = 0; scroll < 25; scroll++) {
            List<WebElement> elements = driver.findElements(getLocatorByString(titles));
            List<String> viewport = new ArrayList<String>();
            for (WebElement element : elements) {
                if (!element.isDisplayed()) continue;
                String title = element.getAttribute("label");
                if (title == null || title.trim().isEmpty()) title = element.getText();
                Assert.assertTrue("Program card has no title", title != null && !title.trim().isEmpty());
                names.add(title.trim());
                Rectangle position = element.getRect();
                viewport.add(title + ":" + position.x + ":" + position.y);
            }
            String fingerprint = viewport.toString();
            if (fingerprint.equals(previous)) {
                Assert.assertFalse("Program list is empty", names.isEmpty());
                return new ArrayList<String>(names);
            }
            previous = fingerprint;
            drag(waitForElementPresent(container, "Program collection is not visible", 5), horizontal, false);
        }
        throw new AssertionError("Program list did not reach its end after 25 overlapping scrolls");
    }

    void rewind(String titles, String container) {
        String previous = null;
        for (int i = 0; i < 25; i++) {
            StringBuilder state = new StringBuilder();
            for (WebElement e : driver.findElements(getLocatorByString(titles))) {
                if (e.isDisplayed()) state.append(e.getAttribute("label"))
                        .append(e.getRect().x).append(':').append(e.getRect().y);
            }
            if (state.toString().equals(previous)) return;
            previous = state.toString();
            drag(waitForElementPresent(container, "Program collection is not visible", 5), true, true);
        }
        throw new AssertionError("Could not rewind the program carousel");
    }

    private void drag(WebElement collection, boolean horizontal, boolean reverse) {
        Rectangle r = collection.getRect();
        int screenHeight = driver.manage().window().getSize().getHeight();
        int top = Math.max(100, r.y), bottom = Math.min(screenHeight - 90, r.y + r.height);
        Map<String,Object> args = new HashMap<String,Object>();
        args.put("duration", 0.65);
        if (horizontal) {
            args.put("fromX", r.x + (int)(r.width * (reverse ? .2 : .8)));
            args.put("toX", r.x + (int)(r.width * (reverse ? .8 : .2)));
            args.put("fromY", (top + bottom) / 2);
            args.put("toY", (top + bottom) / 2);
        } else {
            args.put("fromX", r.x + r.width / 2);
            args.put("toX", r.x + r.width / 2);
            args.put("fromY", bottom - 25);
            args.put("toY", top + (bottom - top) / 3);
        }
        ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", args);
    }
}
