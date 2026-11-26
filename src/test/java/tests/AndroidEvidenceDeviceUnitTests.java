package tests;

import lib.AndroidDevice;
import org.junit.*;
import java.lang.reflect.*;
import java.util.*;

/** USB evidence opt-in cannot relax the existing emulator-only operations. */
public class AndroidEvidenceDeviceUnitTests {
    private final Map<String,String> original=new HashMap<String,String>();
    @Before public void remember(){
        for(String key:new String[]{"android.udid","android.appPackage","coach.android.physicalUiSettings",
                "coach.emailRelease.dedicatedDevice"})original.put(key,System.getProperty(key));
        System.setProperty("android.udid","qa_usb_device");System.setProperty("android.appPackage","com.vamapps.thecoach");
        System.setProperty("coach.android.physicalUiSettings","true");
        System.clearProperty("coach.emailRelease.dedicatedDevice");
    }
    @After public void restore(){
        for(Map.Entry<String,String> e:original.entrySet())if(e.getValue()==null)System.clearProperty(e.getKey());else System.setProperty(e.getKey(),e.getValue());
    }
    private String serial(String pkg)throws Exception{
        Method method=AndroidDevice.class.getDeclaredMethod("appPrivateEvidenceSerial",String.class);method.setAccessible(true);
        try{return (String)method.invoke(null,pkg);}
        catch(InvocationTargetException e){if(e.getCause() instanceof AssertionError)throw (AssertionError)e.getCause();throw e;}
    }
    @Test(expected=AssertionError.class) public void noDebugBindingStillRejectsPhysicalDevice()throws Exception{serial(null);}
    @Test(expected=AssertionError.class) public void debugBindingNeedsPhysicalOptIn()throws Exception{
        System.setProperty("coach.android.physicalUiSettings","false");serial("com.vamapps.thecoach");
    }
    @Test(expected=AssertionError.class) public void anotherAppCannotBorrowTheBinding()throws Exception{serial("another.app");}
    @Test public void matchingDebugBindingAllowsExplicitUsbDevice()throws Exception{Assert.assertEquals("qa_usb_device",serial("com.vamapps.thecoach"));}
    @Test public void matchingHerBindingIsSupported()throws Exception{
        System.setProperty("android.appPackage","com.vamapps.thecoach.forher");Assert.assertEquals("qa_usb_device",serial("com.vamapps.thecoach.forher"));
    }
    @Test public void defaultEmulatorPathAndItsStrictGuardArePreserved()throws Exception{
        System.setProperty("android.udid","emulator-5554");Assert.assertEquals("emulator-5554",serial(null));Assert.assertEquals("emulator-5554",serial(""));
    }
    @Test(expected=AssertionError.class) public void evidenceOptInCannotAuthorizeEmailInstallationReset(){
        AndroidDevice.emailRegressionSerial();
    }
    @Test(expected=AssertionError.class) public void anotherDedicatedDeviceCannotAuthorizeEmailInstallationReset(){
        System.setProperty("coach.emailRelease.dedicatedDevice","another_usb_device");
        AndroidDevice.emailRegressionSerial();
    }
    @Test public void explicitDedicatedDeviceAuthorizesEmailRegression(){
        System.setProperty("coach.emailRelease.dedicatedDevice","qa_usb_device");
        Assert.assertEquals("qa_usb_device",AndroidDevice.emailRegressionSerial());
    }
    @Test public void disposableEmulatorNeedsNoPhysicalDeviceApproval(){
        System.setProperty("android.udid","emulator-5554");
        Assert.assertEquals("emulator-5554",AndroidDevice.emailRegressionSerial());
    }
    @Test(expected=AssertionError.class) public void emailAdbRejectsAnotherSerialBeforeRunningACommand()throws Exception{
        System.setProperty("coach.emailRelease.dedicatedDevice","qa_usb_device");
        AndroidDevice.adbForEmailRegression("another_usb_device","version");
    }
}
