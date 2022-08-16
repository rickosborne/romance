package org.rickosborne.romance;

public interface StoryGraph {
    String API_HOST = "app.thestorygraph.com";
    String API_PATH = "/";
    String API_PROTO = "https";
    String API_BASE = API_PROTO + "://" + API_HOST;
    String API_ROOT = API_BASE + API_PATH;
    int DELAY_MS = 5000;
    String SIGN_IN_URL = API_BASE + "/users/sign_in";
}
