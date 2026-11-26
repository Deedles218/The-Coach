package tests;

import lib.Platform;
import lib.ui.android.AndroidProgramSelectorPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Both selector generations must exclude titles in other screens and packages. */
public class AndroidSelectorLayoutLocatorUnitTests {
    private static class Screen extends AndroidProgramSelectorPageObject {
        Screen() { super(null); }
        String list() { return LIST_QUERY; }
        String titles() { return TITLES_QUERY; }
    }
    private List<String> texts(String xml,String query) throws Exception {
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        org.w3c.dom.NodeList nodes=(org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(query,doc,XPathConstants.NODESET);
        List<String> result=new ArrayList<>();
        for(int n=0;n<nodes.getLength();n++)result.add(nodes.item(n).getAttributes().getNamedItem("text").getNodeValue());
        return result;
    }
    private String prefix() { return Platform.getInstance().getAndroidAppPackage()+":id/"; }
    private String title(String name) { return "<android.widget.TextView resource-id='"+prefix()+"tvTitle' text='"+name+"'/>"; }
    private String sheet(String children) {
        return "<hierarchy><android.widget.FrameLayout resource-id='"+prefix()+"design_bottom_sheet'><android.widget.ScrollView resource-id='"+prefix()+"scrollView' text='list'>"+children+"</android.widget.ScrollView></android.widget.FrameLayout></hierarchy>";
    }
    @Test public void legacyRecyclerTitlesRemainRecognized() throws Exception {
        String xml="<hierarchy><androidx.recyclerview.widget.RecyclerView resource-id='"+prefix()+"rvActivePrograms' text='list'>"+title("Main")+title("Secondary")+"</androidx.recyclerview.widget.RecyclerView></hierarchy>";
        Assert.assertEquals(Arrays.asList("Main","Secondary"),texts(xml,new Screen().titles()));
        Assert.assertEquals(Arrays.asList("list"),texts(xml,new Screen().list()));
    }
    @Test public void mainAndSecondaryRowsShareTheNewScrollContainer() throws Exception {
        String xml=sheet("<android.widget.LinearLayout resource-id='"+prefix()+"containerMain'>"+title("Main")+"</android.widget.LinearLayout><android.widget.LinearLayout resource-id='"+prefix()+"containerSecondary'>"+title("Secondary")+"</android.widget.LinearLayout>");
        Assert.assertEquals(Arrays.asList("Main","Secondary"),texts(xml,new Screen().titles()));
        Assert.assertEquals(Arrays.asList("list"),texts(xml,new Screen().list()));
    }
    @Test public void aNonModalScrollViewCannotBecomeASelector() throws Exception {
        String xml="<hierarchy><android.widget.ScrollView resource-id='"+prefix()+"scrollView' text='other'>"+title("Other screen")+"</android.widget.ScrollView></hierarchy>";
        Assert.assertTrue(texts(xml,new Screen().titles()).isEmpty());
        Assert.assertTrue(texts(xml,new Screen().list()).isEmpty());
    }
    @Test public void anotherPackageCannotSatisfySelectorReadiness() throws Exception {
        String xml=sheet(title("Other app")).replace(prefix(),"another.app:id/");
        Assert.assertTrue(texts(xml,new Screen().titles()).isEmpty());
        Assert.assertTrue(texts(xml,new Screen().list()).isEmpty());
    }
}
