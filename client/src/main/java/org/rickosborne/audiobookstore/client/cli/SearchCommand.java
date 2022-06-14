package org.rickosborne.audiobookstore.client.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;
import org.rickosborne.audiobookstore.client.AudiobookStoreService;
import org.rickosborne.audiobookstore.client.AudiobookStoreSuggestService;
import org.rickosborne.audiobookstore.client.CacheClient;
import org.rickosborne.audiobookstore.client.GoodreadsService;
import org.rickosborne.audiobookstore.client.response.AudiobookStoreSuggestion;
import org.rickosborne.audiobookstore.client.response.BookInformation;
import org.rickosborne.audiobookstore.client.response.GoodreadsAuthor;
import org.rickosborne.audiobookstore.client.response.GoodreadsAutoComplete;
import org.rickosborne.audiobookstore.client.response.Login;
import org.rickosborne.audiobookstore.client.response.UserInformation2;
import picocli.CommandLine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

@Log
@CommandLine.Command(
    name = "search",
    description = "Search for a book by title"
)
public class SearchCommand implements Callable<Integer> {
    public static boolean caseInsensitiveCompare(final String a, final String b) {
        return a != null
            && b != null
            && a.trim().equalsIgnoreCase(b.trim());
    }

    @CommandLine.Mixin
    AudiobookStoreAuthOptions auth;
    @CommandLine.Option(names = "--author")
    String author;
    @CommandLine.Option(names = "--title")
    String title;

    @Override
    public Integer call() {
        if (title == null || title.isBlank()) {
            throw new NullPointerException("title");
        } else if (author == null || author.isBlank()) {
            throw new NullPointerException("author");
        }
        final String query = title + " " + author;
        final CacheClient<GoodreadsService> gr = GoodreadsService.buildCaching();
        final List<GoodreadsAutoComplete> grCompletes = gr.fetchFomCache(new TypeReference<>() {
        }, s -> s.autoComplete(query), query);
        if (grCompletes == null || grCompletes.isEmpty()) {
            throw new NullPointerException("Book not found in GoodReads");
        }
        final GoodreadsAutoComplete goodreads = grCompletes.stream()
            .filter(c -> caseInsensitiveCompare(title, c.getTitle()) && caseInsensitiveCompare(author, Optional.ofNullable(c.getAuthor()).map(GoodreadsAuthor::getName).orElse(null)))
            .findAny()
            .orElse(null);
        if (goodreads == null) {
            throw new NullPointerException("No matches found in " + grCompletes.size() + " results");
        }
        final CacheClient<AudiobookStoreSuggestService> suggestService = AudiobookStoreSuggestService.buildCaching();
        final List<AudiobookStoreSuggestion> absSuggestions = suggestService.fetchFomCache(new TypeReference<>() {
        }, s -> s.suggest(title), title);
        final AudiobookStoreSuggestion suggestion;
        if (absSuggestions != null && !absSuggestions.isEmpty()) {
            suggestion = absSuggestions.stream()
                .filter(s -> caseInsensitiveCompare(title, s.getTitle()))
                .findAny()
                .orElse(null);
        } else {
            suggestion = null;
        }
        BookInformation bookInformation;
        final CacheClient<AudiobookStoreService> abs = AudiobookStoreService.buildCaching();
        final Login login = auth.ensureAuthGuid(abs.getService());
        if (auth.getAbsUserGuid() != null && suggestion != null) {
            final String userGuid = auth.getAbsUserGuid().toString();
            final String sku = suggestion.getKeyId();
            bookInformation = abs.fetchFomCache(new TypeReference<>() {
            }, s -> s.bookInformation(userGuid, sku), sku);
            final UserInformation2 user = RetrofitCaller.fetchOrNull(abs.getService().userInformation2(userGuid));
            if (user != null) {
                final List<BookInformation> audiobooks = user.getAudiobooks();
                if (audiobooks != null) {
                    final BookInformation bookInfo = audiobooks.stream()
                        .filter(b -> Objects.equals(sku, b.getSku()))
                        .findAny()
                        .orElse(null);
                    if (bookInfo != null) {
                        bookInformation = BookMerger.merge(bookInfo, bookInformation);
                    }
                }
            }

        } else {
            bookInformation = null;
        }
        DocTabbed docTabbed = DocTabbed.fromGoodreadsAutoComplete(goodreads);
        if (suggestion != null) {
            docTabbed = docTabbed.merge(DocTabbed.fromAudiobookStoreSuggestion(suggestion));
        }
        if (bookInformation != null) {
            docTabbed = docTabbed.merge(DocTabbed.fromBookInformation(bookInformation));
        }
        System.out.println(docTabbed);
        return 0;
    }
}
