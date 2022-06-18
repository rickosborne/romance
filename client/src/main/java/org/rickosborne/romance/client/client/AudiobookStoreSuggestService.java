package org.rickosborne.romance.client.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.client.response.AudiobookStoreSuggestion;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.nio.file.Path;
import java.util.List;

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

    @GET(SUGGEST_PATH)
    Call<List<AudiobookStoreSuggestion>> suggest(
        @Query("term") String term
    );

    default AudiobookStoreSuggestion findBookByTitle(
        final String title
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
        return suggestions.stream()
            .filter(s -> lcTitle.equals(s.getTitle().toLowerCase()) || lcTitle.equals(s.getCleanTitle().toLowerCase()))
            .findAny()
            .orElse(null);
    }
}
