package tests;

import lib.CoreTestCase;
import lib.ui.SearchPageObject;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;

public class HomeworkEx12 extends CoreTestCase {
    @Test
    public void testCheckArticlesByTitleAndDescription()
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.initSearchInput();
        SearchPageObject.typeSearchLine("Java");

        SearchPageObject.waitForElementByTitleAndDescription("Java","Indonesian island");
        SearchPageObject.waitForElementByTitleAndDescription("JavaScript","High-level programming language");
        SearchPageObject.waitForElementByTitleAndDescription("Java (programming language)","Object-oriented programming language");


/* вариант с циклом при поиске по слову Languages проходит успешно
        String  article_element = null;
        for (int i = 1; i <= 3; i++) {
            article_element = SearchPageObject.waitForElementByTitleAndDescription("Languages","Languages");
            assertEquals("Different title and description", true, article_element.contains("Languages"));
        } */
    }
}
