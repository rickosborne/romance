package org.rickosborne.romance.client.html;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.rickosborne.romance.StoryGraph;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.UrlRank;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.StringStuff.ellipsize;
import static org.rickosborne.romance.util.StringStuff.nonBlank;
import static org.rickosborne.romance.util.StringStuff.uriFromString;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

@Slf4j
@RequiredArgsConstructor
public class StoryGraphHtml {
    public static final int DELAY_MS = 4000;
    public static final String REMEMBER_COOKIE_NAME = "remember_user_token";
    public static final String SESSION_COOKIE_NAME = "_storygraph_beta_session";
    private static final Set<String> relevantCookieNames = Set.of(REMEMBER_COOKIE_NAME, SESSION_COOKIE_NAME);
    private final Path cachePath;
    private final JsonCookieStore cookieStore;
    private final Map<String, String> requestHeaders = Map.of(
        "x-requested-with", "com.thestorygraph.thestorygraph",
        "user-agent", "Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 Build/SE1A.220630.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Mobile Safari/537.36",
        "referer", StoryGraph.API_ROOT
    );
    private final Pattern setCookiePattern = Pattern.compile("^\\s*(?<cookieName>[^=]+)\\s*=\\s*(?<cookieValue>[^;]*)\\s*;.*$");

    public Session ensureSignedIn(
        final Session maybeSession,
        final String email,
        final String password
    ) {
        final Session session = Optional.ofNullable(maybeSession).orElseGet(() -> new Session(cookieStore));
        final HtmlScraper home = HtmlScraper.forUrlWithDelay(urlFromString(StoryGraph.API_BASE + "/"), null, StoryGraph.DELAY_MS, cookieStore, session.getRequestHeaders());
        final String profilePath = home.selectOne("#user-menu-dropdown a[href^=/profile/]").getAttr("href");
        if (nonBlank(profilePath)) {
            log.info("Logged into StoryGraph as " + profilePath.replace("/profile/", ""));
            return session;
        }
        final SignInParams signInParams = fetchSignInParams();
        return postSignIn(signInParams.withEmailAndPassword(email, password));
    }

    public SignInParams fetchSignInParams() {
        final String signInUrl = StoryGraph.API_BASE + "/users/sign_in";
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(urlFromString(signInUrl), null, null, cookieStore, requestHeaders);
        final String csrfToken = scraper.selectOne("meta[name=csrf-token]").getAttr("content");
        if (csrfToken == null) {
            throw new IllegalStateException("Expected a CSRF token from StoryGraph");
        }
        final Connection.Response response = scraper.getResponse();
        final List<String> setCookies = response.headers("set-cookie");
        final Map<String, HttpCookie> cookiesByName = setCookies.stream()
            .flatMap(c -> HttpCookie.parse(c).stream())
            .collect(Collectors.toMap(HttpCookie::getName, c -> c));
        final HttpCookie sessionCookie = cookiesByName.get(SESSION_COOKIE_NAME);
        if (sessionCookie == null) {
            throw new IllegalStateException("Expected a session cookie from StoryGraph: " + setCookies);
        }
        final HttpCookie rememberCookie = Optional.ofNullable(cookiesByName.get(REMEMBER_COOKIE_NAME))
            .orElseGet(() -> {
                if (cookieStore == null) {
                    return null;
                }
                return cookieStore.getByName(REMEMBER_COOKIE_NAME);
            });
        final Map<String, String> cookies = Map.of(
            "cookie", String.format("%s=%s; %s=%s", sessionCookie.getName(), sessionCookie.getValue(), rememberCookie.getName(), rememberCookie.getValue())
        );
        if (cookieStore != null) {
            final URI sgUri = uriFromString(StoryGraph.API_ROOT);
            cookieStore.add(sgUri, sessionCookie);
            cookieStore.add(sgUri, rememberCookie);
        }
        final Map<String, String> bodyData = Map.of(
            "authenticity_token", csrfToken,
            "user[remember_me]", "1",
            "return_to", ""
        );
        return new SignInParams(cookies, bodyData, requestHeaders);
    }

    public Session postSignIn(
        final SignInParams params
    ) {
        final Document doc = HtmlScraper.postFormEncoded(StoryGraph.API_BASE + "/users/sign_in", params);
        final Connection.Response response = doc.connection().response();
        final Map<String, String> cookies = new HashMap<>(params.getCookies());
        for (final String setCookie : response.headers("set-cookie")) {
            for (final HttpCookie cookie : HttpCookie.parse(setCookie)) {
                final String cookieName = cookie.getName();
                final String cookieValue = cookie.getValue();
                if (cookieValue.isBlank()) {
                    cookies.remove(cookieName);
                } else {
                    log.info("Got StoryGraph cookie: " + cookieName + "=" + ellipsize(cookieValue, 10));
                    cookies.put(cookieName, cookieValue);
                    if (cookieStore != null) {
                        cookieStore.add(uriFromString(StoryGraph.API_ROOT), cookie);
                    } else {
                        log.warn("No SG cookie store");
                    }
                }
            }
        }
        return new Session(cookies, requestHeaders);
    }

    public BookModel searchForBook(@NonNull final BookModel like) {
        final String authorName = like.getAuthorName();
        final String bookTitle = like.getTitle();
        if (authorName == null || bookTitle == null) {
            return null;
        }
        final URL url = StringStuff
            .urlFromString(StoryGraph.API_BASE + "/search?search_term=" + URLEncoder
                .encode(String.format("\"%s\" \"%s\"",
                    bookTitle.replace("\"", ""),
                    authorName.replace("\"", "").replace(".", ". ")
                ), StandardCharsets.UTF_8));
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, cachePath, DELAY_MS, null, Map.of(
            "Accept", "text/html, application/xhtml+xml",
            "Turbo-Frame", "search_results"
        ));
        final BookModel.BookModelBuilder builder = like.toBuilder();
        scraper
            .selectOne("#search-results-ul")
            .selectMany(".book-list-item a.book-list-option", bli -> {
                final Set<String> texts = new HashSet<>();
                bli.selectMany(".list-option-text", t -> {
                    final String text = t.getText();
                    if (nonBlank(text)) {
                        texts.add(text.toLowerCase());
                    }
                });
                if (texts.contains(authorName.toLowerCase()) && texts.contains(bookTitle.toLowerCase())) {
                    builder
                        .storygraphUrl(urlFromString(StoryGraph.API_BASE + bli.getAttr("href")))
                        .imageUrl(UrlRank.fixup(urlFromString(bli.selectOne(".book-cover img").getAttr("src"))));
                }
            });
        return builder.build();
    }

    @Value
    @RequiredArgsConstructor
    public static class Session {
        Map<String, String> cookies;
        Map<String, String> requestHeaders;

        public Session(final CookieStore cookieStore) {
            requestHeaders = Collections.emptyMap();
            if (cookieStore != null) {
                cookies = cookieStore.getCookies().stream()
                    .filter(c -> relevantCookieNames.contains(c.getName()))
                    .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));
            } else {
                cookies = Collections.emptyMap();
            }
        }
    }

    @Value
    public static class SignInParams implements HtmlScraper.RequestExtras {
        Map<String, String> cookies;
        Map<String, String> requestBodyData;
        Map<String, String> requestHeaders;

        public SignInParams withEmailAndPassword(@NonNull final String email, @NonNull final String password) {
            final HashMap<String, String> bodyData = new HashMap<>(requestBodyData);
            bodyData.put("user[email]", email);
            bodyData.put("user[password]", password);
            return new SignInParams(cookies, bodyData, requestHeaders);
        }
    }
}
