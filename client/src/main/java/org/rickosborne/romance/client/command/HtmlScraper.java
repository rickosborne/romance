package org.rickosborne.romance.client.command;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.rickosborne.romance.util.StringStuff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Predicate;

@Log
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HtmlScraper {
    public static final int TIMEOUT_MS = 5000;

    public static HtmlScraper forUrl(
        @NonNull final URL url,
        @NonNull final Path cachePath
    ) {
        return forUrlWithDelay(url, cachePath, null);
    }

    public static HtmlScraper forUrlWithDelay(
        @NonNull final URL url,
        @NonNull final Path cachePath,
        final Integer delay
    ) {
        try {
            Document doc = fromCache(url, cachePath);
            if (doc == null) {
                if (delay != null) {
                    Thread.sleep(delay);
                }
                doc = Jsoup.parse(url, TIMEOUT_MS);
                final String fileName = StringStuff.cacheName(url) + ".html";
                try (final FileWriter fw = new FileWriter(cachePath.resolve(fileName).toFile())) {
                    fw.write(doc.outerHtml());
                }
            }
            return new HtmlScraper(cachePath, doc);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Document fromCache(@NonNull final URL url, @NonNull final Path cachePath) {
        final String fileName = StringStuff.cacheName(url) + ".html";
        final File file = cachePath.resolve(fileName).toFile();
        if (!cachePath.toFile().exists()) {
            cachePath.toFile().mkdirs();
        }
        if (file.isFile()) {
            try {
                return Jsoup.parse(file);
            } catch (IOException e) {
                log.warning("Could not parse: " + file + "; " + e.getMessage());
            }
        }
        return null;
    }

    private final Path cachePath;
    private final Element element;

    private HtmlScraper empty() {
        return new HtmlScraper(cachePath, null);
    }

    public String getAttr(@NonNull final String attrName) {
        if (element == null) {
            return null;
        }
        return element.attr(attrName);
    }

    public String getHtml() {
        if (element == null) {
            return null;
        }
        return element.html();
    }

    public String getText() {
        if (element == null) {
            return null;
        }
        return element.text();
    }

    public HtmlScraper parentHas(@NonNull final String selector) {
        if (element == null) {
            return this;
        }
        Element el = element;
        while (el != null) {
            final Element parent = el.parent();
            if (parent != null) {
                final Element descendents = parent.selectFirst(selector);
                if (descendents != null) {
                    return new HtmlScraper(cachePath, parent);
                }
                el = parent;
            }
        }
        return empty();
    }

    public HtmlScraper selectOne(@NonNull final String selector) {
        if (element == null) {
            return this;
        }
        final Elements elements = element.select(selector);
        if (elements.isEmpty()) {
            return empty();
        } else if (elements.size() == 1) {
            return new HtmlScraper(cachePath, elements.first());
        } else {
            throw new IllegalArgumentException("More than one element matches selector: " + selector);
        }
    }

    public HtmlScraper selectWithText(@NonNull final String selector, final Predicate<String> needle) {
        if (element == null) {
            return this;
        }
        final Element nextElement = element.select(selector).stream()
            .filter(el -> {
                final String text = el.ownText();
                //noinspection ConstantConditions
                return text != null && !text.isBlank() && needle.test(text);
            })
            .findFirst()
            .orElse(null);
        return new HtmlScraper(cachePath, nextElement);
    }
}
