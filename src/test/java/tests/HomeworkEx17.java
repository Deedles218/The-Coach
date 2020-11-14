package tests;

import lib.CoreTestCase;
import lib.Platform;
import lib.ui.*;
import lib.ui.factories.ArticlePageObjectFactory;
import lib.ui.factories.MyListsPageObjectFactory;
import lib.ui.factories.NavigationUIFactory;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;

import static lib.ui.MyListsPageObject.*;

public class HomeworkEx17 extends CoreTestCase {
        private static final String name_of_folder = "Learning programming";
        private static final String login="AutomatorQA";
        private static final String password = "Automator2020";

        @Test
        public void testSaveFirstArticleToMyList() throws InterruptedException {
            SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);

            SearchPageObject.initSearchInput();
            SearchPageObject.typeSearchLine("Java");
            if ((Platform.getInstance().isAndroid())){
                SearchPageObject.clickByArticleWithSubstring("Island of Indonesia");
            } else {
                SearchPageObject.clickByArticleWithSubstring("sland");}

            ArticlePageObject ArticlePageObject = ArticlePageObjectFactory.get(driver);
            ArticlePageObject.waitForTitleElement();
            String article_title = ArticlePageObject.getArticleTitle();
            if (Platform.getInstance().isAndroid()) {
                ArticlePageObject.addArticleToMyList(name_of_folder);
            } else {
                ArticlePageObject.addArticlesToMySaved();
            }
            Thread.sleep(5000);

            if(Platform.getInstance().isMw()) {
                AuthorizationPageObject authorizationPageObject = new AuthorizationPageObject(driver);
                authorizationPageObject.clickAuthButton();
                Thread.sleep(5000);
                authorizationPageObject.enterLoginData(login, password);
                authorizationPageObject.submitForm();
                Thread.sleep(5000);
                ArticlePageObject.waitForTitleElement();
                assertEquals(
                        "We are not on the same page after login",
                        article_title,
                        ArticlePageObject.getArticleTitle()
                );
            }
            Thread.sleep(5000);
            if (Platform.getInstance().isMw()) {
                ArticlePageObject.addArticlesToMySaved();
            }
            NavigationUI NavigationUI = NavigationUIFactory.get(driver);

            if (Platform.getInstance().isIOS()) {
                NavigationUI.closeIOSSync();
            }
            Thread.sleep(5000);
            ArticlePageObject.closeArticle();
            NavigationUI.openNavigation();
            Thread.sleep(5000);
            NavigationUI.clickMyLists();
            Thread.sleep(5000);

            MyListsPageObject MyListPageObject = MyListsPageObjectFactory.get(driver);
            if (Platform.getInstance().isAndroid()) {
                MyListPageObject.openFolderByName(name_of_folder);
            }
            Thread.sleep(5000);
            MyListPageObject.swipeByArticleToDelete(article_title);
        }

        public void testSaveSecondArticle() throws InterruptedException{
            SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);

            SearchPageObject.initSearchInput();
            SearchPageObject.typeSearchLine("Java");
            if ((Platform.getInstance().isAndroid())){
                SearchPageObject.clickByArticleWithSubstring("Island of Indonesia");
            } else {
            SearchPageObject.clickByArticleWithSubstring("sland");}

             ArticlePageObject ArticlePageObject = ArticlePageObjectFactory.get(driver);
            ArticlePageObject.waitForTitleElement();
            String article_title = ArticlePageObject.getArticleTitle();

            if (Platform.getInstance().isAndroid()) {
                ArticlePageObject.addArticleToMyList(name_of_folder);
            } else {
                ArticlePageObject.addArticlesToMySaved();
            }
            Thread.sleep(5000);

            if(Platform.getInstance().isMw()) {
                AuthorizationPageObject authorizationPageObject = new AuthorizationPageObject(driver);
                authorizationPageObject.clickAuthButton();
                Thread.sleep(5000);
                authorizationPageObject.enterLoginData(login, password);
                authorizationPageObject.submitForm();
                Thread.sleep(5000);
                ArticlePageObject.waitForTitleElement();
                assertEquals(
                        "We are not on the same page after login",
                        article_title,
                        ArticlePageObject.getArticleTitle()
                );
            }
            Thread.sleep(5000);
            if (Platform.getInstance().isMw()) {
                ArticlePageObject.addArticlesToMySaved();
            }
            NavigationUI NavigationUI = NavigationUIFactory.get(driver);

            if (Platform.getInstance().isIOS()) {
                NavigationUI.closeIOSSync();
            }
            Thread.sleep(5000);
            ArticlePageObject.closeArticle();
            SearchPageObject.initSearchInput();
            SearchPageObject.typeSearchLine("JavaScript");
            if ((Platform.getInstance().isAndroid())){
                SearchPageObject.clickByArticleWithSubstring("Programming language");
            } else {
                SearchPageObject.clickByArticleWithSubstring("programming language");}
            ArticlePageObject.waitForTitleElement();


            if (Platform.getInstance().isAndroid()) {
                ArticlePageObject.addSecondArticleToMyList(name_of_folder);
                ArticlePageObject.closeArticle();
            } else if ((Platform.getInstance().isIOS())){ //тут дописала про айос
                ArticlePageObject.addArticlesToMySaved();
                ArticlePageObject.closeArticle();
            } else {
                ArticlePageObject.addArticlesToMySaved();
            }

            NavigationUI.openNavigation();
            Thread.sleep(5000);
            NavigationUI.clickMyLists();
            Thread.sleep(5000);

            MyListsPageObject MyListPageObject = MyListsPageObjectFactory.get(driver);
            if (Platform.getInstance().isAndroid()) {
                MyListPageObject.openFolderByName(name_of_folder);
            }

            Thread.sleep(5000);
            MyListPageObject.swipeByArticleToDelete(article_title);
            Thread.sleep(5000);
            MyListPageObject.waitListsElementByDescription(article_title);

            }
        }