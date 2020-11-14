package tests;

import lib.CoreTestCase;
import lib.Platform;
import lib.ui.SearchPageObject;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;

public class HomeworkEx18 extends CoreTestCase {
        @Test
        public void testCheckArticlesByTitleAndDescription()
        {
            SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
            SearchPageObject.initSearchInput();
            SearchPageObject.typeSearchLine("Java");

             if ((Platform.getInstance().isAndroid())){
                 SearchPageObject.waitForElementByTitleAndDescription("Java","Island of Indonesia");
            } else {
                 SearchPageObject.waitForElementByTitleAndDescription("Java","sland");
             }
            if ((Platform.getInstance().isAndroid())){
                SearchPageObject.waitForElementByTitleAndDescription("JavaScript","Programming language");
            } else {
                SearchPageObject.waitForElementByTitleAndDescription("JavaScript","rogramming language");
            }

            if ((Platform.getInstance().isAndroid())){
                SearchPageObject.waitForElementByTitleAndDescription("Java version history","Wikimedia list article");
            } else {
                SearchPageObject.waitForElementByTitleAndDescription("Javanese script","riting system");
            }
            ;


/* вариант с циклом при поиске по слову Languages проходит успешно
        String  article_element = null;
        for (int i = 1; i <= 3; i++) {
            article_element = SearchPageObject.waitForElementByTitleAndDescription("Languages","Languages");
            assertEquals("Different title and description", true, article_element.contains("Languages"));
        } */
        }
    }
