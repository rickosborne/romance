package org.rickosborne.romance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.rickosborne.romance.Goodreads;
import org.rickosborne.romance.client.response.GoodreadsAuthor;
import org.rickosborne.romance.client.response.GoodreadsAutoComplete;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.rickosborne.romance.Goodreads.API_PATH;

public interface GoodreadsService {
    String AUTO_COMPLETE_PATH = API_PATH + "book/auto_complete?format=json";
    Path CACHE_BASE_PATH = Path.of(".cache");
    String CACHE_KEY = "goodreads";
    int DELAY_SECONDS = 5;

    static GoodreadsService build() {
        return new Retrofit.Builder()
            .baseUrl(Goodreads.API_BASE)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(GoodreadsService.class);
    }

    static CacheClient<GoodreadsService> buildCaching() {
        return new CacheClient<>(build(), CACHE_BASE_PATH, CACHE_KEY, DELAY_SECONDS);
    }

    @GET(AUTO_COMPLETE_PATH)
    Call<List<GoodreadsAutoComplete>> autoComplete(
        @Query("q") String query
    );

    default GoodreadsAutoComplete findBook(
        final String title,
        final String author
    ) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            return null;
        }
        final String lcTitle = title.toLowerCase();
        final String lcAuthor = author.toLowerCase();
        final String query = lcTitle + " " + lcAuthor;
        final List<GoodreadsAutoComplete> completes = buildCaching().fetchFomCache(new TypeReference<>() {
        }, s -> s.autoComplete(query), query);
        if (completes == null || completes.isEmpty()) {
            return null;
        }
        return completes.stream()
            .filter(c -> {
                final String a = Optional.ofNullable(c.getAuthor()).map(GoodreadsAuthor::getName).map(String::toLowerCase).orElse(null);
                final String t = Optional.ofNullable(c.getTitle()).map(String::toLowerCase).orElse(null);
                final String b = Optional.ofNullable(c.getBookTitleBare()).map(String::toLowerCase).orElse(null);
                return (lcTitle.equals(t) || lcTitle.equals(b)) && lcAuthor.equals(a);
            })
            .findAny()
            .orElse(null);
    }
}
