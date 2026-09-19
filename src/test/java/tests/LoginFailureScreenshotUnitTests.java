package tests;

import lib.LoginFailureScreenshot;
import org.junit.Test;
import org.junit.Assert;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public class LoginFailureScreenshotUnitTests {
    private String source(String extra) {
        return "<AppiumAUT><XCUIElementTypeApplication x='0' y='0' width='100' height='200'>"
                + "<XCUIElementTypeStaticText name='ENTER THE MAIL THAT IS LINKED TO YOUR ACCOUNT'/>"
                + extra + "</XCUIElementTypeApplication></AppiumAUT>";
    }
    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(200, 400, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 400; y++) for (int x = 0; x < 200; x++) image.setRGB(x,y,0xffffff);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image,"png",out);
        return out.toByteArray();
    }
    @Test public void masksScaledInputAndKeyboardButRetainsErrorArea() throws Exception {
        String xml = source("<XCUIElementTypeTextField visible='true' x='10' y='40' width='80' height='10'/>"
                + "<XCUIElementTypeKeyboard visible='true' x='0' y='120' width='100' height='80'/>");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(LoginFailureScreenshot.redact(png(),xml)));
        Assert.assertEquals(0xff000000,result.getRGB(25,85));
        Assert.assertEquals(0xff000000,result.getRGB(25,260));
        Assert.assertEquals(0xffffffff,result.getRGB(25,150));
    }
    @Test(expected = IllegalArgumentException.class) public void refusesScreenWithoutLocatedInput() throws Exception {
        LoginFailureScreenshot.redact(png(),source(""));
    }
    @Test public void masksConnectMailFormToo() throws Exception {
        String xml = source("<XCUIElementTypeTextField visible='true' x='10' y='40' width='80' height='10'/>")
                .replace("ENTER THE MAIL THAT IS LINKED TO YOUR ACCOUNT", "ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(LoginFailureScreenshot.redact(png(),xml)));
        Assert.assertEquals(0xff000000,result.getRGB(25,85));
    }
    @Test(expected = IllegalArgumentException.class) public void refusesOtpScreen() throws Exception {
        LoginFailureScreenshot.redact(png(),source("<label name='ENTER SECURITY CODE'/>"));
    }
}
