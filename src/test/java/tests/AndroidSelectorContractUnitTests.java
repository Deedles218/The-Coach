package tests;

import lib.ui.android.AndroidProgramSelectorPageObject;
import org.junit.*;
import org.openqa.selenium.Rectangle;
import java.util.*;

/** Retain legacy assertions while checking complete visible v3 membership. */
public class AndroidSelectorContractUnitTests {
    private String previous;
    private static class Screen extends AndroidProgramSelectorPageObject {
        Screen() {super(null);}
        static void motion(Map<String,Rectangle> before,Map<String,Rectangle> after,boolean scrollable,Set<String> expected) {
            assertProgramListMotion(before,after,scrollable,expected);
        }
    }
    @Before public void saveContract() {previous=System.getProperty("coach.selector.contract");System.clearProperty("coach.selector.contract");}
    @After public void restoreContract() {
        if(previous==null)System.clearProperty("coach.selector.contract");else System.setProperty("coach.selector.contract",previous);
    }
    private Map<String,Rectangle> viewport(int x,int y,String... titles) {
        Map<String,Rectangle> result=new LinkedHashMap<>();
        for(String title:titles) {result.put(title,new Rectangle(x,y,40,100));y+=50;}
        return result;
    }
    private Set<String> expected() {return new LinkedHashSet<>(Arrays.asList("Main","Secondary"));}
    private void activation() {System.setProperty("coach.selector.contract","activation-v3");}
    @Test public void unspecifiedContractKeepsLegacy() {Assert.assertFalse(Screen.isActivationFlow());}
    @Test public void activationContractIsExplicit() {activation();Assert.assertTrue(Screen.isActivationFlow());}
    @Test(expected=AssertionError.class) public void unknownContractIsRejected() {
        System.setProperty("coach.selector.contract","v4");Screen.isActivationFlow();
    }
    @Test(expected=AssertionError.class) public void legacyStillRequiresVerticalMovement() {
        Screen.motion(viewport(0,100,"Main","Secondary"),viewport(0,100,"Main","Secondary"),false,expected());
    }
    @Test public void legacyVerticalMovementStillPasses() {
        Screen.motion(viewport(0,100,"Main","Secondary"),viewport(0,50,"Main","Secondary"),true,Collections.emptySet());
    }
    @Test public void nonScrollingActivationListMustExposeEverySavedProgram() {
        activation();Screen.motion(viewport(0,100,"Main","Secondary"),viewport(0,100,"Main","Secondary"),false,expected());
    }
    @Test(expected=AssertionError.class) public void nonScrollingActivationListCannotHideAProgram() {
        activation();Screen.motion(viewport(0,100,"Main"),viewport(0,100,"Main"),false,expected());
    }
    @Test(expected=AssertionError.class) public void activationCannotInferMembershipFromVisibleRows() {
        activation();Screen.motion(viewport(0,100,"Main"),viewport(0,100,"Main"),false,Collections.emptySet());
    }
    @Test(expected=AssertionError.class) public void scrollableActivationListStillMustMove() {
        activation();Screen.motion(viewport(0,100,"Main","Secondary"),viewport(0,100,"Main","Secondary"),true,expected());
    }
    @Test(expected=AssertionError.class) public void horizontalMovementIsRejected() {
        activation();Screen.motion(viewport(0,100,"Main","Secondary"),viewport(20,50,"Main","Secondary"),true,expected());
    }
}
