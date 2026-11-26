package lib;

import io.appium.java_client.screenrecording.CanRecordScreen;
import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebDriver;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Real Android screen recording; the shared image oracle checks intermediate motion. */
public final class AndroidModalMotion implements AutoCloseable {
    private final CanRecordScreen recorder;
    private final String device;
    private final Path folder;
    private boolean stopped;
    private final Map<String,String> animationSettings=new LinkedHashMap<String,String>();
    public AndroidModalMotion(RemoteWebDriver driver, String key) throws Exception {
        recorder=(CanRecordScreen)driver;
        device = AndroidDevice.uiSettingsSerial(driver);
        folder=Files.createTempDirectory(Paths.get("target"),key+"-android-motion-");
        try {
            for(String setting:new String[]{"window_animation_scale","transition_animation_scale","animator_duration_scale"}) {
                String value=AndroidDevice.adbForSerial(device,"shell","settings","get","global",setting).trim();
                animationSettings.put(setting,value);
                AndroidDevice.adbForSerial(device,"shell","settings","put","global",setting,"1");
            }
            recorder.startRecordingScreen();
        } catch(Exception | AssertionError failure) {
            try {restoreAnimations();} catch(Exception | AssertionError recovery) {failure.addSuppressed(recovery);}
            throw failure;
        }
    }
    public Path finishRecording() throws Exception {
        if (!stopped) {
            stopped=true;
            byte[] bytes=Base64.getDecoder().decode(recorder.stopRecordingScreen());
            Path video=folder.resolve("motion.mp4"); Files.write(video,bytes);
            Allure.addAttachment("Android selector motion","video/mp4",new ByteArrayInputStream(bytes),"mp4");
            Process ffmpeg=new ProcessBuilder("ffmpeg","-nostdin","-v","error","-i",video.toString(),"-vf","fps=30",folder.resolve("frame-%05d.png").toString())
                    .redirectErrorStream(true).redirectOutput(folder.resolve("frames.log").toFile()).start();
            try {
                Assert.assertTrue("Android frame extraction timed out",ffmpeg.waitFor(30,TimeUnit.SECONDS));
                Assert.assertEquals("Android frame extraction failed",0,ffmpeg.exitValue());
            } finally { if(ffmpeg.isAlive()) ffmpeg.destroyForcibly(); }
        }
        return folder;
    }
    public void assertVerticalMotion(Rectangle card,int width,boolean opening) throws Exception {
        assertVerticalMotion(card,width,opening,false);
    }
    public void assertVerticalMotion(Rectangle card,int width,boolean opening,boolean activationSelector) throws Exception {
        File[] frames=finishRecording().toFile().listFiles((d,n)->n.startsWith("frame-")&&n.endsWith(".png"));
        Assert.assertNotNull("No Android motion frames",frames); Arrays.sort(frames);
        List<Integer> positions=new ArrayList<Integer>();
        for(File file:frames) {
            java.awt.image.BufferedImage image=ImageIO.read(file);
            int minimum=(int)((card.y-100)*(double)image.getWidth()/width);
            int y=activationSelector?findActivationSheetHandle(image,minimum):SimulatorModalMotion.findSheetHandle(image,minimum);
            if(y>=0&&(positions.isEmpty()||y!=positions.get(positions.size()-1))) positions.add(y);
        }
        Allure.addAttachment("Android sheet handle positions",positions.toString());
        SimulatorModalMotion.assertMotionPositions(positions,opening);
    }
    /** V3 fades the sheet as it moves; the handle remains a short darker gray bar. */
    public static int findActivationSheetHandle(java.awt.image.BufferedImage image,int minimumY) {
        int center=image.getWidth()/2,half=(int)(image.getWidth()*.04),outside=(int)(image.getWidth()*.10);
        int gap=Math.max(3,(int)(image.getWidth()*.012));
        for(int y=Math.max(gap,minimumY);y<image.getHeight()-gap;y++) {
            int gray=0,total=0;
            for(int x=center-half;x<=center+half;x++) {
                int rgb=image.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                if(r>=170&&r<=252&&Math.abs(r-g)<10&&Math.abs(g-b)<10)gray++;
                total+=r;
            }
            if(gray<(2*half+1)*.85)continue;
            double middle=total/(double)(2*half+1);
            if(minChannel(image.getRGB(center-outside,y))>middle+3
                    &&minChannel(image.getRGB(center+outside,y))>middle+3
                    &&minChannel(image.getRGB(center,y-gap))>middle+3
                    &&minChannel(image.getRGB(center,y+gap))>middle+3)return y;
        }
        return -1;
    }
    private static int minChannel(int rgb) {return Math.min((rgb>>16)&255,Math.min((rgb>>8)&255,rgb&255));}
    private void restoreAnimations() throws Exception {
        for(Map.Entry<String,String> entry:animationSettings.entrySet()) {
            if("null".equals(entry.getValue())) AndroidDevice.adbForSerial(device,"shell","settings","delete","global",entry.getKey());
            else AndroidDevice.adbForSerial(device,"shell","settings","put","global",entry.getKey(),entry.getValue());
        }
        animationSettings.clear();
    }
    @Override public void close() throws Exception {
        try {finishRecording();} finally {restoreAnimations();}
    }
}
