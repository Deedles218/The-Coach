package tests;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.ArticlePageObject;
import lib.ui.MyListsPageObject;
import lib.ui.NavigationUI;
import lib.ui.SearchPageObject;
import lib.ui.factories.ArticlePageObjectFactory;
import lib.ui.factories.MyListsPageObjectFactory;
import lib.ui.factories.NavigationUIFactory;
import lib.ui.factories.SearchPageObjectFactory;

public class HomeworkEx8 extends CoreTestCase
{
    private static final String name_of_folder = "Learning programming";
    public void testRefEx3()
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);

        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine("Unicorn");
        SearchPageObject.waitForSearchResult("Unicorn");
        int amount_of_search_results = SearchPageObject.getAmountOfFoundArticles();
        assertTrue(
                "We found too few results",
                amount_of_search_results >0);
        SearchPageObject.waitForCancelButtonToAppear();
        SearchPageObject.clickCancelSearch();
        SearchPageObject.assertThereIsNoResultOfSearch();
    }

    public void testRefEx5()
    { SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);

        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine("Java");
        SearchPageObject.clickByArticleWithSubstring("Object-oriented programming language");

        ArticlePageObject ArticlePageObject = ArticlePageObjectFactory.get(driver);
        ArticlePageObject.waitForTitleElement();
        String article_title = ArticlePageObject.getArticleTitle();

        if (Platform.getInstance().isAndroid()) {
        ArticlePageObject.addArticleToMyList(name_of_folder);
         } else {
        ArticlePageObject.addArticlesToMySaved();
                }

        NavigationUI NavigationUI = NavigationUIFactory.get(driver);

        if (Platform.getInstance().isIOS()) {
        NavigationUI.closeIOSSync();
            }
        ArticlePageObject.closeArticle();

        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine("Script");
        SearchPageObject.clickByArticleWithSubstring("Programming language");
        ArticlePageObject.waitForTitleElement();
        if (Platform.getInstance().isAndroid()) {
        ArticlePageObject.addSecondArticleToMyList(name_of_folder);
        } else {
        ArticlePageObject.addArticlesToMySaved();
        }

        ArticlePageObject.closeArticle();

        NavigationUI.clickMyLists();
        MyListsPageObject MyListPageObject = MyListsPageObjectFactory.get(driver);
        if (Platform.getInstance().isAndroid()) {
        MyListPageObject.openFolderByName(name_of_folder);
        }
        MyListPageObject.swipeByArticleToDelete(article_title);
        MyListPageObject.waitListsElementByDescription(article_title);
}

    public void testRefEx6()
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);

        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine("Jesus");
        SearchPageObject.clickByArticleWithSubstring("Jewish preacher and religious leader, central figure of Christianity");
        SearchPageObject.waitForArticleTitle();
    }
}



