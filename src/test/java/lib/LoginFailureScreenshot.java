package lib;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;

/** Preserves the actual login error screenshot, masking input and keyboard in memory. */
public final class LoginFailureScreenshot {
    private LoginFailureScreenshot() { }

    public static byte[] redact(byte[] png, String source) throws Exception {
        if ((!source.contains("ENTER THE MAIL THAT IS LINKED TO YOUR ACCOUNT")
                && !source.contains("ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS"))
                || source.contains("ENTER SECURITY CODE")) {
            throw new IllegalArgumentException("Unsupported credential screen for screenshot redaction");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(source.getBytes("UTF-8")));
        NodeList applications = document.getElementsByTagName("XCUIElementTypeApplication");
        if (applications.getLength() != 1) throw new IllegalArgumentException("Unknown screenshot coordinate system");
        Element app = (Element) applications.item(0);
        double width = Double.parseDouble(app.getAttribute("width"));
        double height = Double.parseDouble(app.getAttribute("height"));
        if (width <= 0 || height <= 0 || !"0".equals(app.getAttribute("x")) || !"0".equals(app.getAttribute("y"))) {
            throw new IllegalArgumentException("Invalid screenshot coordinate system");
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null) throw new IllegalArgumentException("Unreadable screenshot");
        double sx = image.getWidth() / width, sy = image.getHeight() / height;
        if (Math.abs(sx - sy) > .02) throw new IllegalArgumentException("Screenshot/source dimensions disagree");
        Graphics2D graphics = image.createGraphics();
        int inputs = 0;
        try {
            graphics.setColor(Color.BLACK);
            NodeList elements = document.getElementsByTagName("*");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if (!"true".equals(element.getAttribute("visible"))) continue;
                boolean input = element.getTagName().endsWith("TextField");
                boolean sensitive = input || element.getTagName().equals("XCUIElementTypeKeyboard");
                for (String attr : new String[]{"name", "label", "value"}) {
                    String text = element.getAttribute(attr);
                    sensitive |= text.contains("@") || !text.equals(TestData.sanitizeSensitiveData(text));
                }
                if (!sensitive) continue;
                double x = Double.parseDouble(element.getAttribute("x"));
                double y = Double.parseDouble(element.getAttribute("y"));
                double w = Double.parseDouble(element.getAttribute("width"));
                double h = Double.parseDouble(element.getAttribute("height"));
                if (input && (w <= 0 || h <= 0)) throw new IllegalArgumentException("Input has no redaction bounds");
                if (input) inputs++;
                // Padding covers anti-aliased glyph edges; source coordinates are points, PNG uses pixels.
                graphics.fillRect((int) Math.floor((x - 3) * sx), (int) Math.floor((y - 3) * sy),
                        (int) Math.ceil((w + 6) * sx), (int) Math.ceil((h + 6) * sy));
            }
        } finally {
            graphics.dispose();
        }
        if (inputs == 0) throw new IllegalArgumentException("Login input was not located for redaction");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
