package org.rickosborne.romance.client.html;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.UrlRank;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.db.model.SchemaAttribute.earlierSameYear;
import static org.rickosborne.romance.util.MathStuff.doubleFromDuration;
import static org.rickosborne.romance.util.ModelSetter.setIfEmpty;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

public class AudiobooksDotComHtml implements ILinkedData {

    public BookModel getBookModelFromBook(@NonNull final URL url) {
        final BookModel book = BookModel.builder().build();
        book.setAudiobooksDotComUrl(url);
        return getFromBook(book, url, BookModelLD.values(), BookModelHtml.values());
    }

    @Getter
    @RequiredArgsConstructor
    enum BookModelHtml implements HtmlData<BookModel> {
        publisherDescription((b, s) -> s.selectMany(".book-details__summary__description", summary -> {
            b.setPublisherDescription(summary.getText());
        })),
        seriesName((b, s) -> s.selectMany("a[href*='/browse/books-in-series/']", link -> {
            String part = null;
            String name = null;
            final String text = link.getText();
            final Matcher matcher = Pattern.compile("^#?(?<part>\\d+(?:\\.\\d+)?)\\s+of\\s+(?<name>.+)$").matcher(text);
            if (matcher.find()) {
                part = matcher.group("part");
                name = matcher.group("name");
            }
            if (name != null) {
                b.setSeriesName(name);
            }
            if (part != null) {
                b.setSeriesPart(part);
            }
        })),
        ;
        private final BiConsumer<BookModel, HtmlScraper> setter;
    }

    @Getter
    @RequiredArgsConstructor
    enum BookModelLD implements LinkedData<BookModel> {
        authorName("/author/name", null, BookModel::setAuthorName),
        datePublished("/datePublished", null, (b, s) -> b.setDatePublish(earlierSameYear(b.getDatePublish(), StringStuff.toLocalDate(s)))),
        image("/image", null, (b, s) -> b.setImageUrl(urlFromString(UrlRank.fixup(s)))),
        title("/name", null, setIfEmpty((b, s) -> b.setTitle(BookStuff.cleanTitle(s)), BookModel::getTitle)),
        url("/url", null, (b, s) -> b.setAudiobooksDotComUrl(urlFromString(UrlRank.fixup(s)))),
        duration("/workExample/duration", null, (b, t) -> b.setDurationHours(doubleFromDuration(t))),
        isbn("/workExample/isbn", null, BookModel::setIsbn),
        narrator("/workExample/readBy", (b, readBy) -> {
            final List<String> names = new LinkedList<>();
            readBy.forEach(rb -> {
                final String name = rb.at("/name").asText();
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            });
            final String allNames = names.stream().sorted().collect(Collectors.joining(", "));
            if (!allNames.isBlank()) {
                b.setNarratorName(allNames);
            }
        }, BookModel::setNarratorName),
        narrators("/workExample/readBy/name", null, BookModel::setNarratorName),
        ;
        private final String ldPath;
        private final BiConsumer<BookModel, JsonNode> nodeHandler;
        private final BiConsumer<BookModel, String> setter;
    }

    @Getter
    enum BookPage implements IBookPage {
        jsonLD((b, s) -> {

        }, "script[type='application/ld+json']"),
        ;
        private final BiConsumer<BookModel, HtmlScraper> scrapeSetter;
        private final String selector;
        private final BiConsumer<BookModel, String> setter;
        private final Function<HtmlScraper, String> stringifier;

        BookPage(
            @NonNull final BiConsumer<BookModel, String> setter,
            @NonNull final String selector,
            @NonNull final Function<HtmlScraper, String> stringifier
        ) {
            this.scrapeSetter = null;
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

        BookPage(
            @NonNull final BiConsumer<BookModel, HtmlScraper> scrapeSetter,
            @NonNull final String selector
        ) {
            this.scrapeSetter = scrapeSetter;
            this.selector = selector;
            this.stringifier = null;
            this.setter = null;
        }
    }
}
