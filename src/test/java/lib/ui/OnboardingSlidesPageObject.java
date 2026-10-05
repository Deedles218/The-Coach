package lib.ui;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lib.Platform;
import lib.TestData;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Expectations are captured from the installed app's active, goal-filtered configuration. */
public abstract class OnboardingSlidesPageObject extends MainPageObject implements AutoCloseable {
    private final Properties fixture = new Properties();
    private boolean recording;
    private boolean monitoring;
    private int recordingNumber;
    private Dimension screenSize;
    protected OnboardingSlidesPageObject(RemoteWebDriver driver) throws IOException {
        super(driver);
        String path = System.getProperty("onboarding.slides.fixture");
        Assert.assertNotNull("Capture active configuration and set -Donboarding.slides.fixture=<properties>", path);
        try (java.io.Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            fixture.load(reader);
        }
        Assert.assertEquals("Fixture belongs to another installed app", (Platform.getInstance().isAndroid() ? Platform.getInstance().getAndroidAppPackage()
                        : Platform.getInstance().getIOSBundleId()),
                fixture.getProperty(Platform.getInstance().isAndroid() ? "appPackage" : "bundleId"));
        Assert.assertNull("Active slide configuration is invalid: " + fixture.getProperty("validationError"),
                fixture.getProperty("validationError"));
        Assert.assertTrue("Configuration must contain at least two slides", count() >= 2);
        for (int page = 1; page <= count(); page++) {
            if (fixture.getProperty(page + ".imageUrl").toLowerCase(Locale.US).contains(".gif")) {
                Assert.assertNotNull("Recapture GIF frame metadata", fixture.getProperty(page + ".gifFrameCount"));
            }
        }
    }

    protected abstract String headerLocator(int page);
    protected abstract String buttonLocator(int page);
    protected abstract By indicatorLocator();
    protected abstract By imageLocator();
    protected abstract By closeLocator();
    protected abstract void assertIndicator(int page);
    protected abstract void swipePlatform(String direction);
    protected abstract void startPlatformRecording(Dimension size);
    protected abstract String stopPlatformRecording();
    protected String buttonText(WebElement button) { return button.getText(); }
    protected String configured(String key) { return fixture.getProperty(key); }

    public int count() { return Integer.parseInt(fixture.getProperty("count")); }
    public String header(int page) { return fixture.getProperty(page + ".header"); }
    public String goal() { return fixture.getProperty("goal"); }
    public String[] headers() {
        String[] headers = new String[count()];
        for (int page = 1; page <= count(); page++) headers[page - 1] = header(page);
        return headers;
    }

    @Step("Record media before the first slide opens, including finite GIF playback")
    public void startMediaRecording() {
        monitoring = true;
        ensureRecording();
    }

    private void ensureRecording() {
        if (recording) return;
        screenSize = driver.manage().window().getSize();
        startPlatformRecording(screenSize);
        recording = true;
    }

    public void finishMediaRecording() {
        monitoring = false;
        saveRecording();
    }

    private Path saveRecording() {
        if (!recording) return null;
        recording = false;
        String encoded = stopPlatformRecording();
        Assert.assertFalse("Media recording is empty", encoded.isEmpty());
        try {
            Path folder = Paths.get(System.getProperty("onboarding.slides.artifacts", "target/onboarding-slides/evidence"))
                    .resolve("recording-" + (++recordingNumber));
            Files.createDirectories(folder);
            Path video = folder.resolve("media.mp4");
            byte[] bytes = Base64.getDecoder().decode(encoded);
            Files.write(video, bytes);
            Allure.addAttachment("Product onboarding media recording", "video/mp4",
                    new ByteArrayInputStream(bytes), "mp4");
            return video;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot save media recording", error);
        }
    }

    @Step("Assert configured slide {page}, indicator, bottom CTA and absence of early close")
    public Rectangle assertSlide(int page) {
        waitForElementVisible(headerLocator(page), "Configured header missing on slide " + page, 20);
        assertIndicator(page);
        WebElement button = waitForElementEnabled(buttonLocator(page),
                "Configured CTA missing on slide " + page, 10);
        Assert.assertEquals("Wrong configured CTA on slide " + page,
                fixture.getProperty(page + ".buttonText"), buttonText(button));
        Rectangle bounds = button.getRect();
        Assert.assertTrue("CTA is not pinned below the page indicator",
                bounds.getY() >= driver.findElement(indicatorLocator()).getRect().getY());
        Assert.assertTrue("CTA is outside the screen", bounds.getY() + bounds.getHeight()
                <= driver.manage().window().getSize().getHeight());
        Assert.assertTrue("Onboarding must not expose Skip/Close before completion",
                driver.findElements(closeLocator()).isEmpty());
        return bounds;
    }

    @Step("Swipe {direction} to configured slide {expectedPage}")
    public void swipe(String direction, int expectedPage) {
        if (monitoring) ensureRecording();
        swipePlatform(direction);
        assertSlide(expectedPage);
    }

    @Step("Tap configured CTA on slide {page}")
    public void tapButton(int page) {
        waitForElementAndClick(buttonLocator(page), "Cannot tap slide CTA", 10);
    }

    @Step("Verify configured media renders and GIF changes inside its image bounds on slide {page}")
    public void assertMedia(int page) {
        WebElement media = createWait(20).withMessage("Slide image is missing").until(webDriver -> {
            List<WebElement> images = driver.findElements(imageLocator());
            Assert.assertTrue("Ambiguous media: more than one visible image", images.size() <= 1);
            return images.isEmpty() ? null : images.get(0);
        });
        Rectangle rectangle = media.getRect();
        Assert.assertTrue("Slide media has empty bounds", rectangle.getWidth() > 0 && rectangle.getHeight() > 0);
        final byte[][] readyScreenshot = new byte[1][];
        createWait(20).withMessage("Media region remains blank on slide " + page).until(webDriver -> {
            byte[] candidate = screenshot();
            if (!hasContent(crop(candidate, rectangle))) return false;
            readyScreenshot[0] = candidate;
            return true;
        });
        byte[] first = readyScreenshot[0];
        attach("slide-" + page + "-media-first", first);
        BufferedImage baseline = crop(first, rectangle);
        if (Integer.parseInt(fixture.getProperty(page + ".gifFrameCount", "1")) > 1) {
            try {
                createWait(15).withMessage("GIF remains static on slide " + page + " (" + header(page) + ")")
                    .until(webDriver -> {
                        byte[] next = screenshot();
                        if (changedPixels(baseline, crop(next, rectangle)) > 0.002) {
                            attach("slide-" + page + "-media-changed", next);
                            return true;
                        }
                        return false;
                    });
            } catch (TimeoutException noLiveMotion) {
                // Finite GIFs may have ended while accessibility assertions ran.
                // Require actual motion in earlier frames, scoped to this exact header.
                Assert.assertTrue("No GIF motion in live samples or recorded slide " + page,
                        recordedMotion(page, rectangle, first));
            }
        }
    }

    private boolean recordedMotion(int page, Rectangle media, byte[] reference) {
        Path video = saveRecording();
        Assert.assertNotNull("GIF motion needs recording armed before slide entry", video);
        try {
            Path folder = video.getParent();
            Path framesFolder = Files.createTempDirectory(folder, "frames-");
            Process extraction = new ProcessBuilder("ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
                    "-i", video.toString(), "-vf", "fps=5", framesFolder.resolve("frame-%05d.png").toString())
                    .redirectErrorStream(true).redirectOutput(folder.resolve("extraction.log").toFile()).start();
            if (!extraction.waitFor(60, TimeUnit.SECONDS)) {
                extraction.destroyForcibly();
                throw new IllegalStateException("Recorded frame extraction timed out: " + folder);
            }
            Assert.assertEquals("Recorded frame extraction failed: " + folder, 0, extraction.exitValue());
            Rectangle title = driver.findElement(getLocatorByString(headerLocator(page))).getRect();
            return recordedFramesShowMotion(page, framesFolder, title, media, reference);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot inspect recorded GIF motion", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GIF motion inspection interrupted", error);
        }
    }

    private boolean recordedFramesShowMotion(int page, Path framesFolder, Rectangle title,
                                            Rectangle media, byte[] reference) throws IOException {
        BufferedImage expected = ImageIO.read(new ByteArrayInputStream(reference));
        BufferedImage expectedTitle = resize(cropImage(expected, title, 0), title.getWidth(), title.getHeight());
        BufferedImage firstMedia = null;
        byte[] firstFrame = null;
        List<Path> frames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(framesFolder, "frame-*.png")) {
            for (Path frame : stream) frames.add(frame);
        }
        Collections.sort(frames);
        for (Path path : frames) {
            BufferedImage frame = ImageIO.read(path.toFile());
            BufferedImage actualTitle = resize(cropImage(frame, title, 0), title.getWidth(), title.getHeight());
            // Screenshot scaling and video compression alter glyph-edge pixels.
            // Compare average color distance for the header; media motion retains
            // the stricter changed-pixel check below.
            if (averageColorDistance(expectedTitle, actualTitle) > 0.04) continue;
            BufferedImage region = cropImage(frame, media, 0.1);
            if (!hasContent(region)) continue;
            if (firstMedia == null) {
                firstMedia = region;
                firstFrame = Files.readAllBytes(path);
            } else if (changedPixels(firstMedia, region) > 0.002) {
                attach("slide-" + page + "-recorded-first", firstFrame);
                attach("slide-" + page + "-recorded-changed", Files.readAllBytes(path));
                return true;
            }
        }
        return false;
    }

    @Step("Assert all product slide headers and indicator are absent")
    public void assertAbsent() {
        for (int page = 1; page <= count(); page++) {
            Assert.assertTrue("Onboarding header still exists: " + header(page),
                    driver.findElements(getLocatorByString(headerLocator(page))).isEmpty());
        }
        Assert.assertTrue("Product onboarding indicator still exists", driver.findElements(indicatorLocator()).isEmpty());
    }

    @Step("Save the failed media state on slide {page}")
    public void captureMediaFailure(int page) {
        attach("slide-" + page + "-media-failure", screenshot());
        byte[] source = TestData.sanitizeSensitiveData(driver.getPageSource()).getBytes(StandardCharsets.UTF_8);
        Allure.addAttachment("Slide " + page + " failure source", "text/xml",
                new ByteArrayInputStream(source), "xml");
        try {
            Path directory = Paths.get(System.getProperty("onboarding.slides.artifacts", "target/onboarding-slides/evidence"));
            Files.createDirectories(directory);
            Files.write(directory.resolve("slide-" + page + "-media-failure.xml"), source);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot save media failure source", error);
        }
    }

    private byte[] screenshot() { return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES); }

    private BufferedImage crop(byte[] screenshot, Rectangle rectangle) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot));
            if (screenSize == null) screenSize = driver.manage().window().getSize();
            return cropImage(image, rectangle, 0.1);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot decode media screenshot", error);
        }
    }

    private BufferedImage cropImage(BufferedImage image, Rectangle rectangle, double inset) {
        double sx = image.getWidth() / (double) screenSize.getWidth();
        double sy = image.getHeight() / (double) screenSize.getHeight();
        int x = (int) ((rectangle.getX() + rectangle.getWidth() * inset) * sx);
        int y = (int) ((rectangle.getY() + rectangle.getHeight() * inset) * sy);
        return image.getSubimage(x, y, (int) (rectangle.getWidth() * (1 - 2 * inset) * sx),
                (int) (rectangle.getHeight() * (1 - 2 * inset) * sy));
    }

    private BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = result.createGraphics();
        try { graphics.drawImage(source, 0, 0, width, height, null); }
        finally { graphics.dispose(); }
        return result;
    }

    @Override
    public void close() { finishMediaRecording(); }

    private boolean hasContent(BufferedImage image) {
        int reference = image.getRGB(0, 0), different = 0, total = 0;
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                total++;
                if (pixelDistance(reference, image.getRGB(x, y)) > 30) different++;
            }
        }
        return different > total * 0.01;
    }

    private double changedPixels(BufferedImage before, BufferedImage after) {
        Assert.assertEquals("Media width changed", before.getWidth(), after.getWidth());
        Assert.assertEquals("Media height changed", before.getHeight(), after.getHeight());
        int changed = 0, total = 0;
        for (int y = 0; y < before.getHeight(); y += 4) {
            for (int x = 0; x < before.getWidth(); x += 4) {
                total++;
                if (pixelDistance(before.getRGB(x, y), after.getRGB(x, y)) > 30) changed++;
            }
        }
        return changed / (double) total;
    }

    private double averageColorDistance(BufferedImage before, BufferedImage after) {
        Assert.assertEquals(before.getWidth(), after.getWidth());
        Assert.assertEquals(before.getHeight(), after.getHeight());
        long distance = 0;
        int total = 0;
        for (int y = 0; y < before.getHeight(); y += 4) {
            for (int x = 0; x < before.getWidth(); x += 4) {
                distance += pixelDistance(before.getRGB(x, y), after.getRGB(x, y));
                total++;
            }
        }
        return distance / (total * 765.0);
    }

    private int pixelDistance(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
    }

    private void attach(String name, byte[] bytes) {
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
        try {
            Path directory = Paths.get(System.getProperty("onboarding.slides.artifacts", "target/onboarding-slides/evidence"));
            Files.createDirectories(directory);
            Files.write(directory.resolve(name + ".png"), bytes);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot save slide evidence", error);
        }
    }
}
