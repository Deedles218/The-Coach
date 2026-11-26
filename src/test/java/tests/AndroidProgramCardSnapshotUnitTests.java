package tests;

import lib.ui.android.AndroidProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;

/** Replay observed card resource IDs; unrelated UI nodes must not change membership or checkmarks. */
public class AndroidProgramCardSnapshotUnitTests {
    private static final String PREFIX = "qa.app:id/";
    private String node(String id, String text, String selected) {
        return "<node resource-id='" + PREFIX + id + "' text='" + text + "' selected='" + selected + "' displayed='true'/>";
    }
    private String card(String title, String body) {
        return "<node resource-id='" + PREFIX + "cardRoot' displayed='true'>" + node("tvTitle", title, "false") + body + "</node>";
    }
    @Test public void globalBadgesAndMarksCannotLeakIntoAnotherCard() {
        String source = "<hierarchy>" + node("ivSelection", "", "true") + node("tvBadge", "Added", "false")
                + card("Unselected", node("ivSelection", "", "false"))
                + card("Selected", node("ivSelection", "", "true") + node("tvRecommended", "RECOMMENDED", "false")) + "</hierarchy>";
        List<AndroidProgramSelectionPageObject.Card> cards = AndroidProgramSelectionPageObject.readCards(source, PREFIX);
        Assert.assertEquals(2, cards.size()); Assert.assertFalse(cards.get(0).selected); Assert.assertFalse(cards.get(0).added);
        Assert.assertTrue(cards.get(1).selected); Assert.assertTrue(cards.get(1).recommended);
    }
    @Test public void addedCardCanBeRecognizedWithoutASelectionControl() {
        List<AndroidProgramSelectionPageObject.Card> cards = AndroidProgramSelectionPageObject.readCards(
                "<hierarchy>" + card("Added program", node("tvBadge", "Added", "false")) + "</hierarchy>", PREFIX);
        Assert.assertTrue(cards.get(0).added); Assert.assertFalse(cards.get(0).selectionControl);
    }
    @Test public void invisibleCardsDoNotBecomeAvailablePrograms() {
        String source = "<hierarchy><node resource-id='" + PREFIX + "cardRoot' displayed='false'>"
                + node("tvTitle", "Hidden", "false") + "</node>" + card("Visible", "") + "</hierarchy>";
        List<AndroidProgramSelectionPageObject.Card> cards = AndroidProgramSelectionPageObject.readCards(source, PREFIX);
        Assert.assertEquals(1, cards.size()); Assert.assertEquals("Visible", cards.get(0).title);
    }
    @Test(expected = IllegalStateException.class) public void invalidSourceCannotLookLikeAnEmptyCatalog() {
        AndroidProgramSelectionPageObject.readCards("not XML", PREFIX);
    }
}
