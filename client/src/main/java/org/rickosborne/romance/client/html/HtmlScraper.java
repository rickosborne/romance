package org.rickosborne.romance.client.html;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.rickosborne.romance.StoryGraph;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.util.StringStuff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HtmlScraper {
    public static final String HTML_FILE_EXTENSION = ".html";
    public static final int TIMEOUT_MS = 9000;

    public static void expire(
        @NonNull final URL url,
        @NonNull final Duration maxAge,
        @NonNull final Path cachePath
    ) {
        final String fileName = StringStuff.cacheName(url) + HTML_FILE_EXTENSION;
        final File file = cachePath.resolve(fileName).toFile();
        if (!file.isFile()) {
            return;
        }
        final long lastModifiedMs = file.lastModified();
        final Instant lastModified = Instant.ofEpochMilli(lastModifiedMs);
        if (Instant.now().minus(maxAge).isAfter(lastModified)) {
            if (!file.delete()) {
                log.warn("Could not delete: " + file);
            }
        }
    }

    public static HtmlScraper forUrl(
        @NonNull final URL url,
        @NonNull final Path cachePath
    ) {
        return forUrlWithDelay(url, cachePath, null, null, null);
    }

    public static HtmlScraper forUrlWithCookies(
        @NonNull final URL url,
        @NonNull final Path cachePath,
        @NonNull final JsonCookieStore cookieStore
    ) {
        return forUrlWithDelay(url, cachePath, null, cookieStore, null);
    }

    public static HtmlScraper forUrlWithDelay(
        @NonNull final URL url,
        final Path cachePath,
        final Integer delay,
        final JsonCookieStore cookieStore,
        final Map<String, String> headers
    ) {
        try {
            if (cachePath != null) {
                final Document cachedDoc = fromCache(url, cachePath);
                if (cachedDoc != null) {
                    return new HtmlScraper(cachePath, cookieStore, cachedDoc);
                }
                if (delay != null) {
                    Thread.sleep(delay);
                }
            }
            Fetched.log.info("Fetch: " + url);
            final Document liveDoc = Jsoup.connect(url.toString())
                .cookieStore(cookieStore)
                .headers(Optional.ofNullable(headers).orElseGet(Collections::emptyMap))
                .timeout(TIMEOUT_MS)
                .get();
            if (cachePath != null) {
                final String fileName = StringStuff.cacheName(url) + HTML_FILE_EXTENSION;
                try (final FileWriter fw = new FileWriter(cachePath.resolve(fileName).toFile())) {
                    fw.write(liveDoc.outerHtml());
                }
            }
            return new HtmlScraper(cachePath, cookieStore, liveDoc);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static HtmlScraper forUrlWithHeaders(
        @NonNull final URL url,
        @NonNull final Path cachePath,
        @NonNull final Map<String, String> headers
    ) {
        return forUrlWithDelay(url, cachePath, null, null, headers);
    }

    protected static Document fromCache(@NonNull final URL url, @NonNull final Path cachePath) {
        final String fileName = StringStuff.cacheName(url) + HTML_FILE_EXTENSION;
        final File file = cachePath.resolve(fileName).toFile();
        if (!cachePath.toFile().exists()) {
            cachePath.toFile().mkdirs();
        }
        if (file.isFile()) {
            try {
                Cached.log.info("Cached: " + url);
                return Jsoup.parse(file);
            } catch (IOException e) {
                log.warn("Could not parse: " + file + "; " + e.getMessage());
            }
        }
        return null;
    }

    public static Document postFormEncoded(
        @NonNull final String url,
        @NonNull final RequestExtras requestExtras
    ) {
        try {
            return Jsoup.connect(url)
                .timeout(StoryGraph.DELAY_MS)
                .headers(requestExtras.getRequestHeaders())
                .method(Connection.Method.POST)
                .data(requestExtras.getRequestBodyData())
                .followRedirects(false)
                .cookies(requestExtras.getCookies())
                .post();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Path cachePath;
    private final JsonCookieStore cookieStore;
    private final Element element;

    private HtmlScraper empty() {
        return new HtmlScraper(cachePath, null, null);
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

    public String getOwnText() {
        if (element == null) {
            return null;
        }
        return element.ownText();
    }

    public Connection.Response getResponse() {
        if (element instanceof Document) {
            return ((Document) element).connection().response();
        }
        return null;
    }

    public String getText() {
        if (element == null) {
            return null;
        }
        return element.text();
    }

    public boolean isEmpty() {
        return element == null;
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
                    return new HtmlScraper(cachePath, cookieStore, parent);
                }
                el = parent;
            }
        }
        return empty();
    }

    public void selectMany(@NonNull final String selector, @NonNull Consumer<HtmlScraper> eachBlock) {
        if (element == null) {
            return;
        }
        for (final Element el : element.select(selector)) {
            eachBlock.accept(new HtmlScraper(cachePath, cookieStore, el));
        }
    }

    public HtmlScraper selectFirst(@NonNull final String selector) {
        if (element == null) {
            return this;
        }
        final Elements elements = element.select(selector);
        if (elements.isEmpty()) {
            return empty();
        } else {
            return new HtmlScraper(cachePath, cookieStore, elements.first());
        }
    }

    public HtmlScraper selectOne(@NonNull final String selector) {
        if (element == null) {
            return this;
        }
        final Elements elements = element.select(selector);
        if (elements.isEmpty()) {
            return empty();
        } else if (elements.size() == 1) {
            return new HtmlScraper(cachePath, cookieStore, elements.first());
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
        return new HtmlScraper(cachePath, cookieStore, nextElement);
    }

    public static interface RequestExtras {
        default Map<String, String> getCookies() {
            return Collections.emptyMap();
        }

        default Map<String, String> getRequestBodyData() {
            return Collections.emptyMap();
        }

        default Map<String, String> getRequestHeaders() {
            return Collections.emptyMap();
        }
    }

    @Slf4j
    public static class Cached {}

    @Slf4j
    public static class Fetched {}
}
