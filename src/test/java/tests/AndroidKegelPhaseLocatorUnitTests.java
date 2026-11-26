package tests;

import lib.Platform;
import lib.ui.android.AndroidKegelPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Passive footer labels and the changing instruction cannot prove phase selection. */
public class AndroidKegelPhaseLocatorUnitTests {
    private static class Screen extends AndroidKegelPageObject {
        Screen(){super(null);}
        String active(){return activeKegelPhase("CLASP").substring(6);}
    }
    private String pkg(){return Platform.getInstance().getAndroidAppPackage();}
    private int matches(String pkg,String container,String id,String label) throws Exception {
        String xml="<hierarchy><androidx.recyclerview.widget.RecyclerView resource-id='"+pkg+":id/"+container+"'>"
                +"<android.widget.TextView resource-id='"+pkg+":id/"+id+"' text='"+label+"'/>"
                +"</androidx.recyclerview.widget.RecyclerView></hierarchy>";
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().active(),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void theRequestedActivePhaseIsRecognized() throws Exception {
        Assert.assertEquals(1,matches(pkg(),"rvExercise","tvExerciseName","CLASP"));
    }
    @Test public void legacyTitleCasePhaseRemainsRecognized() throws Exception {
        Assert.assertEquals(1,matches(pkg(),"rvExercise","tvExerciseName","Clasp"));
    }
    @Test public void passiveLabelCannotProveSelection() throws Exception {
        Assert.assertEquals(0,matches(pkg(),"rvExercise","tvExerciseNamePassive","CLASP"));
    }
    @Test public void anotherActivePhaseCannotSatisfyTheCheck() throws Exception {
        Assert.assertEquals(0,matches(pkg(),"rvExercise","tvExerciseName","REST"));
    }
    @Test public void unrelatedTextAndOtherAppsAreRejected() throws Exception {
        Assert.assertEquals(0,matches(pkg(),"rvExercise","tvActionName","CLASP"));
        Assert.assertEquals(0,matches(pkg(),"rvContent","tvExerciseName","CLASP"));
        Assert.assertEquals(0,matches("another.app","rvExercise","tvExerciseName","CLASP"));
    }
}
