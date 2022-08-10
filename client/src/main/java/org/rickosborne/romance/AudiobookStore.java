package org.rickosborne.romance;

import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.time.Duration;

public interface AudiobookStore {
    String API_HOST = "api.audiobookstore.com";
    String API_PATH = "/";
    String API_PROTO = "https";
    String API_BASE = API_PROTO + "://" + API_HOST;
    String API_ROOT = API_BASE + API_PATH;
    int DELAY_MS = 5000;
    Duration MY_LIBRARY_EXPIRY = Duration.ofHours(1L);
    String SUGGEST_HOST = "audiobookstore.com";
    String SUGGEST_BASE = API_PROTO + "://" + SUGGEST_HOST;
    URL MY_LIBRARY_URL = StringStuff.urlFromString(SUGGEST_BASE + "/my-library.aspx");
    String SIGN_IN_URL = SUGGEST_BASE + "/login.aspx";
    String WISHLIST_URL = SUGGEST_BASE + "/wishlist.aspx";
}
