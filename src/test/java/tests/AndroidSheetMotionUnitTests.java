package tests;

import lib.AndroidModalMotion;
import lib.SimulatorModalMotion;
import org.junit.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class AndroidSheetMotionUnitTests {
    private BufferedImage image(Color background,Color handle,int y) {
        BufferedImage image=new BufferedImage(300,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(background);g.fillRect(0,0,300,600);
        g.setColor(handle);g.fillRect(132,y,36,4);g.dispose();return image;
    }
    private int find(BufferedImage image) {return AndroidModalMotion.findActivationSheetHandle(image,100);}
    private void assertOnHandle(BufferedImage image,int top) {
        int y=find(image);Assert.assertTrue("Detected row is outside the rendered handle: "+y,y>=top&&y<top+4);
    }
    @Test public void opaqueHandleIsLocated() {assertOnHandle(image(Color.WHITE,new Color(210,210,210),150),150);}
    @Test public void fadingHandleIsLocated() {assertOnHandle(image(Color.WHITE,new Color(246,246,246),180),180);}
    @Test public void fadingHandleOnGraySurfaceIsLocated() {assertOnHandle(image(new Color(245,245,245),new Color(230,230,230),180),180);}
    @Test public void flatBackdropIsNotAHandle() {Assert.assertEquals(-1,find(image(new Color(210,210,210),new Color(210,210,210),180)));}
    @Test public void coloredBackdropDoesNotSatisfyTheSurface() {Assert.assertEquals(-1,find(image(Color.RED,new Color(210,210,210),180)));}
    @Test public void fullWidthDividerIsNotAHandle() {
        BufferedImage im=image(Color.WHITE,Color.WHITE,180);Graphics2D g=im.createGraphics();g.setColor(new Color(210,210,210));g.fillRect(0,180,300,4);g.dispose();
        Assert.assertEquals(-1,find(im));
    }
    @Test public void textIsNotAHandle() {
        BufferedImage im=image(Color.WHITE,Color.WHITE,180);Graphics2D g=im.createGraphics();g.setColor(new Color(210,210,210));g.drawString("Stage 2 of 7",120,180);g.dispose();
        Assert.assertEquals(-1,find(im));
    }
    @Test(expected=AssertionError.class) public void fadingWithoutMovementStillFails() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(find(image(Color.WHITE,new Color(210,210,210),180)),find(image(Color.WHITE,new Color(230,230,230),180)),find(image(Color.WHITE,new Color(246,246,246),180))),false);
    }
    @Test public void movingFadedHandleRetainsIntermediateMotionChecks() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(find(image(Color.WHITE,new Color(210,210,210),150)),find(image(Color.WHITE,new Color(230,230,230),180)),find(image(Color.WHITE,new Color(246,246,246),240))),false);
    }
}
