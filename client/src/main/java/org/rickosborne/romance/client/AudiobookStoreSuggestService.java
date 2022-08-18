package org.rickosborne.romance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.util.BookMerger;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.db.model.BookModel;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static org.rickosborne.romance.util.StringStuff.fuzzyMatch;
import static org.rickosborne.romance.util.StringStuff.nullIfBlank;

public interface AudiobookStoreSuggestService {
    Path CACHE_BASE_PATH = Path.of(".cache");
    String CACHE_NAME = "abs";
    int DELAY_SECONDS = 5;
    String SUGGEST_PATH = "/Handlers/SearchSuggestHandler.ashx?SearchType=100";

    static AudiobookStoreSuggestService build() {
        return new Retrofit.Builder()
            .baseUrl(AudiobookStore.SUGGEST_BASE)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(AudiobookStoreSuggestService.class);
    }

    static CacheClient<AudiobookStoreSuggestService> buildCaching() {
        return new CacheClient<>(build(), CACHE_BASE_PATH, CACHE_NAME, DELAY_SECONDS);
    }

    default BookModel findBookLike(
        final BookModel bookModel,
        @NonNull final UnaryOperator<BookModel> filter
    ) {
        if (bookModel == null) {
            return null;
        }
        final String sku = bookModel.getAudiobookStoreSku();
        return findBookLike(bookModel.getTitle(), sku, book -> {
            if (sku != null && sku.equals(book.getAudiobookStoreSku())) {
                return book;
            }
            return filter.apply(book);
        });
    }

    default BookModel findBookLike(
        final String title,
        final String audiobookStoreSku,
        final UnaryOperator<BookModel> filter
    ) {
        if (title == null) {
            return null;
        }
        final String lcTitle = title.toLowerCase();
        final List<AudiobookStoreSuggestion> suggestions = buildCaching().fetchFomCache(new TypeReference<>() {
        }, s -> s.suggest(lcTitle), lcTitle);
        if (suggestions == null) {
            return null;
        }
        final String sku = nullIfBlank(audiobookStoreSku);
        final UnaryOperator<BookModel> filterBooks = Optional.ofNullable(filter).orElse(b -> b);
        return suggestions.stream()
            .filter(s -> (sku == null || sku.equals(s.getKeyId())) && (fuzzyMatch(lcTitle, s.getTitle()) || fuzzyMatch(lcTitle, s.getCleanTitle().toLowerCase())))
            .map(BookMerger::modelFromAudiobookStoreSuggestion)
            .map(filterBooks)
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);
    }

    @GET(SUGGEST_PATH)
    Call<List<AudiobookStoreSuggestion>> suggest(
        @Query("term") String term
    );
}
