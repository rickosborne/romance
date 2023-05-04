package org.rickosborne.romance.client.html;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.rickosborne.romance.db.DbJsonWriter;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BiConsumer;

import static org.rickosborne.romance.AudiobookStore.DELAY_MS;

public interface ILinkedData {
    Duration MAX_AGE = Duration.ofDays(90);

    default String getCacheIdentifier() {
        return getClass().getSimpleName()
            .replace("Html", "")
            .toLowerCase();
    }

    default Path getCachePath() {
        return Path.of(".cache", getCacheIdentifier());
    }

    default <M, E extends Enum<E> & LinkedData<M>, H extends Enum<H> & HtmlData<M>> M getFromBook(
        @NonNull final M model,
        @NonNull final URL bookUrl,
        final E[] ldValues,
        final H[] htmlValues
    ) {
        final HtmlScraper scraper = HtmlScraper.scrape(HtmlScraper.Scrape.builder()
            .url(bookUrl)
            .cachePath(getCachePath())
            .delay(DELAY_MS)
            .maxAge(MAX_AGE)
            .build());
        final String ld = scraper.selectOne("script[type=application/ld+json]").getHtml();
        final JsonNode ldNode = DbJsonWriter.readTree(ld);
        if (ldValues != null) {
            for (final E ldItem : ldValues) {
                final JsonNode value = ldNode.at(ldItem.getLdPath());
                if (value == null) {
                    continue;
                }
                if (value.isTextual()) {
                    ldItem.getSetter().accept(model, value.asText());
                } else {
                    final BiConsumer<M, JsonNode> nodeHandler = ldItem.getNodeHandler();
                    if (nodeHandler != null) {
                        nodeHandler.accept(model, value);
                    }
                }
            }
        }
        if (htmlValues != null) {
            for (final H htmlItem : htmlValues) {
                htmlItem.getSetter().accept(model, scraper);
            }
        }
        return model;
    }
}
