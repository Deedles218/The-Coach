package lib.ui;

/** Native lesson sharing actions exercised by the same TMS cases on both platforms. */
public interface VideoSharingPageObject {
    void openByExitingVideo(String lesson);
    void openByFinishingVideo(String lesson);
    String readSharePreview(String action);
    void dismissShareSheetIfPresent();
    void closePopupIfPresent();
    void returnFromSharingToToday();
    void restorePreparedToday();
}
