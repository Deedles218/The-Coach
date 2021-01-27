package tests;

import lib.CoreTestCase;
import lib.ui.SearchPageObject;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;
public class SearchTests extends CoreTestCase
{
    @Test
    public void testSearch()
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        String TAB_FEED = "id:Feed";
        SearchPageObject.waitForElementAndClick(TAB_FEED,"error",10);
        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine("Coronavirus");
        SearchPageObject.typeSearchButton();
        SearchPageObject.waitForSearchResult("Coronavirus");
    }
    @Test
    public void testCancelSearch() {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        String TAB_FEED = "id:Feed";
        SearchPageObject.waitForElementAndClick(TAB_FEED,"error",10);
        SearchPageObject.initSearchInput();
        //SearchPageObject.typeSearchLine("Java");
        SearchPageObject.waitForCancelButtonToAppear();
        SearchPageObject.clickCancelSearch();
        String FEED="id:FEED";
        SearchPageObject.waitForElementPresent(FEED,"нет таббара фид",5);
    }
    @Test
    public void testAmountOfNotEmptySearch()
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        String search_line = "Linking park discography";
        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine(search_line);
        int amount_of_search_results = SearchPageObject.getAmountOfFoundArticles();
        assertTrue(
                "We found too few results",
                amount_of_search_results >0
        );

    }
    @Test
    public void testAmountOfEmptySearch()
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.initSearchInput();
        String search_line = "tfgvb";
        SearchPageObject.typeSearchLine(search_line);
        SearchPageObject.waitForEmptyResultsLabel();
        SearchPageObject.assertThereIsNoResultOfSearch();
    }
}
