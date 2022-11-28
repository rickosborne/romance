package org.rickosborne.romance.client;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.codec.CharEncoding;
import org.rickosborne.romance.client.response.BookWyrmBook;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.util.Mutable;
import org.rickosborne.romance.util.StringStuff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface BookWyrmService {
    Pattern CSRF_INPUT = Pattern.compile("<input[^>]+(name=\"csrfmiddlewaretoken\"[^>]+value=\"(?<token1>[^\"]+)\"|value=\"(?<token2>[^\"]+)\"[^>]+name=\"csrfmiddlewaretoken\")");
    Pattern REMOTE_ID_PATTERN = Pattern.compile("/book/(?<bookId>[0-9]+)$");
    Logger log = LoggerFactory.getLogger(BookWyrmService.class);

    static BookWyrmService build(final String apiBase) {
        return new Retrofit.Builder()
            .baseUrl(apiBase)
            .addConverterFactory(JacksonConverterFactory.create(DbJsonWriter.getJsonMapper()))
            .build()
            .create(BookWyrmService.class);
    }

    static BookWyrmService buildForHtml(final String apiBase) {
        return new Retrofit.Builder()
            .baseUrl(apiBase)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(BookWyrmService.class);
    }

    static void tryToUpdateCsrf(
        final Mutable<String> csrf,
        final HttpUrl apiBase,
        final Response<?> response
    ) {
        if (response == null) {
            return;
        }
        for (final String setCookie : response.headers().values("Set-Cookie")) {
            final Cookie cookie = Cookie.parse(apiBase, setCookie);
            if (cookie == null || !"csrftoken".equals(cookie.name())) {
                continue;
            }
            final String updated = cookie.value();
            if (updated != null && !updated.isBlank()) {
                log.info("CSRF Cookie: {}", updated);
                csrf.setItem(updated);
            }
        }
    }

    default Integer bookIdFromRemoteId(final String remoteId) {
        final Matcher matcher = REMOTE_ID_PATTERN.matcher(remoteId);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group("bookId"), 10);
    }

    default String cookieHeader(final String csrfToken, final String sessionId) {
        if (csrfToken == null) {
            return String.format("django_language=en-us; sessionid=%s", sessionId);
        }
        return String.format("csrftoken=%s; django_language=en-us; sessionid=%s", csrfToken, sessionId);
    }

    @GET("/book/{bookId}.json")
    Call<BookWyrmBook> getBook(
        @Path("bookId") final int bookId
    );

    default void getBookCsrf(
        final int bookId,
        final String sessionId,
        final HttpUrl apiBase,
        final Mutable<String> csrfApp,
        final Mutable<String> csrfCookie
    ) {
        try {
            final Response<ResponseBody> response = getBookHtml(bookId, cookieHeader(null, sessionId)).execute();
            if (!response.isSuccessful()) {
                log.warn("Could not getBookHtml: " + response.code() + " " + response.message());
                return;
            }
            if (csrfCookie != null && apiBase != null) {
                tryToUpdateCsrf(csrfCookie, apiBase, response);
            }
            try (
                final ResponseBody body = response.body();
                final InputStreamReader isr = new InputStreamReader(body.byteStream());
                final BufferedReader in = new BufferedReader(isr)
            ) {
                String line = null;
                final StringBuilder sb = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                final Matcher matcher = CSRF_INPUT.matcher(sb.toString());
                if (matcher.find()) {
                    final String appToken = StringStuff.coalesceNonBlank(matcher.group("token1"), matcher.group("token2"));
                    if (appToken != null && !appToken.isBlank()) {
                        log.info("CSRF App: {}", appToken);
                        csrfApp.setItem(appToken);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to fetch getBookHtml", e);
        }
    }

    @GET("/book/{bookId}")
    Call<ResponseBody> getBookHtml(
        @Path("bookId") final int bookId,
        @Header("Cookie") final String cookie
    );

    @POST("/upload-cover/{bookId}")
    @Multipart
    @Headers({
        "Accept: application/json"
    })
    Call<Void> updateCoverFromUrl(
        @Path("bookId") final int bookId,
        @Part(value = "cover-url", encoding = CharEncoding.US_ASCII) final RequestBody coverUrl,
        @Part(value = "csrfmiddlewaretoken", encoding = CharEncoding.US_ASCII) final RequestBody csrfToken,
        @Header("Cookie") final String cookie,
        @Header("Referer") final String referer
    );

    default public Call<Void> updateCoverFromUrl(
        final int bookId,
        final URL coverUrl,
        final String csrfCookie,
        final String csrfApp,
        final String sessionId,
        final String referer
    ) {
        final RequestBody appRequest = RequestBody.create(MediaType.parse("text/plain"), csrfApp);
        final RequestBody coverRequest = RequestBody.create(MediaType.parse("text/plain"), coverUrl.toString());
        return updateCoverFromUrl(bookId, coverRequest, appRequest, cookieHeader(csrfCookie, sessionId), referer);
    }

}
