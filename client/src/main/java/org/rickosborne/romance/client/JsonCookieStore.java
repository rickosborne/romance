package org.rickosborne.romance.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.util.Pair;

import java.io.IOException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JsonCookieStore implements CookieStore {
    public static JsonCookieStore fromPath(@NonNull final Path path) {
        try {
            final SimpleStore simpleStore = DbJsonWriter.getJsonMapper().readerFor(SimpleStore.class).readValue(path.toFile());
            final List<Pair<URI, HttpCookie>> cookies = simpleStore.getCookies().stream()
                .map(s -> Pair.build(s.getUri(), s.toHttpCookie()))
                .collect(Collectors.toList());
            return new JsonCookieStore(cookies, path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load JsonCookieStore: " + path, e);
        }
    }

    private final List<Pair<URI, HttpCookie>> cookies;

    private final Path storePath;

    @Override
    public void add(final URI uri, final HttpCookie cookie) {
        final URI shortUri;
        try {
            shortUri = new URI(uri.getScheme() + "://" + uri.getHost() + "/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final LinkedList<Pair<URI, HttpCookie>> updatedCookies = new LinkedList<>(cookies);
        cookies.removeIf(pair -> pair.getLeft().equals(shortUri) && pair.getRight().getName().equals(cookie.getName()));
        updatedCookies.add(Pair.build(shortUri, cookie));
        final List<SimpleCookie> simpleCookies = updatedCookies.stream().map(pair -> {
                final URI u = pair.getLeft();
                final HttpCookie c = pair.getRight();
                final URL url;
                try {
                    url = u == null ? null : u.toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                return new SimpleCookie(c.getName(), c.getPath(), url, c.getValue());
            })
            .sorted(Comparator.comparing(SimpleCookie::getUri).thenComparing(SimpleCookie::getName))
            .collect(Collectors.toList());
        try {
            DbJsonWriter.getJsonWriter().writeValue(storePath.toFile(), new SimpleStore(simpleCookies));
        } catch (IOException e) {
            throw new RuntimeException("Could not write cookieStore: " + storePath, e);
        }
    }

    @Override
    public List<HttpCookie> get(final URI uri) {
        final String full = uri.toString();
        return cookies.stream()
            .filter(pair -> full.startsWith(pair.getLeft().toString()))
            .map(Pair::getRight)
            .collect(Collectors.toList());
    }

    @Override
    public List<HttpCookie> getCookies() {
        return cookies.stream()
            .map(Pair::getRight)
            .collect(Collectors.toList());
    }

    @Override
    public List<URI> getURIs() {
        return cookies.stream()
            .map(Pair::getLeft)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public boolean remove(final URI uri, final HttpCookie cookie) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean removeAll() {
        cookies.clear();
        return true;
    }

    @Value
    static class SimpleCookie {
        String name;
        String path;
        URL url;
        String value;

        @JsonIgnore
        public URI getUri() {
            try {
                return url == null ? null : url.toURI().normalize();
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid URI: " + url, e);
            }
        }

        public HttpCookie toHttpCookie() {
            final HttpCookie cookie = new HttpCookie(name, value);
            cookie.setPath(path);
            cookie.setDomain(url.getHost());
            cookie.setSecure("https".equals(url.getProtocol()));
            return cookie;
        }
    }

    @Value
    static class SimpleStore {
        List<SimpleCookie> cookies;
    }
}
