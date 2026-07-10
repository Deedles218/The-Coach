package lib.ui.factories;

import lib.Platform;

import java.util.function.Supplier;

final class IOSPageObjectFactory {
    private IOSPageObjectFactory() {
    }

    static <T> T create(Supplier<T> pageObjectSupplier) {
        Platform platform = Platform.getInstance();
        if (!platform.isIOS()) {
            throw new IllegalStateException(
                    "iOS page object requested for unsupported platform: " + platform.getPlatformVar()
            );
        }

        return pageObjectSupplier.get();
    }
}
