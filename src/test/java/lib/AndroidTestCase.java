package lib;

/** Base class for scenarios that are valid only for the Android application. */
public abstract class AndroidTestCase extends CoreTestCase {
    @Override
    protected boolean isPlatformSupported() {
        return Platform.getInstance().isAndroid();
    }

    @Override
    protected String unsupportedPlatformMessage() {
        return "Android-only test was skipped for platform " + Platform.getInstance().getPlatformVar();
    }
}
