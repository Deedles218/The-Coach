package lib.ui.android;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import lib.Platform;
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.json.Json;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Read the rendered document of this application's WebView, then return to native UI. */
public final class AndroidWebViewPageObject extends MainPageObject {
    public AndroidWebViewPageObject(RemoteWebDriver driver) {super(driver);}
    public static boolean sameDestination(String actual,String expected) {
        try {
            URI a=URI.create(actual),e=URI.create(expected);
            return "https".equalsIgnoreCase(a.getScheme())&&"https".equalsIgnoreCase(e.getScheme())
                    &&e.getHost()!=null&&e.getHost().equalsIgnoreCase(a.getHost())
                    &&a.getRawUserInfo()==null&&e.getRawUserInfo()==null
                    &&a.getPort()!=0&&e.getPort()!=0&&a.getPort()<=65535&&e.getPort()<=65535
                    &&(a.getPort()<0?443:a.getPort())==(e.getPort()<0?443:e.getPort())
                    &&a.getRawPath().replaceAll("/+$","").equals(e.getRawPath().replaceAll("/+$",""));
        } catch(IllegalArgumentException|NullPointerException invalid) {return false;}
    }
    public static boolean isAttachedPage(Map<?,?> page,String expectedUrl) {
        if(page==null||!"page".equals(page.get("type"))||!(page.get("url") instanceof String)
                ||!sameDestination((String)page.get("url"),expectedUrl)
                ||!(page.get("description") instanceof String))return false;
        try {
            Map<?,?> view=new Json().toType((String)page.get("description"),Map.class);
            return Boolean.TRUE.equals(view.get("attached"))&&Boolean.TRUE.equals(view.get("visible"))
                    &&Boolean.FALSE.equals(view.get("empty"))
                    &&view.get("width") instanceof Number&&((Number)view.get("width")).doubleValue()>0
                    &&view.get("height") instanceof Number&&((Number)view.get("height")).doubleValue()>0;
        } catch(RuntimeException invalidDescription) {return false;}
    }
    /** Fail closed on incomplete DevTools metadata and ignore other applications. */
    public static String[] attachedPage(Object contexts,String appPackage,String expectedUrl) {
        if(!(contexts instanceof List))return null;
        String own="WEBVIEW_"+appPackage;
        for(Object entry:(List<?>)contexts) {
            if(!(entry instanceof Map))continue;
            Map<?,?> mapping=(Map<?,?>)entry;
            Object context=mapping.get("webviewName"),pages=mapping.get("pages");
            if(!(context instanceof String)||!(own.equals(context)||((String)context).startsWith(own+":"))
                    ||!(pages instanceof List))continue;
            for(Object candidate:(List<?>)pages) {
                if(!(candidate instanceof Map))continue;
                Map<?,?> target=(Map<?,?>)candidate;
                if(isAttachedPage(target,expectedUrl)&&target.get("id") instanceof String
                        &&!((String)target.get("id")).trim().isEmpty())
                    return new String[]{(String)context,(String)target.get("id")};
            }
        }
        return null;
    }
    public void assertLoaded(String expectedUrl,String expectedText) {
        Assert.assertTrue("A reviewed HTTPS WebView destination is required",sameDestination(expectedUrl,expectedUrl));
        Assert.assertTrue("Reviewed WebView text is required",expectedText!=null&&!expectedText.trim().isEmpty());
        Assert.assertTrue("Android WebView verification requires AndroidDriver",driver instanceof AndroidDriver);
        AndroidDriver android=(AndroidDriver)driver;
        String original=android.getContext();
        String[] page=createWait(35).withMessage("The application's attached WebView page is absent: "+expectedUrl)
                .until(d -> attachedPage(((JavascriptExecutor)driver).executeScript("mobile: getContexts",Collections.emptyMap()),
                        Platform.getInstance().getAndroidAppPackage(),expectedUrl));
        try {
            android.context(page[0]);
            String window=createWait(10).withMessage("The attached WebView page has no browser window")
                    .until(d -> driver.getWindowHandles().stream().filter(h -> h.equals(page[1])||h.equals("CDwindow-"+page[1]))
                            .findFirst().orElse(null));
            driver.switchTo().window(window);
            createWait(35).withMessage("The configured WebView document did not load: "+expectedUrl).until(d -> {
                if(!sameDestination(driver.getCurrentUrl(),expectedUrl))return false;
                return Boolean.TRUE.equals(((JavascriptExecutor)driver).executeScript(
                        "return document.readyState==='complete' && document.visibilityState==='visible' && !!document.body && "
                        +"document.body.innerText.replace(/\\s+/g,' ').includes(arguments[0]);",
                        expectedText.trim().replaceAll("\\s+"," ")));
            });
            URI url=URI.create(driver.getCurrentUrl());
            Allure.addAttachment("Verified WebView document",url.getScheme()+"://"+url.getHost()+url.getPath()+"\n"+driver.getTitle());
        } finally {android.context(original);}
    }
}
