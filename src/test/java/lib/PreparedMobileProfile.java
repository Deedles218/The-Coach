package lib;

import lib.ui.CoachFlowPageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;

/** Verifies the prepared test account without signing out or using an OTP. */
public final class PreparedMobileProfile {
    private PreparedMobileProfile() {}

    public static void assertPreservationAllowed() {
        Assert.assertEquals("Prepared profiles require logout isolation so the app is not reinstalled",
                "logout", TestData.testIsolationMode());
    }

    public static void verifyFromToday(CoachFlowPageObject coach) {
        String email = TestData.existingProgressAccountEmail();
        coach.openToday();
        coach.openProfile();
        if (Platform.getInstance().isAndroid()) {
            try {
                ((AndroidCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(email);
            } finally {
                coach.returnToMainScreenFromProfile();
            }
        } else {
            try {
                ((iOSCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(email);
            } finally {
                coach.openToday();
            }
        }
    }
}
