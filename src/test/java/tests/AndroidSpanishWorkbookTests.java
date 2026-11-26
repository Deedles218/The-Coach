package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.RestoringTestAction;
import lib.ui.android.AndroidSpanishWorkbookPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Epic("The Coach for Him")
@Feature("PDF workbook")
public class AndroidSpanishWorkbookTests extends CoreTestCase {
    private String workbookLabel;
    private String coverResource;

    @Override
    protected boolean isPlatformSupported() {
        return Platform.getInstance().isAndroid();
    }

    @Before @Override
    public void setUp() throws Exception {
        if (isPlatformSupported()) {
            PreparedMobileProfile.assertPreservationAllowed();
            Assert.assertEquals("Use The Coach for Him", "com.vamapps.thecoach", Platform.getInstance().getAndroidAppPackage());
            workbookLabel = fixture("coach.pdf.spanishWorkbookLabel");
            coverResource = fixture("coach.pdf.coverResource");
        }
        super.setUp();
        PreparedMobileProfile.verifyFromToday(CoachFlowPageObjectFactory.get(driver));
    }

    @Test @Issue("COA-8864")
    @DisplayName("COA-8864 The workbook upsell shows its Spanish offer and a resolved price")
    public void testCoa8864WorkbookOfferInSpanish() throws Exception {
        AndroidSpanishWorkbookPageObject workbook = new AndroidSpanishWorkbookPageObject(driver);
        RestoringTestAction.run(() -> {
            workbook.openOffer(workbookLabel);
            workbook.assertRenderedCover(coverResource);
            for (String expected : new String[]{
                    "Cuaderno digital", "Habilidades esenciales de una gran pareja",
                    "Descuento para nuevos usuarios", "Conexión emocional más profunda",
                    "Mayor intimidad física", "Construye la relación perfecta",
                    "Compra única. Sin pagos recurrentes"}) {
                workbook.assertCopy(expected);
            }
            workbook.assertPurchaseDetails("Agregar a mi programa");
        }, workbook::closeOfferIfVisible);
    }

    private static String fixture(String property) {
        String value = System.getProperty(property, "").trim();
        Assert.assertFalse("Set -D" + property + " from the Spanish build's observed UI", value.isEmpty());
        return value;
    }

    @Override
    protected void cleanupSessionForNextTest() {
        // Preserve the prepared Spanish no-PDF QA profile.
    }
}
