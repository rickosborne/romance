package org.rickosborne.audiobookstore.client;

import org.rickosborne.audiobookstore.AudiobookStore;
import org.rickosborne.audiobookstore.client.response.BookInformation;
import org.rickosborne.audiobookstore.client.response.Bookmark;
import org.rickosborne.audiobookstore.client.response.Login;
import org.rickosborne.audiobookstore.client.response.PlaybackPosition;
import org.rickosborne.audiobookstore.client.response.UserInformation2;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.nio.file.Path;
import java.util.List;

import static org.rickosborne.audiobookstore.AudiobookStore.API_PATH;

public interface AudiobookStoreService {
    Path CACHE_BASE_PATH = Path.of(".cache");
    String CACHE_KEY = "abs-api";
    String TRACKING_HERPES = "OSType=android&Device=Google%20sdk_gphone64_arm64&OSVersion=12&AppVersion=v2.1.4.154%20Build%20154";
    String PLAYBACK_POSITIONS_PATH = API_PATH + "PlaybackPositions.aspx?Method=GET&IsPodcast=false&" + TRACKING_HERPES;
    String BOOKMARKS_PATH = API_PATH + "Bookmarks.aspx?Method=Get&IsPodcast=false&" + TRACKING_HERPES;
    String BOOK_INFORMATION_PATH = API_PATH + "GetBookInformation.aspx?" + TRACKING_HERPES;
    String CHECK_LOGIN_PATH = API_PATH + "CheckLogin.aspx?" + TRACKING_HERPES;
    String USER_INFORMATION2_PATH = API_PATH + "GetUserInformation2.aspx?" + TRACKING_HERPES;

    static AudiobookStoreService build() {
        return new Retrofit.Builder()
            .baseUrl(AudiobookStore.API_BASE)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(AudiobookStoreService.class);
    }

    static CacheClient<AudiobookStoreService> buildCaching() {
        return new CacheClient<>(build(), CACHE_BASE_PATH, CACHE_KEY);
    }

    @GET(BOOK_INFORMATION_PATH)
    Call<BookInformation> bookInformation(
        @Query("UserGuid") String userGuid,
        @Query("SKU") String sku
    );

    @GET(BOOKMARKS_PATH)
    Call<List<Bookmark>> bookmarks(
        @Query("UserGUID") String userGuid,
        @Query("SKU") String sku
    );

    @GET(CHECK_LOGIN_PATH)
    Call<Login> checkLogin(
        @Query("UserName") String userName,
        @Query("Password") String password
    );

    @GET(PLAYBACK_POSITIONS_PATH)
    Call<List<PlaybackPosition>> playbackPositions(
        @Query("UserGUID") String userGuid,
        @Query("SKU") String sku
    );

    @GET(USER_INFORMATION2_PATH)
    Call<UserInformation2> userInformation2(
        @Query("UserGuid") String userGuid
    );
}
