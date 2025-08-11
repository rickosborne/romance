package org.rickosborne.romance.client.audiobookshelf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface AudiobookShelfService {
    static AudiobookShelfService build(@NonNull final AudiobookShelfConfig config) {
        final String apiToken = config.apiToken;
        Objects.requireNonNull(apiToken, "AudiobookShelfConfig#apiToken");
        final OkHttpClient client = new OkHttpClient.Builder()
            .addNetworkInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                .addHeader("Authorization", "Bearer " + apiToken)
                .build()))
            .connectTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofMinutes(2))
            .callTimeout(Duration.ofMinutes(2))
            .build();
        return new Retrofit.Builder()
            .baseUrl(config.getUrl())
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(AudiobookShelfService.class);
    }

    @POST("/api/authors/{authorId}")
    Call<AudiobookShelfAuthor> addAuthorImageById(@Path("authorId") UUID authorId, @Body String imageUrl);

    @POST("/api/libraries")
    Call<List<AudiobookShelfLibrary>> createLibrary(@Body AudiobookShelfLibrary library);

    @GET("/api/authors/{authorId}")
    Call<AudiobookShelfAuthor> getAuthor(@Path("authorId") UUID authorId);

    @GET("/api/authors/{authorId}/image")
    Call<Object> getAuthorImage(@Path("authorId") UUID authorId, @Body AudiobookShelfImageRequest image);

    @GET("/audiobookshelf/api/libraries/{id}/authors?limit=9999&sort=name")
    Call<AudiobookShelfAuthorsPage> getAuthors(@Path("id") UUID libraryId);

    @GET("/audiobookshelf/api/items/{id}")
    Call<AudiobookShelfLibraryItemMinified> getItem(@Path("id") UUID libraryItemId);

    @GET("/audiobookshelf/api/items/{id}?expanded=1")
    Call<AudiobookShelfLibraryItemMinified> getItemExpanded(@Path("id") UUID libraryItemId);

    @GET("/audiobookshelf/api/me/progress/{id}")
    Call<AudiobookShelfProgressUpdate> getItemProgress(@Path("id") UUID libraryItemId);

    @GET("/audiobookshelf/api/libraries")
    Call<GetLibrariesResponse> getLibraries();

    @GET("/audiobookshelf/api/libraries/{id}")
    Call<GetLibraryByIdResponse> getLibraryById(@Path("id") UUID libraryId);

    @GET("/audiobookshelf/api/libraries/{id}?include=filterdata")
    Call<GetLibraryByIdResponse> getLibraryByIdWithFilterData(@Path("id") UUID libraryId);

    @GET("/audiobookshelf/api/libraries/{id}/items")
    Call<AudiobookShelfLibraryItemsPage> getLibraryItems(
        @Path("id") UUID libraryId,
        @Query("limit") Integer limit,
        @Query("page") Integer page,
        @Query("sort") String sort,
        @Query("desc") Integer desc,
        @Query("filter") String filter,
        @Query("include") String include,
        @Query("minified") Integer minified,
        @Query("collapseSeries") Integer collapseSeries
    );

    @GET("/api/libraries/{id}/series")
    Call<AAudiobookShelfPage> getLibrarySeries(
        @Path("id") UUID libraryId,
        @Query("limit") Integer limit,
        @Query("page") Integer page
    );

    @GET("/api/series/{id}")
    Call<AudiobookShelfSeriesWithProgressAndRSS> getSeries(@Path("id") UUID seriesId);

    @PATCH("/audiobookshelf/api/authors/{authorId}")
    Call<UpdateAuthorResponse> updateAuthorById(@Path("authorId") UUID authorId, @Body AudiobookShelfAuthor author);

    @POST("/audiobookshelf/api/authors/{authorId}/image")
    Call<UpdateAuthorResponse> updateAuthorImageById(@Path("authorId") UUID authorId, @Body UpdateImageRequest image);

    @POST("/audiobookshelf/api/items/{id}/cover")
    Call<UpdateItemCoverResponse> updateItemCover(@Path("id") UUID libraryItemId, @Body UpdateImageRequest request);

    @PATCH("/audiobookshelf/api/items/{id}/media")
    Call<UpdateItemMediaResponse> updateItemMedia(@Path("id") UUID libraryItemId, @Body AudiobookShelfBookMinified book);

    @PATCH("/audiobookshelf/api/me/progress/{id}")
    Call<Void> updateItemProgress(@Path("id") UUID libraryItemId, @Body AudiobookShelfProgressUpdate progress);

    @PATCH("/api/libraries/{id}")
    Call<AudiobookShelfLibrary> updateLibraryById(@Path("id") UUID libraryId, @Body AudiobookShelfLibrary library);

    @PATCH("/api/series/{id}")
    Call<AudiobookShelfSeriesWithProgressAndRSS> updateSeries(@Path("id") UUID seriesId, AudiobookShelfSeries series);

    @Data
    class GetLibrariesResponse {
        List<AudiobookShelfLibrary> libraries;
    }

    @Data
    @JsonIgnoreProperties("customMetadataProviders")
    class GetLibraryByIdResponse {
        @JsonProperty("filterdata")
        LibraryFilterData filterData;
        int issues;
        AudiobookShelfLibrary library;
        int numUserPlaylists;
    }

    @Data
    class IdAndName {
        UUID id;
        String name;
    }

    @Data
    class LibraryFilterData {
        int authorCount;
        List<IdAndName> authors;
        int bookCount;
        List<String> genres;
        List<String> languages;
        long loadedAt;
        List<String> narrators;
        int numIssues;
        int podcastCount;
        List<String> publishedDecades;
        List<String> publishers;
        List<AudiobookShelfSeriesSequence> series;
        int seriesCount;
        List<String> tags;
    }

    @Data
    class UpdateAuthorResponse {
        AudiobookShelfAuthorExpanded author;
        Boolean updated;
    }

    @Data
    @AllArgsConstructor
    class UpdateImageRequest {
        String url;
    }

    @Data
    class UpdateItemCoverResponse {
        String cover;
        boolean success;
    }

    @Data
    class UpdateItemMediaResponse {
        AudiobookShelfLibraryItem libraryItem;
        boolean updated;
    }
}
