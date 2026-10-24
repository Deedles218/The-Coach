package tests;

import lib.Platform;
import lib.ui.BookmarksPageObject;
import lib.ui.android.AndroidBookmarksPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import org.junit.*;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AndroidBookmarksLocatorUnitTests {
    private static final String P = Platform.getInstance().getAndroidAppPackage() + ":id/";
    private static final Map<Field, Object> ORIGINAL = new HashMap<>();

    // Existing Page Objects share static selectors. Do not leak this Android-only fixture into iOS unit tests.
    @BeforeClass public static void captureSelectors() throws Exception {
        for (Field f : BookmarksPageObject.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                f.setAccessible(true); ORIGINAL.put(f, f.get(null));
            }
        }
        new Screen();
    }
    @AfterClass public static void restoreSelectors() throws Exception {
        for (Map.Entry<Field, Object> e : ORIGINAL.entrySet()) e.getKey().set(null, e.getValue());
    }

    private static class Screen extends AndroidBookmarksPageObject {
        Screen() { super(null); }
        String card(String text) { return cardFor(text); }
        String icon(String text) { return cardBookmarkFor(text); }
        String image(String text) { return cardImageFor(text); }
        String category(String text, String category) { return cardCategoryFor(text, category); }
        String group(String text, String group) { return cardGroupFor(text, group); }
        String slide(String text) { return slideText(text); }
        String completed(String text) { return completedMarkerFor(text); }
        String exploreEntry() { return exploreBookmarksEntry(); }
        String previous() { return previousDayControl(); }
    }

    private static String label(String id, String text) {
        return "<android.widget.TextView resource-id='" + P + id + "' text='" + text + "'/>";
    }
    private static String card(String title) {
        return "<androidx.cardview.widget.CardView><android.view.ViewGroup>" + label("tvTitle", title)
                + "<android.widget.ImageView resource-id='" + P + "ivCover'/>"
                + "<android.widget.ImageView resource-id='" + P + "ivBookmark' content-desc='Remove from bookmarks'/>"
                + "</android.view.ViewGroup></androidx.cardview.widget.CardView>";
    }
    private static int count(String xml, String locator) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(locator.substring(6), document,
                XPathConstants.NODESET)).getLength();
    }
    private static String list(String rows) {
        return "<root><androidx.recyclerview.widget.RecyclerView resource-id='" + P + "rvBookmarks'>"
                + rows + "</androidx.recyclerview.widget.RecyclerView></root>";
    }

    @Test public void cardControlsAreScopedToTheRequestedRecyclerRow() throws Exception {
        String xml = list(card("First") + card("Target")); Screen screen = new Screen();
        Assert.assertEquals(1, count(xml, screen.card("Target")));
        Assert.assertEquals(1, count(xml, screen.icon("Target")));
        Assert.assertEquals(1, count(xml, screen.image("Target")));
        Assert.assertEquals(0, count(xml, screen.card("Absent")));
    }

    @Test public void neighboringHeadingsCannotSatisfyCardGrouping() throws Exception {
        String xml = list(label("tvProgramName", "First Program") + label("tvModuleName", "Module 1")
                + label("tvSectionName", "Lessons") + card("First")
                + label("tvProgramName", "Second Program") + label("tvModuleName", "Module 2")
                + label("tvSectionName", "Practices") + card("Target"));
        Screen screen = new Screen();
        for (String locator : Arrays.asList(screen.group("Target", "Second Program"),
                screen.group("Target", "Module 2"), screen.category("Target", "Practices"))) {
            Assert.assertEquals(1, count(xml, locator));
        }
        for (String locator : Arrays.asList(screen.group("Target", "First Program"),
                screen.group("Target", "Module 1"), screen.category("Target", "Lessons"))) {
            Assert.assertEquals(0, count(xml, locator));
        }
    }

    @Test public void aModuleFromThePreviousProgramCannotOwnANewProgramCard() throws Exception {
        String xml = list(label("tvProgramName", "First Program") + label("tvModuleName", "Module 1")
                + card("First") + label("tvProgramName", "Second Program") + card("Target"));
        Assert.assertEquals(0, count(xml, new Screen().group("Target", "Module 1")));
    }

    @Test public void savedSlideMustBeInsideTheLessonBodyNotThePersistentHeader() throws Exception {
        String xml = "<root>" + label("tvControlHeader", "Persistent title")
                + "<androidx.recyclerview.widget.RecyclerView resource-id='" + P + "rvLessonDetail'>"
                + label("tvTextContent", "Saved slide") + "</androidx.recyclerview.widget.RecyclerView></root>";
        Assert.assertEquals(0, count(xml, new Screen().slide("Persistent title")));
        Assert.assertEquals(1, count(xml, new Screen().slide("Saved slide")));
    }

    @Test public void hiddenSlidesAndBothQuoteTypesAreHandled() throws Exception {
        String xml = "<root><androidx.recyclerview.widget.RecyclerView resource-id='" + P + "rvLessonDetail'>"
                + "<android.widget.TextView text='Your &quot;partner&quot;&apos;s desire'/>"
                + "<android.widget.TextView text='Hidden' displayed='false'/>"
                + "</androidx.recyclerview.widget.RecyclerView></root>";
        Assert.assertEquals(1, count(xml, new Screen().slide("Your \"partner\"'s desire")));
        Assert.assertEquals(0, count(xml, new Screen().slide("Hidden")));
    }

    @Test public void completionCannotComeFromANeighborOrUnfinishedText() throws Exception {
        String xml = "<root><androidx.cardview.widget.CardView resource-id='" + P + "cvLessonContainer'>"
                + label("tvLessonName", "Neighbor") + label("status", "Completed")
                + "</androidx.cardview.widget.CardView><androidx.cardview.widget.CardView resource-id='" + P + "cvLessonContainer'>"
                + label("tvLessonName", "Target") + label("status", "Not completed")
                + "<android.view.View resource-id='" + P + "lpProgressContainer' content-desc='0%' selected='false'/>"
                + "</androidx.cardview.widget.CardView></root>";
        Assert.assertEquals(0, count(xml, new Screen().completed("Target")));
        Assert.assertEquals(1, count(xml, new Screen().completed("Neighbor")));
    }

    @Test public void androidControlsUseTheConfiguredPackage() {
        Assert.assertEquals("id:" + P + "btnSecondaryAction", new Screen().exploreEntry());
        Assert.assertEquals("id:" + P + "ivBack", new Screen().previous());
    }

    @Test public void emptyStateDoesNotWaitForAnIOSIllustration() {
        Screen screen = new Screen() {
            @Override public org.openqa.selenium.WebElement waitForElementPresent(String locator, String message, long timeout) {
                Assert.assertEquals("id:" + P + "tvEmpty", locator); return null;
            }
            @Override public boolean hasAnyBookmarkCard() { return false; }
        };
        screen.assertEmptyState();
    }

    @Test public void factorySelectsAndroidWithoutCreatingAnAppiumSession() {
        String before = System.getProperty("platform");
        try {
            System.setProperty("platform", "android");
            Assert.assertTrue(BookmarksPageObjectFactory.get(null) instanceof AndroidBookmarksPageObject);
        } finally {
            if (before == null) System.clearProperty("platform"); else System.setProperty("platform", before);
        }
    }

    @Test public void androidInheritsAll21CasesAndOnlyOverridesItsTwoDifferentContracts() throws Exception {
        Assert.assertEquals(21, new BlockJUnit4ClassRunner(BookmarksTests.class).getDescription().getChildren().size());
        Assert.assertEquals(21, new BlockJUnit4ClassRunner(AndroidBookmarksTests.class).getDescription().getChildren().size());
        Assert.assertEquals(AndroidBookmarksTests.class,
                AndroidBookmarksTests.class.getMethod("availableLessonCanBeBookmarkedWithoutSubscription").getDeclaringClass());
        Assert.assertEquals(BookmarksTests.class,
                AndroidBookmarksTests.class.getMethod("removeDeletesOnlySelectedBookmarkAndSyncsContent").getDeclaringClass());
    }

    @Test public void wrongPlatformIsRejectedBeforeDriverCreation() throws Exception {
        String before = System.getProperty("platform");
        try {
            System.setProperty("platform", "ios");
            AndroidBookmarksTests test = new AndroidBookmarksTests() {
                @Override protected RemoteWebDriver createDriver() {
                    throw new AssertionError("Driver creation must not be reached");
                }
            };
            try { test.setUp(); Assert.fail("Wrong platform accepted"); }
            catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("platform=android")); }
        } finally {
            if (before == null) System.clearProperty("platform"); else System.setProperty("platform", before);
        }
    }
}
