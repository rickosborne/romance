package org.rickosborne.romance.client.html;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.client.command.HtmlScraper;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.nio.file.Path;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class AudiobookStoreHtml {
    private final Path cachePath;

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

    @Getter
    @RequiredArgsConstructor
    enum BookModelLD {
        AuthorName("/mainEntity/author/name", BookModel::setAuthorName),
        NarratorName("/mainEntity/readBy/name", BookModel::setNarratorName),
        DatePublished("/mainEntity/datePublished", (b, d) -> b.setDatePublish(StringStuff.toLocalDate(d))),
        Duration("/mainEntity/timeRequired", (b, t) -> b.setDurationHours(java.time.Duration.parse(t).toMinutes() / 60d)),
        Publisher("/mainEntity/publisher/name", BookModel::setPublisherName),
        Image("/mainEntity/image", (b, i) -> b.setImageUrl(StringStuff.urlFromString(i))),
        ;
        private final String ldPath;
        private final BiConsumer<BookModel, String> setter;
    }
}
