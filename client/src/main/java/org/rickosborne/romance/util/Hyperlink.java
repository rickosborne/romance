package org.rickosborne.romance.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.net.URI;
import java.net.URL;
import java.util.Comparator;

@Value
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Hyperlink {
    public static Comparator<Hyperlink> SORT_BY_TEXT = Comparator.comparing(Hyperlink::getText);

    public static Hyperlink build(final String text, final String href) {
        if (text == null || text.isBlank() || href == null || href.isBlank()) {
            return null;
        }
        return new Hyperlink(href.trim(), text.trim());
    }

    String href;
    String text;

    @JsonIgnore
    public URI uriFromHref() {
        return StringStuff.uriFromString(href);
    }

    @JsonIgnore
    public URL urlFromHref() {
        return StringStuff.urlFromString(href);
    }
}
