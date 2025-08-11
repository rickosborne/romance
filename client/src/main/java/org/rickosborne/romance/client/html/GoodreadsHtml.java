package org.rickosborne.romance.client.html;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.UrlRank;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.util.ModelSetter.setIfEmpty;

@Slf4j
@RequiredArgsConstructor
public class GoodreadsHtml {
    public static final int DELAY_MS = 5000;
    public static final Pattern SERIES_NAME_AND_PART = Pattern.compile("\\((?<name>.+?)\\s+#(?<part>.+)\\)");
    private final Path cachePath;

    public AuthorModel getAuthorModel(@NonNull final URL url) {
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, cachePath, DELAY_MS, null, null);
        final AuthorModel author = AuthorModel.build();
        for (final AuthorPage value : AuthorPage.values()) {
            value.findAndSet(author, scraper);
        }
        final String photoHref = scraper.selectFirst(".authorLeftContainer a[href*=/photo/author/]")
            .getAttr("href");
        if (photoHref != null) {
            try {
                final URL photoPageUrl = new URIBuilder(url.toString()).setPath(photoHref).build().toURL();
                final HtmlScraper photoScraper = HtmlScraper.forUrlWithDelay(photoPageUrl, cachePath, DELAY_MS, null, null);
                final String largestPhotoUrl = photoScraper.selectFirst(".leftContainer a[href*=https://images.gr-assets.com]")
                    .getAttr("href");
                if (largestPhotoUrl != null) {
                    author.setPicUrl(new URL(largestPhotoUrl));
                }
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return author;
    }

    public BookModel getBookModel(@NonNull final URL url) {
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, cachePath, DELAY_MS, null, null);
        final BookModel book = BookModel.build();
        for (final BookPage value : BookPage.values()) {
            value.findAndSet(book, scraper);
        }
        return book;
    }

    @Getter
    public enum AuthorPage implements IAuthorPage {
        bioHtml(AuthorModel::setBioHtml, ".aboutAuthorInfo span:last-of-type", (t) -> Optional.ofNullable(t.getText()).map(b -> b.replaceFirst("<br>\\s+<br>\\s+<i>[^\\x01]*$", "").trim()).orElse(null)),
        goodreadsId(AuthorModel::setGoodreadsId, "a[href*=/author/show/]", (a) -> Optional.ofNullable(a.getAttr("href")).map((h) -> h.replaceFirst(".+?/[0-9]+[.].*$", "")).orElse(null)),
        goodreadsUrl(AuthorModel::setGoodreadsUrlFromString, "a[href*=/author/show/]", (a) -> Optional.ofNullable(a.getAttr("href")).map((id) -> id.startsWith("https") ? id : ("https://www.goodreads.com" + id)).orElse(null)),
        name(AuthorModel::setName, "h1.authorName", HtmlScraper::getText),
        ;
        private final String selector;
        private final BiConsumer<AuthorModel, String> setter;
        private final Function<HtmlScraper, String> stringifier;

        AuthorPage(
            @NonNull final BiConsumer<AuthorModel, String> setter,
            @NonNull final String selector,
            @NonNull final Function<HtmlScraper, String> stringifier
        ) {
            this.setter = setter;
            this.stringifier = stringifier;
            this.selector = selector;
        }

        AuthorPage(
            @NonNull final BiConsumer<AuthorModel, String> setter,
            @NonNull final String selector,
            @NonNull final String attributeName
        ) {
            this(setter, selector, s -> s.getAttr(attributeName));
        }
    }

    @Getter
    public enum BookPage implements IBookPage {
        authorGoodreadsUrl(BookModel::setAuthorGoodreadsUrl, "a.ContributorLink", "href"),
        title((b, t) -> b.setTitle(BookStuff.cleanTitle(t)), "meta[property=og:title]", "content"),
        titleLonger((b, t) -> b.setTitle(BookStuff.cleanTitle(t)), "#bookTitle", HtmlScraper::getText),
        goodreadsUrl((b, u) -> b.setGoodreadsUrl(StringStuff.urlFromString(u)), "link[rel=canonical]", "href"),
        imageUrl((b, u) -> b.setImageUrl(StringStuff.urlFromString(UrlRank.fixup(u))), "meta[property=og:image]", "content"),
        imageUrlTwitter((b, u) -> b.setImageUrl(StringStuff.urlFromString(UrlRank.fixup(u))), "meta[property=twitter:image]", "content"),
        // isbn(setIfEmpty(setButNot(BookModel::setIsbn, "null", ""), BookModel::getIsbn), "meta[property=books:isbn]", "content"),
        pages((b, p) -> b.setPages(Integer.parseInt(p, 10)), "meta[property=books:page_count]", "content"),
        pagesDetails((b, p) -> b.setPages(Integer.parseInt(p, 10)), "#details [itemprop=numberOfPages]", s -> s.getHtml().replace(" pages", "").replace(" page", "")),
        publisherDescription(setIfEmpty(BookModel::setPublisherDescription, BookModel::getPublisherDescription), "#description span[style]", HtmlScraper::getText),
        authorName(BookModel::setAuthorName, "#bookAuthors:first-of-type .authorName__container:first-of-type .authorName:first-of-type [itemprop=name]:first-of-type", HtmlScraper::getText),
        seriesName(BookModel::setSeriesName, "#bookSeries a", h -> {
            final String nameAndPart = h.getText();
            if (nameAndPart == null) {
                return null;
            }
            final Matcher matcher = SERIES_NAME_AND_PART.matcher(nameAndPart);
            if (matcher.find()) {
                return matcher.group("name").trim();
            }
            return null;
        }),
        seriesPart(BookModel::setSeriesPart, "#bookSeries a", h -> {
            final String nameAndPart = h.getText();
            if (nameAndPart == null) {
                return null;
            }
            final Matcher matcher = SERIES_NAME_AND_PART.matcher(nameAndPart);
            if (matcher.find()) {
                return matcher.group("part").trim();
            }
            return null;
        }),
        narratorName(BookModel::setNarratorName, "#bookAuthors:first-of-type .authorName__container:last-of-type .authorName.role", s -> {
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
                final int mo = English.MONTHS.indexOf(matcher.group("mo"));
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
    }
}
