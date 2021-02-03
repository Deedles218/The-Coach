package lib.ui;

import org.openqa.selenium.remote.RemoteWebDriver;

abstract public class SearchPageObject extends MainPageObject {
    protected static String
            FEED_SEARCH_INIT_ELEMENT,
            FEED_SEARCH_INIT_BUTTON,
    SEND_BUTTON,
    TAB_FEED,
    FEED_TAB,
    INPUT_FIELD,
    IMAGE,
    GIF,
    INPUT_FIELD_ACTIVE,
            FEED_SEARCH_INPUT,
    SEARCH_RESULT_BY_SUBSTRING_TPL,
    SEARCH_RESULT_BY_TWO_SUBSTRINGS_TPL,
    SEARCH_CANCEL_BUTTON,
    SEARCH_RESULT_ELEMENT,
    SEARCH_EMPTY_RESULT_ELEMENT,
    ARTICLE_TITLE;

    public SearchPageObject(RemoteWebDriver driver) {
        super(driver);
    }
    /* TEMPLATES METHODS */
    private static String getResultSearchElement(String substring) {
        return SEARCH_RESULT_BY_SUBSTRING_TPL.replace("{SUBSTRING}", substring);
    }

    private static String getResultSearchElementByTwoOptions(String firstSubstring, String secondSubstring) {
        String stringAfterFirstReplace = SEARCH_RESULT_BY_TWO_SUBSTRINGS_TPL.replace("{firstSUBSTRING}", firstSubstring);

        return stringAfterFirstReplace.replace("{secondSUBSTRING}", secondSubstring);
    }
    /* TEMPLATES METHODS */

    public void initSearchInput() {
        this.waitForElementAndClick(FEED_SEARCH_INIT_ELEMENT, "Cannot find and click search init element", 5);
        this.waitForElementPresent(FEED_SEARCH_INPUT, "Cannot find search input after clicking search init element");
    }

    public void waitForCancelButtonToAppear() {
        this.waitForElementPresent(SEARCH_CANCEL_BUTTON, "Cannot find search cancel button", 5);
    }

    public void waitForCancelButtonToDisappear() {
        this.waitForElementNotPresent(SEARCH_CANCEL_BUTTON, "Search cancel button is still present", 5);
    }

    public void clickCancelSearch() {
        this.waitForElementAndClick(SEARCH_CANCEL_BUTTON, "Cannot find and click search cancel button", 5);
    }

    public void typeSearchLine(String search_line) {
        this.waitForElementAndSendKeys(FEED_SEARCH_INPUT, search_line, "Cannot find and type into  search input", 5);
    }
 public void typeSearchButton ()
 {
     this.waitForElementAndClick(FEED_SEARCH_INIT_BUTTON, "Cannot find and type into  search input",10);

 }
    public void waitForSearchResult(String substring)
    {
        String search_result_xpath = getResultSearchElement(substring);
        this.waitForElementPresent(search_result_xpath, "Cannot find search result with substring " + substring);
    }

    public void clickByArticleWithSubstring(String substring) {
        String search_result_xpath = getResultSearchElement(substring);
        this.waitForElementAndClick(search_result_xpath, "Cannot find and click search result with substring " + substring, 10);
    }

    public int getAmountOfFoundArticles() {
        this.waitForElementPresent(
                SEARCH_RESULT_ELEMENT,
                "Cannot find anything by request ",
                15
        );
        return this.getAmountElements(SEARCH_RESULT_ELEMENT);
    }

    public void waitForEmptyResultsLabel() {
        this.waitForElementPresent(SEARCH_EMPTY_RESULT_ELEMENT, "Cannot find empty result element", 15);
    }

    public void assertThereIsNoResultOfSearch() {
        this.assertElementNotPresent(SEARCH_RESULT_ELEMENT, "We supposed not to find any results");
    }

    public void waitForArticleTitle() {
        this.waitForElementPresent(ARTICLE_TITLE, "Cannot find article title", 15);
    }
    public String waitForElementByTitleAndDescription(String title, String description)
    {
        String search_result_xpath = getResultSearchElementByTwoOptions(title, description);
        this.waitForElementPresent(search_result_xpath, "Cannot find search result with title = '" + title + "' and description = '" + description + "'");
        return search_result_xpath;
    }
    public void clickOnInputField(){
        this.waitForElementAndClick(INPUT_FIELD,"Cannot found and tap on input field", 10);
    }
    public void typeMessage(String debug_message){
        this.waitForElementAndSendKeys(INPUT_FIELD_ACTIVE, debug_message, "Cannot find and type into  search input", 5);
    }
    public void sendMessage(){
        this.waitForElementAndClick(SEND_BUTTON,"Button unactive",15);
    }

    public void tapOnImage(){
        this.waitForElementAndClick(IMAGE,  "Cannot found Image in bot answer", 5);
    }
    public void tapOnGif(){
        this.waitForElementAndClick(GIF,  "Cannot found Image in bot answer", 5);
    }

}
