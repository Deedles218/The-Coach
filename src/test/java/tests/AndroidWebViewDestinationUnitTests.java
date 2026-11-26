package tests;

import lib.ui.android.AndroidWebViewPageObject;
import org.junit.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;

public class AndroidWebViewDestinationUnitTests {
    private Map<String,Object> page(String description) {
        Map<String,Object> page=new HashMap<>();
        page.put("type","page");page.put("url","https://vamapps.com/faq_forher");page.put("description",description);
        return page;
    }
    private static final String VISIBLE="{\"attached\":true,\"visible\":true,\"empty\":false,\"width\":1080,\"height\":1900}";
    @Test public void trackingQueryDoesNotChangeTheReviewedPage() {
        Assert.assertTrue(AndroidWebViewPageObject.sameDestination("https://vamapps.com/faq_forher?utm_source=profile","https://vamapps.com/faq_forher"));
    }
    @Test public void trailingSlashDoesNotChangeThePage() {
        Assert.assertTrue(AndroidWebViewPageObject.sameDestination("https://vamapps.com/faq_forher/","https://vamapps.com/faq_forher"));
    }
    @Test public void aDifferentApplicationPageIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination("https://vamapps.com/faq","https://vamapps.com/faq_forher"));
    }
    @Test public void aDifferentHostIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination("https://example.com/faq_forher","https://vamapps.com/faq_forher"));
    }
    @Test public void anUnexpectedPortIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination("https://vamapps.com:8443/faq_forher","https://vamapps.com/faq_forher"));
    }
    @Test public void plaintextDowngradeIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination("http://vamapps.com/faq_forher","https://vamapps.com/faq_forher"));
    }
    @Test public void invalidOrBlankDocumentsAreRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination("about:blank","https://vamapps.com/faq_forher"));
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination(null,"https://vamapps.com/faq_forher"));
    }
    @Test public void attachedVisiblePageWithTheReviewedAddressIsEligible() {
        Assert.assertTrue(AndroidWebViewPageObject.isAttachedPage(page(VISIBLE),"https://vamapps.com/faq_forher"));
    }
    @Test public void aDetachedPageWithTheSameAddressIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page(VISIBLE.replace("\"attached\":true","\"attached\":false")),"https://vamapps.com/faq_forher"));
    }
    @Test public void aHiddenPageWithTheSameAddressIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page(VISIBLE.replace("\"visible\":true","\"visible\":false")),"https://vamapps.com/faq_forher"));
    }
    @Test public void emptyAndZeroSizedViewsAreRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page(VISIBLE.replace("\"empty\":false","\"empty\":true")),"https://vamapps.com/faq_forher"));
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page(VISIBLE.replace("1080","0")),"https://vamapps.com/faq_forher"));
    }
    @Test public void unknownViewGeometryIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page("{}"),"https://vamapps.com/faq_forher"));
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page("invalid"),"https://vamapps.com/faq_forher"));
    }
    @Test public void visibleButDifferentDestinationIsRejected() {
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page(VISIBLE),"https://vamapps.com/sensualmassagew"));
    }
    @Test public void encodedPathSeparatorsDoNotMatchADifferentPath() {
        Assert.assertFalse(AndroidWebViewPageObject.sameDestination("https://vamapps.com/faq%2Fforher","https://vamapps.com/faq/forher"));
    }
    @Test public void userInfoAndInvalidPortsAreRejected() {
        for(String actual:new String[]{"https://private@vamapps.com/faq_forher","https://vamapps.com:0/faq_forher",
                "https://vamapps.com:65536/faq_forher","https://vamapps.com.evil.test/faq_forher"})
            Assert.assertFalse(AndroidWebViewPageObject.sameDestination(actual,"https://vamapps.com/faq_forher"));
    }
    @Test public void explicitHttpsDefaultPortIsAccepted() {
        Assert.assertTrue(AndroidWebViewPageObject.sameDestination("https://vamapps.com:443/faq_forher","https://vamapps.com/faq_forher"));
    }
    @Test public void malformedMetadataCannotBecomeAnAttachedPage() {
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(null,"https://vamapps.com/faq_forher"));
        Map<String,Object> bad=page(VISIBLE);bad.put("url",42);
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(bad,"https://vamapps.com/faq_forher"));
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page("null"),"https://vamapps.com/faq_forher"));
        Assert.assertFalse(AndroidWebViewPageObject.isAttachedPage(page("[]"),"https://vamapps.com/faq_forher"));
    }
    private Map<String,Object> context(String name,Object pages) {
        Map<String,Object> context=new HashMap<>();context.put("webviewName",name);context.put("pages",pages);return context;
    }
    @Test public void onlyTheOwnVisibleAttachedPageCanSelectAWindow() {
        Map<String,Object> target=page(VISIBLE);target.put("id","target");
        Map<String,Object> hidden=page(VISIBLE.replace("\"visible\":true","\"visible\":false"));hidden.put("id","hidden");
        Object contexts=Arrays.asList(context("WEBVIEW_com.vamapps.thecoach.forher",Arrays.asList(target)),
                context("WEBVIEW_com.vamapps.thecoach:renderer",Arrays.asList(hidden,target)));
        Assert.assertArrayEquals(new String[]{"WEBVIEW_com.vamapps.thecoach:renderer","target"},
                AndroidWebViewPageObject.attachedPage(contexts,"com.vamapps.thecoach","https://vamapps.com/faq_forher"));
    }
    @Test public void incompleteContextSnapshotsAndMissingPageIdsAreIgnored() {
        Assert.assertNull(AndroidWebViewPageObject.attachedPage(null,"com.vamapps.thecoach","https://vamapps.com/faq_forher"));
        Map<String,Object> target=page(VISIBLE);target.put("id","");
        Object contexts=Arrays.asList(null,42,context("WEBVIEW_com.vamapps.thecoach",Arrays.asList(null,42,target)),
                context("WEBVIEW_com.vamapps.thecoach",Collections.emptyMap()));
        Assert.assertNull(AndroidWebViewPageObject.attachedPage(contexts,"com.vamapps.thecoach","https://vamapps.com/faq_forher"));
    }
}
