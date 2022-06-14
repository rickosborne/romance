package org.rickosborne.audiobookstore;

public interface Goodreads {
    String API_HOST = "www.goodreads.com";
    String API_PROTO = "https";
    String API_PATH = "/";
    String API_BASE = API_PROTO + "://" + API_HOST;
    String API_ROOT = API_BASE + API_PATH;
}
