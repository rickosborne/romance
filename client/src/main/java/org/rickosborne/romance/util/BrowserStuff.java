package org.rickosborne.romance.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.util.function.Function;

@Slf4j
@UtilityClass
public class BrowserStuff {
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private static final SafariDriver safari = Once.supply(() -> {
        WebDriverManager.safaridriver().browserInDocker().arm64().setup();
        final SafariDriver browser = new SafariDriver();
        Runtime.getRuntime().addShutdownHook(new Thread(browser::quit));
        return browser;
    });

    public static WebDriver getBrowser() {
        return getSafari();
    }

    public static <T> T withBrowser(final Function<WebDriver, T> block) {
        return block.apply(BrowserStuff.getBrowser());
    }
}
