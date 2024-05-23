package org.rickosborne.romance.client.html;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HtmlScraper {
    public static final String HTML_FILE_EXTENSION = ".html";
    public static final int TIMEOUT_MS = 9000;
    private static final Map<String, LocalDateTime> waitUntil = new ConcurrentHashMap<>();

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
                log.warn("Could not delete: {}", file);
            }
        }
    }

    public static HtmlScraper forUrl(
        @NonNull final URL url,
        @NonNull final Path cachePath
    ) {
        return scrape(Scrape.builder()
            .url(url)
            .cachePath(cachePath)
            .build());
    }

    public static HtmlScraper forUrlWithCookies(
        @NonNull final URL url,
        @NonNull final Path cachePath,
        @NonNull final JsonCookieStore cookieStore
    ) {
        return scrape(Scrape.builder()
            .url(url)
            .cachePath(cachePath)
            .cookieStore(cookieStore)
            .build());
    }

    public static HtmlScraper forUrlWithDelay(
        @NonNull final URL url,
        final Path cachePath,
        final Integer delay,
        final JsonCookieStore cookieStore,
        final Map<String, String> headers
    ) {
        return scrape(Scrape.builder()
            .url(url)
            .cachePath(cachePath)
            .delay(delay)
            .cookieStore(cookieStore)
            .headers(headers)
            .build());
    }

    public static HtmlScraper forUrlWithHeaders(
        @NonNull final URL url,
        @NonNull final Path cachePath,
        @NonNull final Map<String, String> headers
    ) {
        return scrape(Scrape.builder()
            .url(url)
            .cachePath(cachePath)
            .headers(headers)
            .build());
    }

    protected static Document fromCache(
        @NonNull final URL url,
        @NonNull final Path cachePath,
        final Duration maxAge
    ) {
        final String fileName = StringStuff.cacheName(url) + HTML_FILE_EXTENSION;
        final File file = cachePath.resolve(fileName).toFile();
        if (!cachePath.toFile().exists()) {
            if (!cachePath.toFile().mkdirs()) {
                log.warn("Could not create cachePath: {}", cachePath);
            }
        }
        if (file.isFile()) {
            if (maxAge != null) {
                final long lastModMS = file.lastModified();
                final Instant lastModInstant = Instant.ofEpochMilli(lastModMS);
                if (lastModInstant.plus(maxAge).isBefore(Instant.now())) {
                    log.info("Cache is out of date: {}", file);
                    if (!file.delete()) {
                        log.warn("Could not delete: {}", file);
                    }
                    return null;
                }
            }
            try {
                Cached.log.info(url.toString());
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

    public static HtmlScraper scrape(@NonNull final Scrape scrape) {
        try {
            if (scrape.cachePath != null) {
                final Document cachedDoc = fromCache(scrape.url, scrape.cachePath, scrape.maxAge);
                if (cachedDoc != null) {
                    return new HtmlScraper(scrape.cachePath, scrape.cookieStore, cachedDoc, scrape.url);
                }
                if (scrape.delay != null) {
                    final String host = scrape.url.getHost();
                    final LocalDateTime maybeNext = waitUntil.get(host);
                    final Long ms = maybeNext == null ? null : ChronoUnit.MILLIS.between(LocalDateTime.now(), maybeNext);
                    if (ms != null && ms > 0) {
                        log.info("Sleeping {}ms for {}", ms, host);
                        Thread.sleep(ms);
                    }
                    waitUntil.put(host, LocalDateTime.now().plus(scrape.delay, ChronoUnit.MILLIS));
                }
            }
            final String scrapeUrl = scrape.url.toString();
            Fetched.log.info(scrapeUrl);
            final Document liveDoc = Jsoup.connect(scrapeUrl)
                .cookieStore(scrape.cookieStore)
                .headers(Optional.ofNullable(scrape.headers).orElseGet(Collections::emptyMap))
                .timeout(TIMEOUT_MS)
                .get();
            if (scrape.cachePath != null) {
                final String fileName = StringStuff.cacheName(scrape.url) + HTML_FILE_EXTENSION;
                try (final FileWriter fw = new FileWriter(scrape.cachePath.resolve(fileName).toFile())) {
                    fw.write(liveDoc.outerHtml());
                }
            }
            return new HtmlScraper(scrape.cachePath, scrape.cookieStore, liveDoc, scrape.url);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final Path cachePath;
    private final JsonCookieStore cookieStore;
    private final Element element;
    @Getter
    private final URL url;

    private HtmlScraper empty() {
        return new HtmlScraper(cachePath, null, null, null);
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
                    return new HtmlScraper(cachePath, cookieStore, parent, url);
                }
            }
            el = parent;
        }
        return empty();
    }

    public HtmlScraper selectFirst(@NonNull final String selector) {
        if (element == null) {
            return this;
        }
        final Elements elements = element.select(selector);
        if (elements.isEmpty()) {
            return empty();
        } else {
            return new HtmlScraper(cachePath, cookieStore, elements.first(), url);
        }
    }

    public void selectMany(@NonNull final String selector, @NonNull Consumer<HtmlScraper> eachBlock) {
        if (element == null) {
            return;
        }
        for (final Element el : element.select(selector)) {
            eachBlock.accept(new HtmlScraper(cachePath, cookieStore, el, url));
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
            return new HtmlScraper(cachePath, cookieStore, elements.first(), url);
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
                // noinspection ConstantConditions
                return text != null && !text.isBlank() && needle.test(text);
            })
            .findFirst()
            .orElse(null);
        return new HtmlScraper(cachePath, cookieStore, nextElement, url);
    }

    public interface RequestExtras {
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
    public static class Cached {
    }

    @Slf4j
    public static class Fetched {
    }

    @Value
    @Builder
    public static class Scrape {
        Path cachePath;
        JsonCookieStore cookieStore;
        Integer delay;
        Map<String, String> headers;
        Duration maxAge;
        @NonNull
        URL url;
    }
}
