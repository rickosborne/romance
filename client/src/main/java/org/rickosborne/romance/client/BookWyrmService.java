package org.rickosborne.romance.client;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.codec.CharEncoding;
import org.rickosborne.romance.client.bookwyrm.BookWyrmConfig;
import org.rickosborne.romance.client.response.BookWyrmBook;
import org.rickosborne.romance.db.DbJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface BookWyrmService {
    Pattern REMOTE_ID_PATTERN = Pattern.compile("/book/(?<bookId>[0-9]+)$");
    Logger log = LoggerFactory.getLogger(BookWyrmService.class);

    static BookWyrmService build(final BookWyrmConfig config) {
        return new Retrofit.Builder()
            .baseUrl(config.getApiBase())
            .addConverterFactory(JacksonConverterFactory.create(DbJsonWriter.getJsonMapper()))
            .client(buildOkHttpClient(config))
            .build()
            .create(BookWyrmService.class);
    }

    static BookWyrmService buildForHtml(final BookWyrmConfig config) {
        return new Retrofit.Builder()
            .baseUrl(config.getApiBase())
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(buildOkHttpClient(config))
            .build()
            .create(BookWyrmService.class);
    }

    static OkHttpClient buildOkHttpClient(final BookWyrmConfig config) {
        return new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                final Request request = chain.request();
                final Request patched = request.newBuilder()
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Referer", config.getApiBase())
                    .header("Accept", "application/json")
                    .header("User-Agent", BookWyrmService.class.getCanonicalName())
                    .method(request.method(), request.body())
                    .build();
                return chain.proceed(patched);
            })
            .connectTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build();
    }

    @POST("/book/{bookId}/filelink/add")
    @FormUrlEncoded
    Call<Void> addFileLink(
        @Path("bookId") final int bookId,
        @Field("book") final String bookIdText,
        @Field("url") final URL linkUrl,
        @Field("filetype") final String linkType,
        @Field("availability") final String availability,
        @Field("added_by") final String addedBy
    );

    default Integer bookIdFromRemoteId(final String remoteId) {
        final Matcher matcher = REMOTE_ID_PATTERN.matcher(remoteId);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group("bookId"), 10);
    }

    @POST("/create-book/confirm")
    @FormUrlEncoded
    Call<BookWyrmBook> createBook(
        @Field("last_edited_by") int lastEditedBy,
        @Field("parent_work") int parentWork,
        @Field("title") String title,
        @Field("subtitle") String subTitle,
        @Field("description") String description,
        @Field("series") String seriesName,
        @Field("series_number") String seriesNumber,
        @Field("languages") String languages,
        @Field("subjects") List<String> subjects,
        @Field("publishers") List<String> publishers,
        @Field("published_date_year") Integer publishedDateYear,
        @Field("published_date_month") Integer publishedDateMonth,
        @Field("published_date_day") Integer publishedDateDay,
        @Field("add_author") List<String> addAuthor,
        @Field("cover-url") URL coverUrl,
        @Field("physical_format") PhysicalFormat physicalFormat,
        @Field("physical_format_detail") String physicalFormatDetail,
        @Field("pages") Integer pages,
        @Field("isbn_13") String isbn13,
        @Field("isbn_10") String isbn10,
        @Field("openlibrary_key") String openLibraryKey,
        @Field("inventaire_id") String inventaire,
        @Field("oclc_number") String oclcNumber,
        @Field("asin") String asin,
        @Field("author-match-count") Integer authorMatchCount,
        // author_match-0
        @FieldMap Map<String, String> additionalFields
    );

    @GET("/book/{bookId}.json")
    Call<BookWyrmBook> getBook(
        @Path("bookId") final int bookId
    );

    @GET("/book/{bookId}")
    Call<ResponseBody> getBookHtml(
        @Path("bookId") final int bookId,
        @Header("Cookie") final String cookie
    );

    @POST("/post/rating")
    @FormUrlEncoded
    Call<ResponseBody> postRating(
        @Field("user") int userId,
        @Field("book") int bookId,
        @Field("privacy") Privacy privacy,
        @Field("rating") double rating
    );

    @POST("/shelve/")
    @FormUrlEncoded
    Call<ResponseBody> shelveBook(
        @Field("book") int bookId,
        @Field("shelf") String shelfNameAndId  // "uncategorized-5"
    );

    @POST("/upload-cover/{bookId}")
    @Multipart
    Call<Void> updateCoverFromUrl(
        @Path("bookId") final int bookId,
        @Part(value = "cover-url", encoding = CharEncoding.US_ASCII) final RequestBody coverUrl
    );

    default public Call<Void> updateCoverFromUrl(
        final int bookId,
        final URL coverUrl
    ) {
        final RequestBody coverRequest = RequestBody.create(MediaType.parse("text/plain"), coverUrl.toString());
        return updateCoverFromUrl(bookId, coverRequest);
    }

    enum PhysicalFormat {
        AudiobookFormat,
        EBook,
        GraphicNovel,
        Hardcover,
        Paperback,
        ;
    }

    enum Privacy {
        Public,
        Unlisted,
        ;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
