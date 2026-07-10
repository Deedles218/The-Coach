package tests.HW;

import lib.AndroidTestCase;
import lib.ui.HWPageObject.ArticlePageObject;
import lib.ui.SearchPageObject;
import lib.ui.factories.ArticlePageObjectFactory;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

public class ChangeAppCondition extends AndroidTestCase {
    @Test
    public void testChangeScreenOrientationOnSearchResult() {
        SearchPageObject searchPage = SearchPageObjectFactory.get(driver);
        searchPage.initSearchInput();
        searchPage.typeSearchLine("Java");
        searchPage.clickByArticleWithSubstring("Object-oriented programming language");
        ArticlePageObject articlePage = ArticlePageObjectFactory.get(driver);
        String titleBeforeRotation = articlePage.getArticleTitle();
        this.rotateScreenLandscape();
        String titleAfterRotation = articlePage.getArticleTitle();
        Assert.assertEquals(
                "Article title have been changed after rotation",
                titleBeforeRotation,
                titleAfterRotation
        );
        this.rotateScreenPortrait();
        String titleAfterSecondRotation = articlePage.getArticleTitle();
        Assert.assertEquals(
                "Article title have been changed after rotation",
                titleBeforeRotation,
                titleAfterSecondRotation
        );
    }

    @Test
    public void testCheckSearchArticleInBackground() {
        SearchPageObject searchPage = SearchPageObjectFactory.get(driver);
        searchPage.initSearchInput();
        searchPage.typeSearchLine("Java");
        searchPage.waitForSearchResult("Object-oriented programming language");
        backgroundApp(2);
        searchPage.waitForSearchResult("Object-oriented programming language");
    }
}
