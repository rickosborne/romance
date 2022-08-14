package org.rickosborne.romance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.BookInformation;
import org.rickosborne.romance.db.model.BookModel;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.nio.file.Path;
import java.util.List;

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

    default AudiobookStoreSuggestion findBookLike(
        final BookModel bookModel
    ) {
        if (bookModel == null) {
            return null;
        }
        return findBookLike(bookModel.getTitle(), bookModel.getAudiobookStoreSku());
    }

    default AudiobookStoreSuggestion findBookLike(
        final BookInformation info
    ) {
        if (info == null) {
            return null;
        }
        return findBookLike(info.getCleanTitle(), info.getSku());
    }

    default AudiobookStoreSuggestion findBookLike(
        final String title,
        final String audiobookStoreSku
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
        return suggestions.stream()
            .filter(s -> (sku == null || sku.equals(s.getKeyId())) && (fuzzyMatch(lcTitle, s.getTitle()) || fuzzyMatch(lcTitle, s.getCleanTitle().toLowerCase())))
            .findAny()
            .orElse(null);
    }

    @GET(SUGGEST_PATH)
    Call<List<AudiobookStoreSuggestion>> suggest(
        @Query("term") String term
    );
}
