package org.rickosborne.romance.client.html;

import lombok.NonNull;
import org.rickosborne.romance.db.model.AuthorModel;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface IAuthorPage {
    default void findAndSet(@NonNull final AuthorModel author, @NonNull final HtmlScraper scraper) {
        final HtmlScraper localScraper = scraper.selectFirst(getSelector());
        if (localScraper.isEmpty()) {
            return;
        }
        final BiConsumer<AuthorModel, HtmlScraper> scrapeSetter = getScrapeSetter();
        final Function<HtmlScraper, String> stringifier = getStringifier();
        try {
            if (scrapeSetter != null) {
                scrapeSetter.accept(author, localScraper);
            } else if (stringifier != null) {
                final String text = stringifier.apply(localScraper);
                if (text != null && !text.isBlank()) {
                    getSetter().accept(author, text);
                }
            } else {
                throw new IllegalStateException("No stringifier or scrapeSetter: " + getClass().getSimpleName());
            }
        } catch (final NullPointerException e) {
            LoggerFactory.getLogger(getClass()).warn("NPE while parsing BookPage: {}", scraper.getUrl());
        }
    }

    default BiConsumer<AuthorModel, HtmlScraper> getScrapeSetter() {
        return null;
    }

    String getSelector();

    BiConsumer<AuthorModel, String> getSetter();

    Function<HtmlScraper, String> getStringifier();
}
