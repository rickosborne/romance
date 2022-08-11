package org.rickosborne.romance.client.html;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.command.HtmlScraper;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.rickosborne.romance.util.StringStuff.nonBlank;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

@RequiredArgsConstructor
public class StoryGraphHtml {
    public static final String BASE_PATH = "https://app.thestorygraph.com";
    public static final int DELAY_MS = 4000;
    private final Path cachePath;
    private final JsonCookieStore cookieStore;

    public BookModel searchForBook(@NonNull final BookModel like) {
        final String authorName = like.getAuthorName();
        final String bookTitle = like.getTitle();
        if (authorName == null || bookTitle == null) {
            return null;
        }
        final URL url = StringStuff
            .urlFromString("https://app.thestorygraph.com/search?search_term=" + URLEncoder
                .encode(String.format("\"%s\" \"%s\"",
                    bookTitle.replace("\"", ""),
                    authorName.replace("\"", "").replace(".", ". ")
                ), StandardCharsets.UTF_8));
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, cachePath, DELAY_MS, null, Map.of(
            "Accept", "text/html, application/xhtml+xml",
            "Turbo-Frame", "search_results"
        ));
        final BookModel.BookModelBuilder builder = like.toBuilder();
        scraper
            .selectOne("#search-results-ul")
            .selectMany(".book-list-item a.book-list-option", bli -> {
                final Set<String> texts = new HashSet<>();
                bli.selectMany(".list-option-text", t -> {
                    final String text = t.getText();
                    if (nonBlank(text)) {
                        texts.add(text.toLowerCase());
                    }
                });
                if (texts.contains(authorName.toLowerCase()) && texts.contains(bookTitle.toLowerCase())) {
                    builder
                        .storygraphUrl(urlFromString(BASE_PATH + bli.getAttr("href")))
                        .imageUrl(urlFromString(bli.selectOne(".book-cover img").getAttr("src")));
                }
            });
        return builder.build();
    }
}
