package org.rickosborne.romance.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.URL;
import java.util.function.Function;
import java.util.regex.Pattern;

public class UrlRank {
    public static final int RANK_DEFAULT = -1;

    public static URL choose(final URL a, final URL b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        }
        return StringStuff.urlFromString(choose(a.toString(), b.toString()));
    }

    public static String choose(final String a, final String b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        }
        final String fa = fixup(a);
        final String fb = fixup(b);
        final int ra = rank(fa);
        final int rb = rank(fb);
        return rb > ra ? fb : fa;
    }

    public static <T> String chooseString(final Function<T, String> accessor, final T[] items) {
        if (items == null || items.length == 0) {
            return null;
        }
        String a = accessor.apply(items[0]);
        if (items.length == 1) {
            return a;
        }
        for (int i = 1; i < items.length; i++) {
            final String b = accessor.apply(items[i]);
            a = choose(a, b);
        }
        return a;
    }

    public static <T> URL chooseUrl(final Function<T, URL> accessor, final T[] items) {
        if (items == null || items.length == 0) {
            return null;
        }
        URL a = accessor.apply(items[0]);
        if (items.length == 1) {
            return a;
        }
        for (int i = 1; i < items.length; i++) {
            final URL b = accessor.apply(items[i]);
            a = choose(a, b);
        }
        return a;
    }

    public static URL fixup(final URL url) {
        if (url == null) {
            return null;
        }
        return StringStuff.urlFromString(fixup(url.toString()));
    }

    public static String fixup(final String url) {
        if (url == null) {
            return null;
        }
        return url
            .replace("//s3-us-west-2.amazonaws.com/tabs.web.media/", "//media.audiobookstore.com/")
            .replace("images-na.ssl-images-amazon.com", "i.gr-assets.com")
            .replace("^(.+?i\\.gr-assets\\.com.+?\\.)_.+?_\\.", "$1")
            .replace("-square-\\d+.", "-square-1536.")
            ;
    }

    public static int rank(final URL url) {
        return url == null ? RANK_DEFAULT : rank(url.toString());
    }

    public static int rank(final String url) {
        for (final UrlRanking ranking : UrlRanking.values()) {
            if (ranking.test(url)) {
                return ranking.getRank();
            }
        }
        return RANK_DEFAULT;
    }

    @RequiredArgsConstructor
    public enum UrlRanking {
        MediaAudiobookStore(Pattern.compile("//media[.]audiobookstore[.]com/", Pattern.CASE_INSENSITIVE)),
        CdnStoryGraph(Pattern.compile("//cdn[.]storygraph[.]com/", Pattern.CASE_INSENSITIVE)),
        GrAssets(Pattern.compile("//i[.]gr-assets[.]com/", Pattern.CASE_INSENSITIVE))
        ;
        @NonNull
        private final Pattern pattern;

        public int getRank() {
            return values().length - this.ordinal();
        }

        public boolean test(@NonNull final String s) {
            return pattern.matcher(s).find();
        }
    }
}
