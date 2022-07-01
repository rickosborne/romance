package org.rickosborne.romance.client.html;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.client.command.HtmlScraper;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class GoodreadsHtml {
    public static final int DELAY_MS = 5000;
    private static final List<String> MONTHS = List.of(
        "",
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    );
    private final Path cachePath;

    public BookModel getBookModel(@NonNull final URL url) {
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, cachePath, DELAY_MS, null);
        final BookModel book = BookModel.build();
        for (final BookPage value : BookPage.values()) {
            value.findAndSet(book, scraper);
        }
        return book;
    }

    public enum BookPage {
        title(BookModel::setTitle, "meta[property=og:title]", "content"),
        titleLonger(BookModel::setTitle, "#bookTitle", HtmlScraper::getText),
        goodreadsUrl((b, u) -> b.setGoodreadsUrl(StringStuff.urlFromString(u)), "link[rel=canonical]", "href"),
        imageUrl((b, u) -> b.setImageUrl(StringStuff.urlFromString(u)), "meta[property=og:image]", "content"),
        imageUrlTwitter((b, u) -> b.setImageUrl(StringStuff.urlFromString(u)), "meta[property=twitter:image]", "content"),
        isbn(BookModel::setIsbn, "meta[property=books:isbn]", "content"),
        pages((b, p) -> b.setPages(Integer.parseInt(p, 10)), "meta[property=books:page_count]", "content"),
        pagesDetails((b, p) -> b.setPages(Integer.parseInt(p, 10)), "#details [itemprop=numberOfPages]", s -> s.getHtml().replace(" pages", "")),
        authorName(BookModel::setAuthorName, "#bookAuthors .authorName [itemprop=name]:first-of-type", HtmlScraper::getText),
        seriesName(BookModel::setSeriesName, "#bookSeries a", HtmlScraper::getText),
        narratorName(BookModel::setNarratorName, "#bookAuthors .authorName.role", s -> {
            if ("(Narrator)".equals(s.getHtml())) {
                return s.parentHas(".authorName").selectOne("[itemprop=name]:first-of-type").getHtml();
            }
            return null;
        }),
        datePublish((b, d) -> b.setDatePublish(StringStuff.toLocalDate(d)), "#details", s -> {
            final String line = s.selectWithText(".row", t -> t.contains("Published")).getText();
            if (line == null || line.isBlank()) {
                return null;
            }
            final Matcher matcher = Pattern.compile("Published\\s+(?<mo>\\S+)\\s+(?<day>\\d+)\\S*\\s+(?<year>\\d+)\\s+by").matcher(line);
            if (matcher.find()) {
                final int mo = MONTHS.indexOf(matcher.group("mo"));
                final int day = Integer.parseInt(matcher.group("day"), 10);
                final int year = Integer.parseInt(matcher.group("year"), 10);
                return String.format("%04d-%02d-%02d", year, mo, day);
            }
            return null;
        });
        private final String selector;
        private final BiConsumer<BookModel, String> setter;
        private final Function<HtmlScraper, String> stringifier;

        BookPage(
            @NonNull final BiConsumer<BookModel, String> setter,
            @NonNull final String selector,
            @NonNull final Function<HtmlScraper, String> stringifier
        ) {
            this.setter = setter;
            this.stringifier = stringifier;
            this.selector = selector;
        }

        BookPage(
            @NonNull final BiConsumer<BookModel, String> setter,
            @NonNull final String selector,
            @NonNull final String attributeName
        ) {
            this(setter, selector, s -> s.getAttr(attributeName));
        }

        public void findAndSet(@NonNull final BookModel book, @NonNull final HtmlScraper scraper) {
            final HtmlScraper localScraper = scraper.selectOne(selector);
            final String text = stringifier.apply(localScraper);
            if (text != null && !text.isBlank()) {
                setter.accept(book, text);
            }
        }
    }
}
