package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

abstract public class ExplorePageObject extends MainPageObject {
    protected static final String
            TEST_ID_EXPLORE_TAB = "id:tab_explore",
            TEST_ID_EXPLORE_SELECTED = "id:tab_explore_selected",
            TEST_ID_LOADING = "id:loading_indicator";

    protected static String
            TAB_EXPLORE,
            SELECTED_EXPLORE_TAB,
            EXPLORE_ENTRY_POINT,
            TAB_PROGRAMS,
            TAB_TOOLS,
            RECOMMENDED_SECTION,
            RECOMMENDED_COLLECTION,
            RECOMMENDED_PROGRAM_CARDS,
            SEXUAL_HEALTH_SECTION,
            COURSES_SECTION,
            COURSES_COLLECTION,
            COURSE_CARDS,
            BODY_PRACTICES_SECTION,
            BODY_PRACTICES_COLLECTION,
            BODY_PRACTICE_CARDS,
            FIRST_BODY_PRACTICE_CARD,
            MIND_PRACTICES_SECTION,
            MIND_PRACTICES_COLLECTION,
            MIND_PRACTICE_CARDS,
            FIRST_MIND_PRACTICE_CARD,
            RETIRED_MAIN_PROGRAMS_SECTION,
            RETIRED_MORE_TOOLS_SECTION,
            COMPLETED_PROGRAMS_SECTION,
            COMING_SOON_SECTION,
            FIRST_PROGRAM_CARD,
            FIRST_PROGRAM_CARD_IMAGE,
            FIRST_PROGRAM_TITLE,
            LAST_RECOMMENDED_PROGRAM_CARD,
            LAST_RECOMMENDED_PROGRAM_TITLE,
            PROGRAM_DETAIL_TITLE,
            PROGRAM_DETAIL_CONTENT,
            PROGRAM_DETAIL_CLOSE_BUTTON,
            FIRST_COURSE_CARD,
            FIRST_COURSE_CARD_IMAGE,
            FIRST_COURSE_TITLE,
            LAST_COURSE_CARD,
            LAST_COURSE_TITLE,
            COURSE_DETAIL_TITLE,
            COURSE_DETAIL_TYPE,
            COURSE_DETAIL_CONTENT,
            COURSE_DETAIL_BACK_BUTTON,
            CUSTOM_KEGEL_CARD,
            CUSTOM_KEGEL_TITLE,
            CUSTOM_KEGEL_START_BUTTON,
            CUSTOM_KEGEL_CLOSE_BUTTON,
            LAST_BODY_PRACTICE_CARD,
            LAST_BODY_PRACTICE_TITLE,
            LAST_BODY_PRACTICE_DETAIL_TITLE,
            LAST_MIND_PRACTICE_CARD,
            LAST_MIND_PRACTICE_TITLE,
            LAST_MIND_PRACTICE_DETAIL_TITLE,
            PRACTICE_DETAIL_HEADER,
            PRACTICE_DETAIL_GOAL,
            PRACTICE_DETAIL_START_BUTTON,
            CONCEPT_POPUP_CONFIRM_BUTTON;

    public ExplorePageObject(RemoteWebDriver driver) {
        super(driver);
    }

    public boolean isExploreContextAvailable() {
        return this.isElementPresent(EXPLORE_ENTRY_POINT);
    }

    @Step("Open Explore tab")
    public void openExploreTab() {
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_EXPLORE_TAB, EXPLORE_ENTRY_POINT},
                "Neither Explore navigation nor a closable Explore detail screen became available",
                20
        );
        closeExploreDetailIfPresent();
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_EXPLORE_TAB, TAB_EXPLORE},
                "Cannot tap Explore tab",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_EXPLORE_SELECTED, SELECTED_EXPLORE_TAB},
                "Explore tab is not selected after a single tap",
                10
        );
        this.waitForLoadingToDisappearIfPresent(
                TEST_ID_LOADING,
                "Explore loading indicator is still displayed",
                20
        );
        this.waitForElementPresent(RECOMMENDED_SECTION, "Explore content did not load", 15);
    }

    @Step("Verify current Explore sections")
    public void assertCurrentSectionsAreDisplayed() {
        this.waitForElementPresent(RECOMMENDED_SECTION, "Recommended for you section is not displayed", 10);
        this.waitForElementPresent(SEXUAL_HEALTH_SECTION, "Sexual Health section is not displayed", 10);
        this.waitForElementPresent(COURSES_SECTION, "Courses section is not displayed", 10);
        this.waitForElementPresent(BODY_PRACTICES_SECTION, "Body Practices section is not displayed", 10);
        this.swipeUpToFindElement(MIND_PRACTICES_SECTION, "Mind Practices section is not displayed", 5);
        this.waitForElementPresent(MIND_PRACTICES_SECTION, "Mind Practices section is not displayed", 10);
    }

    @Step("Verify the current Main programs area contains Recommended for you programs")
    public void assertMainProgramsRecommendedSection() {
        this.waitForElementPresent(RECOMMENDED_SECTION, "Recommended for you heading is not displayed", 10);
        assertSectionContainsCards(
                RECOMMENDED_PROGRAM_CARDS,
                "Recommended for you must contain at least one main program"
        );
    }

    @Step("Verify Courses section contains course cards")
    public void assertCoursesSection() {
        this.swipeUpToFindElement(COURSES_SECTION, "Courses section is not displayed", 4);
        this.waitForElementPresent(COURSES_SECTION, "Courses heading is not displayed", 10);
        assertSectionContainsCards(COURSE_CARDS, "Courses must contain at least one course card");
        WebElement courseImage = this.waitForElementPresent(
                FIRST_COURSE_CARD_IMAGE,
                "First course card has no image",
                10
        );
        Assert.assertTrue(
                "First course card image must have a visible area",
                courseImage.getSize().getWidth() > 0 && courseImage.getSize().getHeight() > 0
        );
    }

    @Step("Verify Body Practices section contains practice cards")
    public void assertBodyPracticesSection() {
        this.swipeUpToFindElement(BODY_PRACTICES_SECTION, "Body Practices section is not displayed", 5);
        this.waitForElementPresent(BODY_PRACTICES_SECTION, "Body Practices heading is not displayed", 10);
        assertSectionContainsCards(BODY_PRACTICE_CARDS, "Body Practices must contain at least one practice card");
        this.waitForElementPresent(CUSTOM_KEGEL_CARD, "Custom Kegel is not available in Body Practices", 10);
    }

    @Step("Verify Mind Practices section contains practice cards")
    public void assertMindPracticesSection() {
        this.swipeUpToFindElement(MIND_PRACTICES_SECTION, "Mind Practices section is not displayed", 6);
        this.waitForElementPresent(MIND_PRACTICES_SECTION, "Mind Practices heading is not displayed", 10);
        assertSectionContainsCards(MIND_PRACTICE_CARDS, "Mind Practices must contain at least one practice card");
    }

    @Step("Swipe Recommended for you and open its last program")
    public void swipeToAndOpenLastRecommendedProgram() {
        swipeLeftUntilLastCardIsVisible(
                RECOMMENDED_COLLECTION,
                FIRST_PROGRAM_CARD,
                LAST_RECOMMENDED_PROGRAM_CARD,
                "last Recommended for you program"
        );
        this.waitForElementAndClick(
                LAST_RECOMMENDED_PROGRAM_TITLE,
                "Cannot open the last Recommended for you program",
                10
        );
        this.waitForElementPresent(PROGRAM_DETAIL_TITLE, "Last program detail screen did not open", 15);
        this.waitForElementPresent(PROGRAM_DETAIL_CONTENT, "Last program detail content is not displayed", 10);
    }

    @Step("Swipe Courses and open its last course")
    public void swipeToAndOpenLastCourse() {
        swipeLeftUntilLastCardIsVisible(
                COURSES_COLLECTION,
                FIRST_COURSE_CARD,
                LAST_COURSE_CARD,
                "last course"
        );
        this.waitForElementAndClick(LAST_COURSE_TITLE, "Cannot open the last course", 10);
        this.waitForElementPresent(COURSE_DETAIL_TITLE, "Last course detail screen did not open", 15);
        this.waitForElementPresent(COURSE_DETAIL_TYPE, "Last course type is not displayed", 10);
        this.waitForElementPresent(COURSE_DETAIL_CONTENT, "Last course content is not displayed", 10);
    }

    @Step("Swipe Body Practices and open its last card")
    public void swipeToAndOpenLastBodyPractice() {
        swipeLeftUntilLastCardIsVisible(
                BODY_PRACTICES_COLLECTION,
                FIRST_BODY_PRACTICE_CARD,
                LAST_BODY_PRACTICE_CARD,
                "last Body Practices card"
        );
        this.waitForElementAndClick(
                LAST_BODY_PRACTICE_TITLE,
                "Cannot open the last Body Practices card",
                10
        );
        assertPracticeStartScreen(LAST_BODY_PRACTICE_DETAIL_TITLE);
    }

    @Step("Swipe Mind Practices and open its last card")
    public void swipeToAndOpenLastMindPractice() {
        this.swipeUpToFindElement(MIND_PRACTICES_SECTION, "Mind Practices section is not displayed", 6);
        swipeLeftUntilLastCardIsVisible(
                MIND_PRACTICES_COLLECTION,
                FIRST_MIND_PRACTICE_CARD,
                LAST_MIND_PRACTICE_CARD,
                "last Mind Practices card"
        );
        this.waitForElementAndClick(
                LAST_MIND_PRACTICE_TITLE,
                "Cannot open the last Mind Practices card",
                10
        );
        assertPracticeStartScreen(LAST_MIND_PRACTICE_DETAIL_TITLE);
    }

    @Step("Close practice start screen")
    public void closePracticeDetails() {
        this.waitForElementAndClick(CUSTOM_KEGEL_CLOSE_BUTTON, "Cannot close practice start screen", 10);
        this.waitForElementPresent(RECOMMENDED_SECTION, "Explore did not return after closing practice", 10);
    }

    @Step("Verify retired Explore navigation and sections are absent")
    public void assertRetiredExploreUiIsAbsent() {
        this.assertElementNotPresent(TAB_PROGRAMS, "Programs tab must remain renamed to Explore");
        this.assertElementNotPresent(TAB_TOOLS, "Retired Tools tab must not be displayed");
        this.assertElementNotPresent(RETIRED_MAIN_PROGRAMS_SECTION, "Retired Main programs grouping must not be displayed");
        this.assertElementNotPresent(RETIRED_MORE_TOOLS_SECTION, "Retired More tools grouping must not be displayed");
        this.assertElementNotPresent(COMPLETED_PROGRAMS_SECTION, "Completed programs section must not be displayed");
        this.assertElementNotPresent(COMING_SOON_SECTION, "Coming Soon section must not be displayed");
    }

    @Step("Verify a recommended program opens its current detail screen")
    public void openFirstRecommendedProgramAndVerifyDetails() {
        WebElement card = this.waitForElementPresent(FIRST_PROGRAM_CARD, "First recommended program card is not displayed", 10);
        WebElement image = this.waitForElementPresent(FIRST_PROGRAM_CARD_IMAGE, "Recommended program card has no background image", 10);
        Assert.assertTrue("Recommended program card must have a visible image area", image.getSize().getWidth() > 0 && image.getSize().getHeight() > 0);

        this.waitForElementAndClick(FIRST_PROGRAM_TITLE, "Cannot open first recommended program", 10);
        this.waitForElementPresent(PROGRAM_DETAIL_TITLE, "Program detail screen did not open", 15);
        this.waitForElementPresent(PROGRAM_DETAIL_CONTENT, "Program detail content is not displayed", 10);
        Assert.assertTrue("Recommended program card must be tappable over a meaningful area", card.getSize().getWidth() > 100 && card.getSize().getHeight() > 100);
        Assert.assertFalse("Ordinary program must not show the concept-program popup", this.isElementPresent(CONCEPT_POPUP_CONFIRM_BUTTON));
    }

    @Step("Close program details")
    public void closeProgramDetails() {
        this.waitForElementAndClick(PROGRAM_DETAIL_CLOSE_BUTTON, "Cannot close program details", 10);
        this.waitForElementPresent(RECOMMENDED_SECTION, "Explore did not return after closing program details", 10);
    }

    @Step("Verify compact course cards and open the first course")
    public void assertCourseCardsAreCompactAndOpenFirstCourse() {
        WebElement programCard = this.waitForElementPresent(FIRST_PROGRAM_CARD, "Program card is not displayed", 10);
        WebElement courseCard = this.waitForElementPresent(FIRST_COURSE_CARD, "Course card is not displayed", 10);
        WebElement courseImage = this.waitForElementPresent(FIRST_COURSE_CARD_IMAGE, "Course card has no image", 10);

        Assert.assertTrue("Course card must be narrower than a program card", courseCard.getSize().getWidth() < programCard.getSize().getWidth());
        Assert.assertTrue("Course card must be shorter than a program card", courseCard.getSize().getHeight() < programCard.getSize().getHeight());
        Assert.assertTrue("Course card image must have a visible area", courseImage.getSize().getWidth() > 0 && courseImage.getSize().getHeight() > 0);

        this.waitForElementAndClick(FIRST_COURSE_TITLE, "Cannot open first course", 10);
        this.waitForElementPresent(COURSE_DETAIL_TITLE, "Course detail title is not displayed", 15);
        this.waitForElementPresent(COURSE_DETAIL_TYPE, "Course detail type is not displayed", 10);
        this.waitForElementPresent(COURSE_DETAIL_CONTENT, "Course detail content is not displayed", 10);
    }

    @Step("Close course details")
    public void closeCourseDetails() {
        this.waitForElementAndClick(COURSE_DETAIL_BACK_BUTTON, "Cannot return from course details", 10);
        this.waitForElementPresent(RECOMMENDED_SECTION, "Explore did not return after closing course details", 10);
    }

    @Step("Open Custom Kegel from Body Practices")
    public void openCustomKegelAndVerifyStartScreen() {
        this.waitForElementAndClick(CUSTOM_KEGEL_CARD, "Cannot open Custom Kegel from Explore", 10);
        this.waitForElementPresent(CUSTOM_KEGEL_TITLE, "Custom Kegel start screen did not open", 15);
        this.waitForElementPresent(CUSTOM_KEGEL_START_BUTTON, "Custom Kegel start button is not displayed", 10);
    }

    @Step("Close Custom Kegel start screen")
    public void closeCustomKegel() {
        this.waitForElementAndClick(CUSTOM_KEGEL_CLOSE_BUTTON, "Cannot close Custom Kegel", 10);
        this.waitForElementPresent(RECOMMENDED_SECTION, "Explore did not return after closing Custom Kegel", 10);
    }

    private void closeExploreDetailIfPresent() {
        try {
            if (this.isElementPresent(CUSTOM_KEGEL_CLOSE_BUTTON)) {
                this.waitForElementAndClick(CUSTOM_KEGEL_CLOSE_BUTTON, "Cannot close existing Custom Kegel screen", 5);
            } else if (this.isElementPresent(PROGRAM_DETAIL_CLOSE_BUTTON)) {
                this.waitForElementAndClick(PROGRAM_DETAIL_CLOSE_BUTTON, "Cannot close existing program detail", 5);
            } else if (this.isElementPresent(COURSE_DETAIL_BACK_BUTTON)) {
                this.waitForElementAndClick(COURSE_DETAIL_BACK_BUTTON, "Cannot close existing course detail", 5);
            }
        } catch (TimeoutException ignored) {
            // A fresh session normally has no Explore detail open.
        }
    }

    private void assertSectionContainsCards(String cardsLocator, String errorMessage) {
        this.waitForElementPresent(cardsLocator, errorMessage, 10);
        Assert.assertTrue(errorMessage, this.getAmountElements(cardsLocator) > 0);
    }

    private void assertPracticeStartScreen(String expectedTitleLocator) {
        this.waitForElementPresent(PRACTICE_DETAIL_HEADER, "Practice start screen did not open", 15);
        this.waitForElementPresent(expectedTitleLocator, "Expected practice title is not displayed", 10);
        this.waitForElementPresent(PRACTICE_DETAIL_GOAL, "Practice goal is not displayed", 10);
        this.waitForElementPresent(PRACTICE_DETAIL_START_BUTTON, "Practice start button is not displayed", 10);
    }

    private void swipeLeftUntilLastCardIsVisible(
            String collectionLocator,
            String firstCardLocator,
            String lastCardLocator,
            String cardDescription
    ) {
        this.waitForElementPresent(
                collectionLocator,
                "Cannot find collection containing " + cardDescription,
                10
        );
        positionCollectionAwayFromTabBar(collectionLocator);

        swipeHorizontallyUntilCardIsVisible(
                collectionLocator,
                firstCardLocator,
                "right",
                "first card used to reset " + cardDescription
        );

        swipeHorizontallyUntilCardIsVisible(
                collectionLocator,
                lastCardLocator,
                "left",
                cardDescription
        );
    }

    private void swipeHorizontallyUntilCardIsVisible(
            String collectionLocator,
            String cardLocator,
            String direction,
            String cardDescription
    ) {
        int swipeCount = 0;
        while (!isFullyVisibleHorizontally(cardLocator) && swipeCount < 4) {
            WebElement collection = this.waitForElementPresent(
                    collectionLocator,
                    "Collection disappeared while swiping to " + cardDescription,
                    10
            );
            swipeCollection(collection, direction);
            swipeCount++;
        }

        Assert.assertTrue(
                "Could not make " + cardDescription + " fully visible after " + swipeCount + " " + direction + " swipes",
                isFullyVisibleHorizontally(cardLocator)
        );
    }

    private void swipeCollection(WebElement collection, String direction) {
        Map<String, Object> swipeArguments = new HashMap<String, Object>();
        swipeArguments.put("elementId", ((RemoteWebElement) collection).getId());
        swipeArguments.put("direction", direction);
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", swipeArguments);
    }

    private void positionCollectionAwayFromTabBar(String collectionLocator) {
        int swipeCount = 0;
        while (swipeCount < 4) {
            WebElement collection = this.waitForElementPresent(
                    collectionLocator,
                    "Section collection is not displayed",
                    10
            );
            Dimension window = driver.manage().window().getSize();
            int top = collection.getLocation().getY();
            int bottom = top + collection.getSize().getHeight();
            if (top >= 90 && bottom <= window.getHeight() - 120) {
                return;
            }

            Map<String, Object> swipeArguments = new HashMap<String, Object>();
            swipeArguments.put("direction", top < 90 ? "down" : "up");
            ((JavascriptExecutor) driver).executeScript("mobile: swipe", swipeArguments);
            swipeCount++;
        }
    }

    private boolean isFullyVisibleHorizontally(String locator) {
        if (!this.isElementPresent(locator)) {
            return false;
        }
        WebElement card = this.waitForElementPresent(locator, "Last card is not present", 2);
        int left = card.getLocation().getX();
        int right = left + card.getSize().getWidth();
        int screenWidth = driver.manage().window().getSize().getWidth();
        return left >= 0 && right <= screenWidth;
    }
}
