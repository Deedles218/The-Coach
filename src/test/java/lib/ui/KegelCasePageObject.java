package lib.ui;

import lib.Platform;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.openqa.selenium.TimeoutException;
import lib.ui.android.AndroidProgramSelectorPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.Rectangle;

/** Build-specific observed selectors, shared UI actions for the October Jira cases. */
public final class KegelCasePageObject extends MainPageObject {
    private final Properties selectors = new Properties();
    private DailyPlanPageObject daily;
    private String[] originalProgram;
    private String[] kegelProgram;
    private String originalStretching;

    public KegelCasePageObject(RemoteWebDriver driver) throws IOException {
        super(driver);
        String path = System.getProperty("coach.kegelUi.fixture");
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalStateException("Supply -Dcoach.kegelUi.fixture=<observed build selectors.properties>; see docs/kegel-october-cases-setup.md");
        }
        try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            selectors.load(reader);
        }
        Assert.assertEquals("Selector fixture belongs to another platform",
                Platform.getInstance().getPlatformVar(), required("platform"));
        required("app.version");
        required("environment");
        if (usesProfileFreeAndroidSession()) {
            Assert.assertTrue("Profile-free session fixture is Android-only", Platform.getInstance().isAndroid());
            io.appium.java_client.android.AndroidDriver android = (io.appium.java_client.android.AndroidDriver) driver;
            android.setSetting("enableMultiWindows", true);
            // Exercise animation does not become idle; actions still use explicit UI waits.
            android.setSetting("waitForIdleTimeout", 1000);
        }
    }

    public String required(String key) {
        String value = selectors.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Observed selector/data missing: " + key);
        }
        return value.trim();
    }

    public void requireKeys(String... keys) {
        for (String key : keys) required(key);
    }

    public WebElement visible(String key) {
        return waitForElementVisible(required(key), "UI state absent: " + key, 10);
    }

    public void tap(String key) {
        waitForElementAndClick(required(key), "Control unavailable: " + key, 10);
    }

    public void gone(String key) {
        waitForElementNotVisible(required(key), "UI state did not close: " + key, 10);
    }

    public boolean usesQuizProfileLogin() {
        return "quiz-profile".equals(selectors.getProperty("auth.flow"));
    }

    public boolean usesProfileFreeAndroidSession() {
        return "android-session".equals(selectors.getProperty("auth.flow"));
    }

    /** Logout may end on the observed Quiz instead of Welcome on this prod build. */
    public void ensureLoggedOutOnObservedStart(CoachFlowPageObject coach) {
        try {
            coach.ensureLoggedOutOnStartScreen();
        } catch (TimeoutException alternateDestination) {
            if (!usesQuizProfileLogin() || !isElementVisible(required("auth.quiz"))) throw alternateDestination;
        }
    }

    /** Build-observed alternate login preparation; never used by the marketing-link assertion. */
    public void loginExistingAccount(CoachFlowPageObject coach, String email, String otp) {
        if (usesProfileFreeAndroidSession()) {
            Assert.assertTrue("Declare the account entered through UI in COACH_KEGEL_SESSION_EMAIL",
                    email.equals(System.getenv("COACH_KEGEL_SESSION_EMAIL")));
            // The operator establishes this dedicated session through email/OTP first.
            // Never open Profile to verify it: this build crashes on that screen.
            new AndroidProgramSelectorPageObject(driver).dismissSelectorIfOpen();
            coach.waitForAuthorizedDashboard();
            return;
        }
        if (!usesQuizProfileLogin()) {
            coach.ensureExistingProgressUserIsLoggedIn(email, otp);
            return;
        }
        requireKeys("auth.quiz", "auth.confirm", "auth.changeEmail", "auth.emailForm", "auth.existingLogin",
                "auth.profileReady", "auth.goal");
        // A previous link inspection can leave an unconfirmed profile open.
        if (!isElementVisible(required("auth.confirm"))) {
            ensureLoggedOutOnObservedStart(coach);
            if (!isElementVisible(required("auth.quiz"))) {
                coach.loginWithEmailAndOtp(email, otp);
                return;
            }
            OnboardingPageObject onboarding = OnboardingPageObjectFactory.get(driver);
            onboarding.selectGoal(OnboardingGoal.fromConfiguredValue(required("auth.goal")));
            onboarding.completeQuestionnaire();
            onboarding.closePaywallsAndPopups();
            onboarding.waitForToday();
            coach.openProfile();
        }
        waitForElementVisible(required("auth.profileReady"), "Profile did not finish loading", 30);
        tap("auth.confirm");
        tap("auth.changeEmail");
        visible("auth.emailForm");
        coach.typeLoginEmail(email);
        coach.submitEmailAndOpenExistingAccountLoginStep();
        tap("auth.existingLogin");
        coach.assertOtpScreenIsDisplayedForEmail(email);
        coach.typeSecurityCode(otp);
        coach.completeAuthorizationAfterSecurityCode();
        // Today can briefly render the previous anonymous profile's cached progress.
        coach.openProfile();
        String nickname = email.substring(0, email.indexOf('@'));
        waitForElementVisible("xpath://XCUIElementTypeStaticText[@name=" + xpathLiteral(nickname) + "]",
                "Authenticated profile has not loaded", 30);
        waitForElementVisible(required("auth.profileReady"), "Authenticated profile controls did not load", 30);
        coach.openToday();
    }

    public void prepareProgram(DailyPlanPageObject daily, boolean custom) {
        this.daily = daily;
        if (Boolean.parseBoolean(selectors.getProperty("program.reloadBeforeSnapshot", "false"))) {
            Assert.assertTrue("Observed reload is iOS-only", Platform.getInstance().isIOS());
            String current = daily.getActiveProgramName();
            iOSProgramSelectorPageObject selector = new iOSProgramSelectorPageObject(driver);
            selector.openProgramSelector();
            selector.selectExactProgram(current);
            finishProgramSwitch(current);
            selector.waitForSelectedProgram(current);
        }
        originalProgram = programState();
        selectProgram(required(custom ? "custom.program" : "regular.program"));
        kegelProgram = programState();
    }

    private String[] programState() {
        return new String[]{daily.getActiveProgramName(), daily.getCurrentDayLabel(), daily.getProgramProgressValue()};
    }

    private void selectProgram(String title) {
        if (title.equalsIgnoreCase(daily.getActiveProgramName())) return;
        String parent = selectors.getProperty("custom.parentProgram");
        if (title.equals(selectors.getProperty("custom.program")) && parent != null
                && !parent.equalsIgnoreCase(daily.getActiveProgramName())) {
            Assert.assertNotEquals("Custom parent must differ from its program", title, parent);
            selectProgram(parent);
        }
        if (Platform.getInstance().isIOS()) {
            iOSProgramSelectorPageObject selector = new iOSProgramSelectorPageObject(driver);
            selector.openProgramSelector();
            selector.selectExactProgram(title);
            finishProgramSwitch(title);
            selector.waitForSelectedProgram(title);
        } else {
            AndroidProgramSelectorPageObject selector = new AndroidProgramSelectorPageObject(driver);
            selector.openProgramSelector();
            selector.selectExactProgram(title);
        }
    }

    private void finishProgramSwitch(String title) {
        if (!selectors.containsKey("program.selected")) return;
        requireKeys("program.confirm", "program.hint", "program.hint.close");
        createWait(20).withMessage("Program switch did not reach its selected Today screen").until(d -> {
            if (isElementVisible(required("program.confirm"))) {
                tap("program.confirm");
                return false;
            }
            if (isElementVisible(required("program.hint"))) {
                tap("program.hint.close");
                return false;
            }
            return isElementVisible(template("program.selected", title));
        });
    }

    public void startRegularPlayer() {
        requireKeys("regular.stretching", "regular.start", "player.pause", "player.paused");
        WebElement stretching = visible("regular.stretching");
        originalStretching = stretching.getAttribute(Platform.getInstance().isIOS() ? "value" : "checked");
        Assert.assertTrue("Unknown stretching switch state", Arrays.asList("0", "1", "true", "false").contains(originalStretching));
        if ("1".equals(originalStretching) || "true".equals(originalStretching)) tap("regular.stretching");
        tap("regular.start");
        // Match DailyPlan's existing player-loading budget; the native shell can
        // appear before the downloaded exercise controls become available.
        waitForElementVisible(required("player.pause"), "Kegel player did not load", 30);
        if (selectors.containsKey("player.selectExercise")) tap("player.selectExercise");
        if (selectors.containsKey("player.expectedExercise")) {
            createWait(10).withMessage("Expected exercise has not replaced the preparation phase").until(d ->
                    required("player.expectedExercise").equals(text("workout.identity")));
        }
        tap("player.pause");
        visible("player.paused");
    }

    /** Run before session logout, including when preparation or the assertion fails. */
    public void restoreProgram() {
        if (originalProgram == null) return;
        try {
            // Content may fail to load while the observed close control remains usable.
            closeGuideIfOpen();
            for (String key : new String[]{"info.close"}) {
                if (selectors.containsKey(key) && isElementVisible(required(key))) tap(key);
            }
            closeCustomWorkoutIfOpen();
            daily.closeKegelExerciseFlowIfPresent();
            daily.openTodayTab();
            if (originalStretching != null) {
                openWorkout(false);
                String attribute = Platform.getInstance().isIOS() ? "value" : "checked";
                if (!originalStretching.equals(visible("regular.stretching").getAttribute(attribute))) tap("regular.stretching");
                Assert.assertEquals("Stretching preference was not restored", originalStretching,
                        visible("regular.stretching").getAttribute(attribute));
                daily.closeKegelExerciseFlowIfPresent();
                daily.openTodayTab();
            }
            if (kegelProgram != null) Assert.assertArrayEquals("Kegel progress changed", kegelProgram, programState());
        } finally {
            if (Platform.getInstance().isIOS()) new iOSProgramSelectorPageObject(driver).dismissSelectorIfOpen();
            else new AndroidProgramSelectorPageObject(driver).dismissSelectorIfOpen();
            daily.closeKegelExerciseFlowIfPresent();
            daily.openTodayTab();
            selectProgram(originalProgram[0]);
            Assert.assertArrayEquals("Original program/day/progress was not restored", originalProgram, programState());
        }
    }

    /** Cleanup only; the Guide test itself still requires the cross to return directly. */
    public void closeGuideIfOpen() {
        if (selectors.containsKey("guide.close") && isElementVisible(required("guide.close"))) tap("guide.close");
        if (selectors.containsKey("guide.quit") && isElementVisible(required("guide.quit"))) {
            tap("guide.quit.confirm");
            waitForFirstElementPresent(new String[]{required("player.pause"), required("player.paused")},
                    "Workout did not return after leaving Guide", 30);
        }
    }

    public void openWorkout(boolean custom) {
        String key = custom ? "custom.card" : "regular.card";
        swipeUpToFindFirstVisibleElement(new String[]{required(key)}, "Prepared workout is unavailable", 8);
        tap(key);
        visible(custom ? "custom.screen" : "regular.screen");
    }

    public void closeCustomWorkoutIfOpen() {
        if (nativeCatalog() && isElementVisible(required("custom.screen"))) {
            scrollNativeTo(required("custom.close"), false);
            tap("custom.close");
            gone("custom.screen");
        }
    }

    public String text(String key) {
        String value = visible(key).getText().trim();
        Assert.assertFalse("Empty UI content: " + key, value.isEmpty());
        return value;
    }

    public void assertStartEnabled(boolean expected) {
        if (androidCatalog()) {
            WebElement button = visible("custom.start");
            io.qameta.allure.Allure.addAttachment("Custom Start before checking active=" + expected,
                    "text/plain", "enabled=" + button.getAttribute("enabled")
                            + ", clickable=" + button.getAttribute("clickable"), "txt");
            io.qameta.allure.Allure.addAttachment("Custom workout before checking Start active=" + expected,
                    "application/xml", lib.TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        }
        createWait(10).withMessage("Custom Kegel Start active state is incorrect").until(d -> {
            WebElement button = visible("custom.start");
            boolean active = isElementEnabled(button);
            if (androidCatalog()) active &= Boolean.parseBoolean(button.getAttribute("clickable"));
            return active == expected;
        });
    }

    public void requireCustomSelectors(boolean restore) {
        if (nativeCatalog()) {
            requireKeys("custom.program", "custom.card", "custom.screen", "custom.collection",
                    "custom.rows", "custom.row.title", "custom.row.metadata", "custom.totalDuration", "custom.close",
                    "custom.confirm", "custom.cancel", "custom.delete", "custom.start", "custom.add",
                    "custom.editor", "custom.editor.close", "custom.remove", "custom.save",
                    "custom.picker", "custom.picker.close", "custom.pick", "custom.picked", "custom.picker.save",
                    "custom.edit.intensity", "custom.wheel", "custom.wheel.save", "custom.fields");
            if (androidCatalog()) requireKeys("custom.list", "custom.selected", "custom.wheel.value", "custom.editor.intensity");
            return;
        }
        requireKeys("custom.card", "custom.screen", "custom.rows", "custom.row.title",
                "custom.row.remove", "custom.confirm", "custom.cancel", "custom.delete",
                "custom.start", "custom.count");
        if (!restore) return;
        requireKeys("custom.add", "custom.picker", "custom.picker.close", "custom.pick",
                "custom.editor", "custom.editor.close", "custom.save", "custom.fields");
        Assert.assertTrue("custom.pick must be an XPath template with {value}",
                required("custom.pick").startsWith("xpath:") && required("custom.pick").contains("{value}"));
        for (String field : fields()) {
            requireKeys("custom.row." + field, "custom.edit." + field,
                    "custom.option." + field);
            Assert.assertTrue("Field option must be an XPath template with {value}",
                    required("custom.option." + field).startsWith("xpath:")
                            && required("custom.option." + field).contains("{value}"));
        }
    }

    private List<String> fields() {
        return Arrays.asList(required("custom.fields").split("\\s*,\\s*"));
    }

    private List<WebElement> rows() {
        return driver.findElements(getLocatorByString(required("custom.rows")));
    }

    public List<Exercise> snapshot() {
        if (nativeCatalog()) return nativeSnapshot();
        // The dedicated small fixture must expose its entire list, including duplicates.
        // A UI total prevents a RecyclerView viewport from masquerading as the whole workout.
        int total = Integer.parseInt(text("custom.count"));
        List<Exercise> result = new ArrayList<>();
        for (WebElement row : rows()) {
            String title = row.findElement(getLocatorByString(required("custom.row.title"))).getText().trim();
            Assert.assertFalse("Empty exercise title", title.isEmpty());
            Map<String, String> values = new LinkedHashMap<>();
            if (selectors.containsKey("custom.fields")) {
                for (String field : fields()) {
                    String value = row.findElement(getLocatorByString(required("custom.row." + field))).getText().trim();
                    Assert.assertFalse("Empty exercise setting: " + field, value.isEmpty());
                    values.put(field, value);
                }
            }
            result.add(new Exercise(title, values));
        }
        Assert.assertEquals("Entire Custom Kegel fixture must fit in the rendered list", total, result.size());
        return result;
    }

    public void requestFirstRemoval() {
        if (nativeCatalog()) {
            Assert.assertFalse("Custom Kegel fixture is empty", snapshot().isEmpty());
            rows().get(0).findElement(getLocatorByString(required("custom.row.title"))).click();
            visible("custom.editor");
            scrollNativeTo(required("custom.remove"), true);
            tap("custom.remove");
            visible("custom.confirm");
            return;
        }
        Assert.assertFalse("Custom Kegel fixture is empty", rows().isEmpty());
        createWait(10).until(d -> {
            WebElement remove = rows().get(0).findElement(getLocatorByString(required("custom.row.remove")));
            if (!remove.isDisplayed() || !isElementEnabled(remove)) return false;
            remove.click();
            return true;
        });
        visible("custom.confirm");
    }

    private void removeFirst() {
        int before = snapshot().size();
        requestFirstRemoval();
        tap("custom.delete");
        gone("custom.confirm");
        if (nativeCatalog()) {
            waitForNativeCount(before - 1);
            return;
        }
        createWait(10).withMessage("Deletion did not remove exactly one exercise").until(d ->
                rows().size() == before - 1 && Integer.toString(before - 1).equals(text("custom.count")));
    }

    public void removeAll() {
        int total = snapshot().size();
        for (int i = 0; i < total; i++) removeFirst();
        Assert.assertTrue("Custom Kegel list did not become empty", snapshot().isEmpty());
    }

    public void add(Exercise exercise) {
        if (nativeCatalog()) {
            addFromNativeCatalog(exercise);
            return;
        }
        int before = snapshot().size();
        tap("custom.add");
        visible("custom.picker");
        waitForElementAndClick(template("custom.pick", exercise.title), "Original exercise unavailable in picker", 10);
        visible("custom.editor");
        for (Map.Entry<String, String> setting : exercise.values.entrySet()) {
            tap("custom.edit." + setting.getKey());
            waitForElementAndClick(template("custom.option." + setting.getKey(), setting.getValue()),
                    "Original exercise setting unavailable", 10);
            String save = selectors.getProperty("custom.optionSave." + setting.getKey());
            if (save != null && !save.trim().isEmpty()) {
                waitForElementAndClick(save, "Cannot save exercise setting", 10);
            }
        }
        tap("custom.save");
        gone("custom.editor");
        visible("custom.screen");
        createWait(10).withMessage("Adding an exercise did not increase the count").until(d ->
                rows().size() == before + 1 && Integer.toString(before + 1).equals(text("custom.count")));
        Assert.assertEquals("Added exercise settings differ", exercise, snapshot().get(before));
    }

    public void restore(List<Exercise> baseline) {
        // Normalize only known overlays, including failures between opening and saving the picker.
        closeIfVisible("custom.confirm", "custom.cancel");
        if (nativeCatalog() && isElementVisible(required("custom.wheel"))) tap("custom.wheel.save");
        for (String field : fields()) {
            String cancel = selectors.getProperty("custom.optionCancel." + field);
            if (cancel != null && !cancel.trim().isEmpty() && isElementVisible(cancel)) {
                waitForElementAndClick(cancel, "Cannot close setting picker during restoration", 10);
            }
        }
        closeIfVisible("custom.editor", "custom.editor.close");
        closeIfVisible("custom.picker", "custom.picker.close");
        visible("custom.screen");
        if (baseline.equals(snapshot())) return;
        removeAll();
        for (Exercise exercise : baseline) add(exercise);
        Assert.assertEquals("Original exercise order/settings were not restored", baseline, snapshot());
        assertStartEnabled(!baseline.isEmpty());
    }

    private void closeIfVisible(String screen, String close) {
        if (isElementVisible(required(screen))) {
            tap(close);
            gone(screen);
        }
    }

    public void cancelRemoval() {
        tap("custom.cancel");
        gone("custom.confirm");
        if (nativeCatalog()) {
            tap("custom.editor.close");
            gone("custom.editor");
        }
    }

    private boolean nativeCatalog() {
        return "ios-catalog".equals(selectors.getProperty("custom.flow")) || androidCatalog();
    }

    private boolean androidCatalog() {
        return "android-catalog".equals(selectors.getProperty("custom.flow"));
    }

    private void scrollNativeTo(String locator, boolean down) {
        for (int n = 0; n < 10 && !nativeControlInViewport(locator); n++) {
            if (androidCatalog()) {
                Map<String,Object> args = new HashMap<>();
                args.put("elementId", ((org.openqa.selenium.remote.RemoteWebElement)visible("custom.collection")).getId());
                args.put("direction", down ? "down" : "up");
                args.put("percent", .35);
                driver.executeScript("mobile: scrollGesture", args);
                continue;
            }
            Rectangle r = visible("custom.collection").getRect();
            int top = Math.max(r.y, 70);
            int bottom = nativeViewportBottom(r);
            Map<String, Object> args = new HashMap<>();
            args.put("duration", .5);
            args.put("fromX", r.x + r.width / 2);
            args.put("toX", r.x + r.width / 2);
            args.put("fromY", top + (int)((bottom - top) * (down ? .95 : .25)));
            args.put("toY", top + (int)((bottom - top) * (down ? .25 : .95)));
            driver.executeScript("mobile: dragFromToForDuration", args);
        }
        createWait(10).withMessage("Custom Kegel control remains outside the unobstructed viewport")
                .until(d -> nativeControlInViewport(locator));
    }

    private boolean nativeControlInViewport(String locator) {
        Rectangle collection = visible("custom.collection").getRect();
        int top = Math.max(collection.y, 70);
        int bottom = nativeViewportBottom(collection);
        for (WebElement element : driver.findElements(getLocatorByString(locator))) {
            if (!element.isDisplayed()) continue;
            Rectangle r = element.getRect();
            // XCUITest may report visible=true even below the fixed Save/Start panel.
            if (r.y >= top && r.y + r.height <= bottom) return true;
        }
        return false;
    }

    private int nativeViewportBottom(Rectangle collection) {
        int bottom = collection.y + collection.height;
        for (String key : new String[]{"custom.start", "custom.save", "custom.footer"}) {
            if (!selectors.containsKey(key)) continue;
            for (WebElement footer : driver.findElements(getLocatorByString(required(key)))) {
                if (footer.isDisplayed()) bottom = Math.min(bottom, footer.getRect().y);
            }
        }
        return bottom;
    }

    private List<Exercise> nativeSnapshot() {
        scrollNativeTo(required("custom.totalDuration"), false);
        int totalSeconds = durationSeconds(text("custom.totalDuration"));
        scrollNativeTo(required("custom.add"), true);
        if (androidCatalog()) {
            // Native bounds are clipped to the screen. A shorter list can leave
            // its first row cut off after scrolling Add into view.
            for (int n = 0; n < 10; n++) {
                List<WebElement> candidates = driver.findElements(getLocatorByString(required("custom.list")));
                if (candidates.isEmpty() || rows().isEmpty()) break;
                Rectangle listBounds = candidates.get(0).getRect();
                Rectangle viewportBounds = visible("custom.collection").getRect();
                String direction;
                if (listBounds.y <= viewportBounds.y) direction = "up";
                else if (listBounds.y + listBounds.height >= nativeViewportBottom(viewportBounds)) direction = "down";
                else break;
                Map<String,Object> args = new HashMap<>();
                args.put("elementId", ((org.openqa.selenium.remote.RemoteWebElement)visible("custom.collection")).getId());
                args.put("direction", direction);
                args.put("percent", .1);
                driver.executeScript("mobile: scrollGesture", args);
            }
            // Android's duration includes rest periods. Instead prove the complete
            // non-virtualized RecyclerView fits above Add and the fixed Start footer.
            List<WebElement> lists = driver.findElements(getLocatorByString(required("custom.list")));
            if (lists.isEmpty() || !lists.get(0).isDisplayed() || lists.get(0).getRect().height == 0) {
                Assert.assertTrue("Hidden list still exposes exercises", rows().isEmpty());
                Assert.assertEquals("Empty list has a nonzero workout duration", 0, totalSeconds);
                return new ArrayList<>();
            }
            WebElement listElement = lists.get(0);
            Assert.assertEquals("Android fixture requires an expanded, non-scrollable exercise list",
                    "false", listElement.getAttribute("scrollable"));
            Rectangle list = listElement.getRect();
            Rectangle viewport = visible("custom.collection").getRect();
            int bottom = nativeViewportBottom(viewport);
            Assert.assertTrue("Entire Android Custom list must fit before any mutation",
                    list.y > viewport.y && list.y + list.height < bottom
                            && list.y + list.height <= visible("custom.add").getRect().y);
            List<WebElement> renderedRows = rows();
            int nextRowY = list.y;
            for (WebElement row : renderedRows) {
                Rectangle bounds = row.getRect();
                Assert.assertEquals("Android exercise rows are clipped or discontinuous", nextRowY, bounds.y);
                nextRowY = bounds.y + bounds.height;
            }
            if (!renderedRows.isEmpty()) {
                Rectangle first = renderedRows.get(0).getRect();
                Rectangle last = renderedRows.get(renderedRows.size() - 1).getRect();
                Assert.assertEquals("First exercise is clipped", list.y, first.y);
                Assert.assertEquals("Last exercise is clipped", list.y + list.height, last.y + last.height);
            }
        }
        List<Exercise> result = new ArrayList<>();
        int observedSeconds = 0;
        for (WebElement row : rows()) {
            String title = row.findElement(getLocatorByString(required("custom.row.title"))).getText().trim();
            String metadata = row.findElement(getLocatorByString(required("custom.row.metadata"))).getText().trim();
            Matcher match = Pattern.compile("^(.+?)\\s+(\\d+)\\s+SEC$").matcher(metadata);
            Assert.assertTrue("Unknown exercise metadata: " + metadata, match.matches());
            int duration = Integer.parseInt(match.group(2));
            Assert.assertTrue("Exercise duration must be positive", duration > 0);
            Map<String, String> values = new LinkedHashMap<>();
            values.put("intensity", match.group(1).trim());
            values.put("duration", Integer.toString(duration));
            result.add(new Exercise(title, values));
            observedSeconds += duration;
        }
        if (!androidCatalog()) Assert.assertEquals("Rendered list is incomplete or total duration differs from its exercises",
                totalSeconds, observedSeconds);
        return result;
    }

    private static int durationSeconds(String text) {
        Matcher units = Pattern.compile("(\\d+)\\s*(min|sec)", Pattern.CASE_INSENSITIVE).matcher(text);
        int seconds = 0;
        boolean found = false;
        while (units.find()) {
            found = true;
            seconds += Integer.parseInt(units.group(1)) * ("min".equalsIgnoreCase(units.group(2)) ? 60 : 1);
        }
        Assert.assertTrue("Unknown workout duration format: " + text, found);
        return seconds;
    }

    private void waitForNativeCount(int expected) {
        createWait(20).withMessage("Custom Kegel count/duration did not settle after mutation").until(d -> {
            try { return nativeSnapshot().size() == expected; }
            catch (AssertionError | org.openqa.selenium.StaleElementReferenceException updating) { return false; }
        });
    }

    private void addFromNativeCatalog(Exercise exercise) {
        int before = snapshot().size();
        tap("custom.add");
        visible("custom.picker");
        String pick = template("custom.pick", exercise.title);
        scrollNativeTo(pick, true);
        waitForElementAndClick(pick, "Original exercise is absent from catalog", 10);
        waitForElementVisible(template("custom.picked", exercise.title), "Exercise was not selected", 10);
        if (androidCatalog()) {
            final int[] stable = {0};
            createWait(10).withMessage("Catalog must settle with exactly the requested exercise selected").until(d -> {
                boolean single = driver.findElements(getLocatorByString(required("custom.selected"))).size() == 1
                        && isElementVisible(template("custom.picked", exercise.title));
                stable[0] = single ? stable[0] + 1 : 0;
                return stable[0] >= 3;
            });
            io.qameta.allure.Allure.addAttachment("Catalog before adding " + exercise.title,
                    "application/xml", lib.TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        }
        tap("custom.picker.save");
        gone("custom.picker");
        waitForNativeCount(before + 1);
        // Native Save is disabled when catalog defaults already match the baseline.
        if (exercise.equals(snapshot().get(before))) return;
        rows().get(before).findElement(getLocatorByString(required("custom.row.title"))).click();
        visible("custom.editor");
        scrollNativeTo(required("custom.edit.intensity"), true);
        tap("custom.edit.intensity");
        String originalIntensity = exercise.values.get("intensity");
        if (androidCatalog()) {
            selectAndroidIntensity(originalIntensity);
            tap("custom.wheel.save");
            createWait(10).withMessage("Selected intensity did not reach the exercise editor").until(d ->
                    originalIntensity.equalsIgnoreCase(text("custom.editor.intensity")));
            tap("custom.save");
            gone("custom.editor");
            Assert.assertEquals("Added exercise/order/settings differ", exercise, snapshot().get(before));
            return;
        }
        StringBuilder wheelValue = new StringBuilder();
        for (String word : originalIntensity.toLowerCase(java.util.Locale.ROOT).split(" ")) {
            if (wheelValue.length() > 0) wheelValue.append(' ');
            wheelValue.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        visible("custom.wheel").sendKeys(wheelValue.toString());
        Assert.assertEquals("Original intensity was not selected", wheelValue.toString(), visible("custom.wheel").getAttribute("value"));
        tap("custom.wheel.save");
        tap("custom.save");
        gone("custom.editor");
        Assert.assertEquals("Added exercise/order/settings differ", exercise, snapshot().get(before));
    }

    private void selectAndroidIntensity(String intensity) {
        for (int n = 0; n < 8; n++) {
            if (intensity.equalsIgnoreCase(text("custom.wheel.value"))) return;
            for (WebElement value : visible("custom.wheel").findElements(org.openqa.selenium.By.className("android.widget.Button"))) {
                if (intensity.equalsIgnoreCase(value.getText())) {
                    value.click();
                    final int[] stable = {0};
                    createWait(10).until(d -> {
                        stable[0] = intensity.equalsIgnoreCase(text("custom.wheel.value")) ? stable[0] + 1 : 0;
                        return stable[0] >= 3;
                    });
                    return;
                }
            }
            Map<String,Object> args = new HashMap<>();
            args.put("elementId", ((org.openqa.selenium.remote.RemoteWebElement)visible("custom.wheel")).getId());
            args.put("direction", "down");args.put("percent", .35);
            driver.executeScript("mobile: scrollGesture", args);
        }
        Assert.fail("Original Android intensity is unavailable");
    }

    private String template(String key, String value) {
        return required(key).replace("{value}", xpathLiteral(value));
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }

    public static final class Exercise {
        private final String title;
        private final Map<String, String> values;

        private Exercise(String title, Map<String, String> values) {
            this.title = title;
            this.values = new LinkedHashMap<>(values);
        }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Exercise)) return false;
            Exercise exercise = (Exercise) other;
            return title.equals(exercise.title) && values.equals(exercise.values);
        }

        @Override public int hashCode() { return 31 * title.hashCode() + values.hashCode(); }

        @Override public String toString() { return title + " " + values; }
    }
}
