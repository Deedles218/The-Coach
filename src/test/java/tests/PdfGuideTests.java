package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic(value = "The Coach PDF Guides")
public class PdfGuideTests extends CoreTestCase {
    private static final String EXISTING_PROGRESS_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "PDF Guides")})
    @DisplayName("PDF guide upsell opens from My workbook")
    @Description("Covers the PDF Guide / E-workbook manual block by opening My workbook from Profile, verifying the upsell paywall, and closing it without purchasing.")
    @Step("Start test testPdfGuideUpsellOpensFromMyWorkbook")
    @Severity(value = SeverityLevel.CRITICAL)
    public void testPdfGuideUpsellOpensFromMyWorkbook() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        coachFlow.openProfile();
        coachFlow.openMyWorkbookFromProfile();
        coachFlow.assertPdfGuideUpsellIsDisplayed();
        coachFlow.closePdfGuideUpsell();
    }
}
