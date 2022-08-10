package org.rickosborne.romance.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;

@UtilityClass
public class BrowserStuff {
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private static final SafariDriver safari = Once.supply(() -> {
        WebDriverManager.safaridriver().browserInDocker().arm64().setup();
        return new SafariDriver();
    });

    public static WebDriver getBrowser() {
        return getSafari();
    }
}
