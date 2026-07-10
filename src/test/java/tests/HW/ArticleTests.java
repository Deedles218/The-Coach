package tests.HW;

import lib.AndroidTestCase;
import lib.ui.HWPageObject.ArticlePageObject;
import lib.ui.SearchPageObject;
import lib.ui.factories.ArticlePageObjectFactory;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

public class ArticleTests extends AndroidTestCase {
    @Test
    public void testCompareArticleTitle() {
        SearchPageObject searchPage = SearchPageObjectFactory.get(driver);
        searchPage.initSearchInput();
        searchPage.typeSearchLine("Java");
        searchPage.clickByArticleWithSubstring("bject-oriented programming language");
        ArticlePageObject articlePage = ArticlePageObjectFactory.get(driver);
        String articleTitle = articlePage.getArticleTitle();
        Assert.assertEquals(
                "We see unexpected title",
                "Java (programming language)",
                articleTitle
        );
    }

    @Test
    public void testSwipeArticle() {
        SearchPageObject searchPage = SearchPageObjectFactory.get(driver);
        searchPage.initSearchInput();
        searchPage.typeSearchLine("Java");
        searchPage.clickByArticleWithSubstring("bject-oriented programming language");
        ArticlePageObject articlePage = ArticlePageObjectFactory.get(driver);
        articlePage.waitForTitleElement();
        articlePage.swipeToFooter();
    }
}
