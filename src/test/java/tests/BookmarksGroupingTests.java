package tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.Platform;
import lib.RestoringTestAction;
import lib.TestData;
import lib.ui.BookmarksPageObject;
import lib.ui.CoachFlowPageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Epic("Daily Plan")
@Feature("Bookmarks")
public class BookmarksGroupingTests extends CoreTestCase {
    private static final String LESSONS = "Lessons";
    private static final String PRACTICES = "Practices";
    private final List<Card> cards = new ArrayList<>();
    private String variant;
    private TestData.TestAccount account;

    @Before
    @Override
    public void setUp() throws Exception {
        Path path = Paths.get(BookmarksTests.required("groupingFixture")).toAbsolutePath();
        Properties fixture = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            fixture.load(reader);
        }
        variant = field(fixture, "variant");
        int count = Integer.parseInt(field(fixture, "card.count"));
        Assert.assertTrue("COA-9651 fixture must contain saved cards", count > 0);
        for (int i = 1; i <= count; i++) {
            String prefix = "card." + i + ".";
            String title = field(fixture, prefix + "title");
            String program = field(fixture, prefix + "program");
            String module = field(fixture, prefix + "module");
            Assert.assertTrue("Use the displayed MODULE heading for " + title, module.startsWith("MODULE "));
            String category = field(fixture, prefix + "category");
            Assert.assertTrue("Invalid category for " + title, LESSONS.equals(category) || PRACTICES.equals(category));
            Path cover = path.getParent().resolve(field(fixture, prefix + "cover"));
            BufferedImage image = ImageIO.read(cover.toFile());
            Assert.assertNotNull("Unreadable reference cover for " + title, image);
            cards.add(new Card(title, program, module, category, image));
        }
        validateFixture();
        account = TestData.existingProgressAccount();
        super.setUp();
    }

    @Test
    @Issue("COA-9651")
    public void cardsMatchApprovedGroupingFixture() throws Exception {
        Allure.addAttachment("COA-9651 grouping variant", variant);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        coach.openProfile();
        if (Platform.getInstance().isAndroid()) {
            AndroidCoachFlowPageObject android = (AndroidCoachFlowPageObject) coach;
            android.assertAuthenticatedProfileForEmail(account.getEmail());
            android.returnToMainScreenFromProfile();
        } else {
            ((iOSCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(account.getEmail());
        }
        coach.openToday();

        BookmarksPageObject bookmarks = BookmarksPageObjectFactory.get(driver);
        bookmarks.openFromToday();
        RestoringTestAction.run(() -> {
            bookmarks.assertTooltipAbsent(BookmarksTests.required("tooltipText"));
            String hiddenCategory = "lessons-only".equals(variant) ? PRACTICES
                    : "practices-only".equals(variant) ? LESSONS : null;
            bookmarks.assertExactCardTitles(cards.stream().map(card -> card.title).collect(Collectors.toList()), hiddenCategory);
            for (Card card : cards) {
                bookmarks.assertCardGroupAndCategory(card.title, card.program, card.category);
                bookmarks.assertCardGroupAndCategory(card.title, card.module, card.category);
                bookmarks.assertCardComposition(card.title);
                bookmarks.assertCardCover(card.title, card.cover);
            }
        }, () -> {
            if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
        });
    }

    private void validateFixture() {
        List<String> titles = cards.stream().map(card -> card.title).collect(Collectors.toList());
        Assert.assertEquals("COA-9651 requires unique titles", titles.size(), new LinkedHashSet<>(titles).size());
        if ("lessons-only".equals(variant) || "practices-only".equals(variant)) {
            String category = "lessons-only".equals(variant) ? LESSONS : PRACTICES;
            Assert.assertTrue("Fixture must contain only " + category, cards.stream().allMatch(card -> category.equals(card.category)));
            return;
        }
        Assert.assertEquals("Unknown COA-9651 fixture variant", "mixed", variant);
        Assert.assertEquals("The mixed fixture must contain four cards", 4, cards.size());
        Map<String, Map<String, List<String>>> groups = new LinkedHashMap<>();
        for (Card card : cards) {
            groups.computeIfAbsent(card.program, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(card.module, ignored -> new ArrayList<>()).add(card.category);
        }
        Assert.assertEquals("Mixed variant needs two programs", 2, groups.size());
        Map<String, List<String>> first = groups.values().stream().filter(modules -> modules.size() == 2).findFirst()
                .orElseThrow(() -> new AssertionError("One program must have cards in two modules"));
        Map<String, List<String>> second = groups.values().stream().filter(modules -> modules.size() == 1).findFirst()
                .orElseThrow(() -> new AssertionError("The other program must have cards in one module"));
        Assert.assertTrue("The first program needs a lesson-only module",
                first.containsValue(Arrays.asList(LESSONS)));
        Assert.assertTrue("The first program needs a mixed lesson and practice module", first.values().stream()
                .anyMatch(categories -> categories.size() == 2 && categories.containsAll(Arrays.asList(LESSONS, PRACTICES))));
        Assert.assertTrue("The second program needs one practice", second.containsValue(Arrays.asList(PRACTICES)));
    }

    private static String field(Properties fixture, String key) {
        String value = fixture.getProperty(key);
        Assert.assertTrue("Missing COA-9651 fixture field " + key, value != null && !value.trim().isEmpty());
        return value.trim();
    }

    private static class Card {
        final String title;
        final String program;
        final String module;
        final String category;
        final BufferedImage cover;

        Card(String title, String program, String module, String category, BufferedImage cover) {
            this.title = title;
            this.program = program;
            this.module = module;
            this.category = category;
            this.cover = cover;
        }
    }
}
