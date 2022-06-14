package org.rickosborne.audiobookstore;

public interface AudiobookStore {
    String API_HOST = "api.audiobookstore.com";
    String API_PATH = "/";
    String API_PROTO = "https";
    String API_BASE = API_PROTO + "://" + API_HOST;
    String API_ROOT = API_BASE + API_PATH;
    String SUGGEST_HOST = "audiobookstore.com";
    String SUGGEST_BASE = API_PROTO + "://" + SUGGEST_HOST;
}
