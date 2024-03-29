package org.rickosborne.romance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import okhttp3.ResponseBody;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.response.LibraryFile;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookMerger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

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
    String DOWNLOAD_PATH = "/download-file.aspx";
    String MY_LIBRARY_PATH = "/Handlers/MyLibraryHandler.ashx";
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

    @Streaming
    @GET(DOWNLOAD_PATH)
    @Headers({
        "Accept: */*; q=0.1",
        "Referer: https://audiobookstore.com/my-library.aspx",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/116.0"
    })
    Call<ResponseBody> downloadFile(
        @Query("FileName") String fileName,
        @Query("BookId") String sku,
        @Query("Url") String url,
        @Header("Cookie") String cookieHeader
    );

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
        }, s -> {
            LoggerFactory.getLogger(getClass()).info("Fetching TABS suggestions for: " + lcTitle);
            return s.suggest(lcTitle);
        }, lcTitle);
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

    @GET(MY_LIBRARY_PATH)
    @Headers({
        "Accept: application/json, text/javascript, */*; q=0.01",
        "Referer: https://audiobookstore.com/my-library.aspx",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/116.0",
        "X-Requested-With: XMLHttpRequest"
    })
    Call<List<LibraryFile>> getMyLibraryFiles(
        @Query("Method") String method,
        @Query("BookId") String sku,
        @Header("Cookie") String cookieHeader
    );

    @GET(SUGGEST_PATH)
    Call<List<AudiobookStoreSuggestion>> suggest(
        @Query("term") String term
    );

    enum MyLibraryFilesMethod {
        GetMp3Files,
        GetM4bFiles,
        GetBonusMaterialFiles,
    }
}
