package org.rickosborne.romance.client.html;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.command.HtmlScraper;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static org.rickosborne.romance.util.MathStuff.doubleFromDuration;

@RequiredArgsConstructor
public class AudiobookStoreHtml {
    public static final int DELAY_MS = 5000;
    public static final Duration MY_LIBRARY_EXPIRY = Duration.ofHours(1L);
    public static final URL MY_LIBRARY_URL = StringStuff.urlFromString("https://audiobookstore.com/my-library.aspx");
    private final Path cachePath;
    private final JsonCookieStore cookieStore;

    public BookModel getBookModel(@NonNull final AudiobookStoreSuggestion suggestion) {
        final URL url = suggestion.getUrl();
        if (url == null) {
            return null;
        }
        return getBookModel(url);
    }

    public BookModel getBookModel(@NonNull final BookInformation bookInformation) {
        final URL url = bookInformation.getUrl();
        if (url == null) {
            return null;
        }
        return getBookModel(url);
    }

    public BookModel getBookModel(@NonNull final URL url) {
        final HtmlScraper scraper = HtmlScraper.forUrl(url, cachePath);
        final String ld = scraper.selectOne("script[type=application/ld+json]").getHtml();
        final JsonNode ldNode = DbJsonWriter.readTree(ld);
        final BookModel book = BookModel.builder().build();
        book.setAudiobookStoreUrl(url);
        for (final BookModelLD ldItem : BookModelLD.values()) {
            final JsonNode value = ldNode.at(ldItem.getLdPath());
            if (value != null && value.isTextual()) {
                ldItem.getSetter().accept(book, value.asText());
            }
        }
        return book;
    }

    public List<BookModel> getPreorders() {
        Objects.requireNonNull(cookieStore, "Cookie store required to get preorders");
        HtmlScraper.expire(MY_LIBRARY_URL, MY_LIBRARY_EXPIRY, cachePath);
        final HtmlScraper html = HtmlScraper.forUrlWithDelay(MY_LIBRARY_URL, cachePath, DELAY_MS, cookieStore);
        final HtmlScraper recentOrders = html.selectOne(".recent-orders > table");
        final List<BookModel> preorders = new LinkedList<>();
        recentOrders.selectMany("tr:has(.library-preorder)", row -> {
            final BookModel.BookModelBuilder bookBuilder = BookModel.builder()
                .datePublish(StringStuff.toLocalDateFromMDY(row.selectOne(".library-preorder").getOwnText()))
                .audiobookStoreUrl(StringStuff.urlFromString(row.selectOne(".my-lib-img a:has(img)").getAttr("href")))
                .imageUrl(StringStuff.urlFromString(row.selectOne(".my-lib-img a img").getAttr("src")));
            row.selectMany(".my-lib-details .titledetail-specs > div", detail -> {
                final String label = detail.selectOne(".titledetail-label").getOwnText();
                final String value = detail.selectOne(".titledetail-value").getOwnText();
                switch (label) {
                    case "Title:":
                        bookBuilder.title(value);
                        break;
                    case "Author:":
                        bookBuilder.authorName(value);
                        break;
                    case "Reader:":
                        bookBuilder.narratorName(value);
                        break;
                    case "Run time:":
                        final Duration duration = Duration.parse(detail.selectOne("time[itemprop=timeRequired]").getAttr("datetime"));
                        bookBuilder.durationHours(duration.toMinutes() / 60d);
                        break;
                    case "Purchased:":
                        bookBuilder.datePurchase(LocalDate.parse(value, DateTimeFormatter.ofPattern("MM-dd-yyyy")));
                        break;
                }
            });
            preorders.add(bookBuilder.build());
        });
        return preorders;
    }

    @Getter
    @RequiredArgsConstructor
    enum BookModelLD {
        AuthorName("/mainEntity/author/name", BookModel::setAuthorName),
        NarratorName("/mainEntity/readBy/name", BookModel::setNarratorName),
        DatePublished("/mainEntity/datePublished", (b, d) -> b.setDatePublish(StringStuff.toLocalDate(d))),
        Duration("/mainEntity/timeRequired", (b, t) -> b.setDurationHours(doubleFromDuration(t))),
        Publisher("/mainEntity/publisher/name", BookModel::setPublisherName),
        Image("/mainEntity/image", (b, i) -> b.setImageUrl(StringStuff.urlFromString(i))),
        ;
        private final String ldPath;
        private final BiConsumer<BookModel, String> setter;
    }
}
