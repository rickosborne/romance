package org.rickosborne.romance.client.html;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.Mutable;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.UrlRank;

import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rickosborne.romance.util.DateStuff.fromMonthDayYear;

@Slf4j
@RequiredArgsConstructor
public class BellaHtml {
    private static final String AUDIOBOOKS_CATEGORY_URL_ROOT = "https://www.bellabooks.com/category/audio/";
    private static final Path CACHE_PATH = Path.of("./.cache/bella");
    public static final int DELAY_MS = 5000;
    public static final Pattern PAGE_PATTERN = Pattern.compile("/page/(?<num>[0-9]+)/");

    public static URL audioBookPageUrl(final int pageNum) {
        return StringStuff.urlFromString(AUDIOBOOKS_CATEGORY_URL_ROOT + (pageNum > 1 ? String.format("page/%d/", pageNum) : ""));
    }

    public LinksPage getAudiobookLinksPage(final int pageNum) {
        final URL url = audioBookPageUrl(pageNum);
        final List<URL> links = new LinkedList<>();
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, CACHE_PATH, DELAY_MS, null, null);
        scraper.selectMany(".products .product a", link -> {
            final String href = link.getAttr("href");
            if (href != null && href.startsWith("https://www.bellabooks.com/product/")) {
                links.add(StringStuff.urlFromString(href));
            }
        });
        final List<Integer> otherPages = new LinkedList<>();
        scraper.selectMany("a.page-numbers", link -> {
            final String href = link.getAttr("href");
            if (href != null && href.startsWith("https://www.bellabooks.com/")) {
                final Matcher matcher = PAGE_PATTERN.matcher(href);
                final String pageNumText = matcher.find() ? matcher.group("num") : null;
                if (pageNumText != null) {
                    final int otherPageNum = Integer.valueOf(pageNumText, 10);
                    if (otherPageNum != pageNum && !otherPages.contains(otherPageNum)) {
                        otherPages.add(otherPageNum);
                    }
                }
            }
        });
        return new LinksPage(links, otherPages, pageNum, url);
    }

    public BookModel getBookModel(final URL url) {
        final HtmlScraper scraper = HtmlScraper.forUrlWithDelay(url, CACHE_PATH, DELAY_MS, null, null);
        final BookModel book = BookModel.build();
        for (final BookPage bookEl : BookPage.values()) {
            bookEl.findAndSet(book, scraper);
        }
        return book;
    }

    @Getter
    enum BookPage implements IBookPage {
        title((b, t) -> b.setTitle(BookStuff.cleanTitle(t)), ".product_title strong", HtmlScraper::getText),
        author(BookModel::setAuthorName, ".product_author strong", HtmlScraper::getText),
        image((b, t) -> b.setImageUrl(StringStuff.urlFromString(UrlRank.fixup(t))), ".woocommerce-product-gallery__image a", "href"),
        dataTable((b, s) -> {
            b.setPublisherName("Bella");
            s.selectMany("tr", tr -> {
                final Mutable<String> label = Mutable.empty();
                tr.selectMany("td", td -> {
                    final String value = td.getText();
                    if (label.isEmpty()) {
                        label.setItem(value);
                        return;
                    }
                    switch (label.getItem()) {
                        case "Length":
                            final Matcher matcher = Pattern.compile("([0-9]+) pages").matcher(value);
                            final String pageCount = matcher.find() ? matcher.group(1) : null;
                            if (pageCount != null) {
                                b.setPages(Integer.parseInt(pageCount, 10));
                            }
                            break;
                        case "Publisher":
                            b.setPublisherName(value);
                            break;
                        case "Publication Date":
                            b.setDatePublish(fromMonthDayYear(value));
                            break;
                        case "ISBN":
                            b.setIsbn(value);
                            break;
                        default:
                            // do nothing
                    }
                });
            });
        }, ".summary table"),
        publisherDescription((b, s) -> {
            final StringBuilder description = new StringBuilder();
            s.selectMany(".br-30 ~ p", p -> {
                if (description.length() > 0) {
                    description.append("\n\n");
                }
                description.append(p.getText());
            });
            b.setPublisherDescription(description.toString());
        }, ".summary");
        private final BiConsumer<BookModel, HtmlScraper> scrapeSetter;
        private final String selector;
        private final BiConsumer<BookModel, String> setter;
        private final Function<HtmlScraper, String> stringifier;

        BookPage(
            @NonNull final BiConsumer<BookModel, String> setter,
            @NonNull final String selector,
            @NonNull final Function<HtmlScraper, String> stringifier
        ) {
            this.scrapeSetter = null;
            this.setter = setter;
            this.stringifier = stringifier;
            this.selector = selector;
        }

        BookPage(
            @NonNull final BiConsumer<BookModel, String> setter,
            @NonNull final String selector,
            @NonNull final String attributeName
        ) {
            this(setter, selector, s -> s.getAttr(attributeName));
        }

        BookPage(
            @NonNull final BiConsumer<BookModel, HtmlScraper> scrapeSetter,
            @NonNull final String selector
        ) {
            this.scrapeSetter = scrapeSetter;
            this.selector = selector;
            this.stringifier = null;
            this.setter = null;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class LinksPage {
        private final List<URL> bookLinks;
        private final List<Integer> pageLinkNums;
        private final int pageNum;
        private final URL url;
    }
}
