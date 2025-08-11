package org.rickosborne.romance.client.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.StringStuff;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudiobookStoreSuggestion {
    private static Pattern SKU_IN_URL = Pattern.compile("^/([a-zA-Z0-9])/([a-zA-Z0-9])/(\\1\\2[a-zA-Z0-9]+)/(\\3)-.*$");

    @JsonProperty("Format")
    String format;
    @JsonProperty("KeyId")
    String keyId;
    @JsonProperty("Name")
    String name;
    @JsonProperty("NameWithNoQuotes")
    String nameWithNoQuotes;
    @JsonProperty("Rank")
    Integer rank;
    @JsonProperty("SmallImageUrl")
    URL smallImageUrl;
    @JsonProperty("Title")
    String title;
    @JsonProperty("TypeId")
    Integer typeId;
    @JsonProperty("url")
    String url;
    @JsonProperty("Url")
    String urlPath;

    public String getCleanTitle() {
        return BookStuff.cleanTitle(nameWithNoQuotes == null ? title : nameWithNoQuotes);
    }

    public String getKeyId() {
        if (keyId != null || smallImageUrl == null) {
            return keyId;
        }
        final Matcher matcher = SKU_IN_URL.matcher(smallImageUrl.getPath());
        if (matcher.find()) {
            return matcher.group(3);
        }
        return null;
    }

    public URL getUrl() {
        if (url != null && !url.isBlank()) {
            return StringStuff.urlFromString(url);
        }
        if (urlPath == null || urlPath.isBlank()) {
            return null;
        }
        try {
            if (urlPath.contains("/audiobooks/") || urlPath.contains("/authors/") || urlPath.contains("/publishers/") || urlPath.contains("/narrators/")) {
                return new URL(AudiobookStore.SUGGEST_BASE + urlPath);
            }
            if (typeId == 2) {
                return new URL(AudiobookStore.SUGGEST_BASE + "/authors/" + urlPath + ".aspx");
            }
            if (typeId == 3) {
                return new URL(AudiobookStore.SUGGEST_BASE + "/narrators/" + urlPath + ".aspx");
            }
            if (typeId == 4) {
                return new URL(AudiobookStore.SUGGEST_BASE + "/publishers/" + urlPath + ".aspx");
            }
            return new URL(AudiobookStore.SUGGEST_BASE + "/audiobooks/" + urlPath + ".aspx");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnore
    public boolean isAuthor() {
        return typeId == 2;
    }

    @JsonIgnore
    public boolean isBook() {
        return typeId == 1;
    }

    @JsonIgnore
    public boolean isNarrator() {
        return typeId == 3;
    }

    @JsonIgnore
    public boolean isPublisher() {
        return typeId == 4;
    }
}
