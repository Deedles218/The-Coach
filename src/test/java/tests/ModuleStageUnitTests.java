package tests;

import lib.ModuleStage;
import lib.ui.ModulesPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Rectangle;

/** Offline tests for repeated stage identities and geometry failure modes. */
public class ModuleStageUnitTests {
    @Test public void sameStageInAnotherModuleIsNotTheSamePosition() {
        Assert.assertNotEquals(ModuleStage.parse("Module 1: A", "Stage 1 of 2"),
                ModuleStage.parse("Module 2: B", "Stage 1 of 2"));
    }
    @Test public void moduleTitleContentIsNotAnOracle() {
        Assert.assertEquals(ModuleStage.parse("Module 1: A", "Stage 1 of 2"),
                ModuleStage.parse("Module 1: Different title", "Stage 1 of 2"));
    }
    @Test(expected = AssertionError.class) public void dayLabelCannotPassAsStage() {
        ModuleStage.parse("Module 1: A", "Day 1 of 2");
    }
    @Test(expected = AssertionError.class) public void emptyModuleNameFails() {
        ModuleStage.parse("Module 1: ", "Stage 1 of 2");
    }
    @Test(expected = IllegalArgumentException.class) public void outOfRangeStageFails() {
        new ModuleStage(1, 3, 2);
    }
    @Test public void touchingEdgesDoNotOverlap() {
        Assert.assertFalse(ModulesPageObject.overlaps(new Rectangle(0, 0, 20, 10),
                new Rectangle(10, 0, 20, 10)));
    }
    @Test public void positiveAreaIntersectionIsAnOverlap() {
        Assert.assertTrue(ModulesPageObject.overlaps(new Rectangle(0, 0, 20, 10),
                new Rectangle(9, 0, 20, 10)));
    }
    @Test(expected = AssertionError.class) public void partiallyClippedElementFails() {
        ModulesPageObject.assertContained(new Rectangle(0, 0, 100, 100), new Rectangle(95, 5, 10, 10));
    }
    @Test(expected = AssertionError.class) public void emptyElementFails() {
        ModulesPageObject.assertContained(new Rectangle(0, 0, 100, 100), new Rectangle(5, 5, 0, 10));
    }
    @Test public void nonSquareContainerUsesHeightAndWidthCorrectly() {
        ModulesPageObject.assertContained(new Rectangle(0, 0, 200, 100), new Rectangle(90, 190, 10, 10));
    }
}
