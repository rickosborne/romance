package org.rickosborne.romance.client.html;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.audiobookstore.AbsPaginatedVisitor;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.model.NarratorModel;
import org.rickosborne.romance.db.model.SeriesModel;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.BrowserStuff;
import org.rickosborne.romance.util.Hyperlink;
import org.rickosborne.romance.util.StringStuff;
import org.rickosborne.romance.util.UrlRank;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.rickosborne.romance.AudiobookStore.DELAY_MS;
import static org.rickosborne.romance.AudiobookStore.MY_LIBRARY_URL;
import static org.rickosborne.romance.db.model.SchemaAttribute.earlierSameYear;
import static org.rickosborne.romance.util.MathStuff.doubleFromDuration;
import static org.rickosborne.romance.util.ModelSetter.setIfEmpty;
import static org.rickosborne.romance.util.StringStuff.ensureToken;
import static org.rickosborne.romance.util.StringStuff.removeToken;
import static org.rickosborne.romance.util.StringStuff.setButNot;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

@RequiredArgsConstructor
public class AudiobookStoreHtml implements ILinkedData {
    @SuppressWarnings("SpellCheckingInspection")
    public static final String AUTHOR_LINK_SELECTOR = ".detailpage a[href*='/authors/']";
    public static final String AUTHOR_NAME_DELIMITER = ", ";
    public static final Duration MAX_AGE = Duration.ofDays(90);
    @SuppressWarnings("SpellCheckingInspection")
    public static final String NARRATOR_LINK_SELECTOR = ".detailpage a[href*='/narrators/']";
    public static final Set<String> ROMANCE_GENRES = Set.of("romance", "erotica",
        "young adult fiction", "fiction", "science fiction", "fantasy");
    @SuppressWarnings("SpellCheckingInspection")
    public static final String SERIES_LINK_SELECTOR = ".detailpage a[href*='/audiobook-series/']";

    protected static List<Hyperlink> collectLinks(final String selector, final HtmlScraper html) {
        final List<Hyperlink> names = new LinkedList<>();
        html.selectMany(selector, link -> Optional
            .ofNullable(Hyperlink.build(link.getOwnText(), link.getAttr("href")))
            .ifPresent(names::add));
        return names;
    }

    protected static Optional<String> collectNames(final String selector, final HtmlScraper html) {
        final List<Hyperlink> links = collectLinks(selector, html);
        if (!links.isEmpty()) {
            final String authorNames = links.stream()
                .sorted(Hyperlink.SORT_BY_TEXT)
                .map(Hyperlink::getText)
                .collect(Collectors.joining(AUTHOR_NAME_DELIMITER));
            if (!authorNames.isBlank()) {
                return Optional.of(authorNames);
            }
        }
        return Optional.empty();
    }

    @Getter
    private final String cacheIdentifier = "abs";
    private final Path cachePath;
    private final JsonCookieStore cookieStore;

    public List<AuthorModel> getAuthorModelsFromBook(@NonNull final URL bookUrl) {
        final HtmlScraper scraper = HtmlScraper.scrape(HtmlScraper.Scrape.builder()
            .url(bookUrl)
            .cachePath(cachePath)
            .delay(DELAY_MS)
            .maxAge(MAX_AGE)
            .build());
        return collectLinks(AUTHOR_LINK_SELECTOR, scraper).stream()
            .map(link -> AuthorModel.builder()
                .name(link.getText())
                .audiobookStoreUrl(link.urlFromHref())
                .build())
            .collect(Collectors.toList());
    }

    public BookModel getBookModelFromBook(@NonNull final URL url) {
        final BookModel book = BookModel.builder().build();
        book.setAudiobookStoreUrl(url);
        return getFromBook(book, url, BookModelLD.values(), BookModelHtml.values());
    }

    public List<BookModel> getBooksForAuthor(final AuthorModel authorModel) {
        if (authorModel == null || authorModel.getAudiobookStoreUrl() == null) {
            return List.of();
        }
        final String authorName = authorModel.getName();
        return getGuestPaginatedBooks(authorModel.getAudiobookStoreUrl()).stream()
            .peek(b -> {
                if (b.getAuthorName() == null) {
                    b.setAuthorName(authorName);
                }
            })
            .collect(Collectors.toList());
    }

    public List<BookModel> getGuestPaginatedBooks(@NonNull final URL url) {
        final List<BookModel> books = new LinkedList<>();
        final HtmlScraper scraper = HtmlScraper.scrape(HtmlScraper.Scrape.builder()
            .url(url)
            .cachePath(cachePath)
            .delay(DELAY_MS)
            .maxAge(MAX_AGE)
            .build());
        scraper.selectMany(".features-books-cat", bookList -> {
            bookList.selectMany(".slide", slide -> {
                final BookModel book = BookModel.build();
                slide.selectMany(".title a", link -> {
                    book.setTitle(link.getAttr("title").trim());
                    book.setAudiobookStoreUrl(StringStuff.urlFromString(link.getAttr("href").trim()));
                });
                slide.selectMany("img.pro-img.categoryImage", img -> {
                    book.setImageUrl(StringStuff.urlFromString(UrlRank.fixup(img.getAttr("src").trim())));
                });
                slide.selectMany(".catimgcont a.trigger[data]", trigger -> {
                    final String data = trigger.getAttr("data");
                    book.setAudiobookStoreSku(trigger.getAttr("dataSKU"));
                    if (data != null) {
                        final String[] parts = data.trim().split("\\|");
                        if (parts.length >= 11) {
                            if (book.getTitle() == null) {
                                book.setTitle(parts[0].trim());
                            }
                            if (book.getNarratorName() == null) {
                                book.setNarratorName(parts[3].trim());
                            }
                            if (book.getAudiobookStoreUrl() == null) {
                                book.setAudiobookStoreUrl(StringStuff.urlFromString(parts[9].trim()));
                            }
                        }
                    }
                });
                books.add(book);
            });
        });
        return books;
    }

    public List<BookModel> getPreorders(final WebDriver browser) {
        browser.navigate().to(MY_LIBRARY_URL);
        Objects.requireNonNull(cookieStore, "Cookie store required to get preorders");
        final WebDriverWait wait = new WebDriverWait(browser, Duration.ofMillis(DELAY_MS));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".recent-orders > table")));
        final WebElement recentOrders = browser.findElement(By.cssSelector(".recent-orders > table"));
        final List<BookModel> preorders = new LinkedList<>();
        for (final WebElement row : recentOrders.findElements(By.cssSelector("tr:has(.library-preorder)"))) {
            final BookModel.BookModelBuilder bookBuilder = BookModel.builder()
                .datePublish(StringStuff.toLocalDateFromMDY(row.findElement(By.cssSelector(".library-preorder")).getText()))
                .audiobookStoreUrl(urlFromString(row.findElement(By.cssSelector(".my-lib-img a:has(img)")).getAttribute("href")))
                .imageUrl(urlFromString(UrlRank.fixup(row.findElement(By.cssSelector(".my-lib-img a img")).getAttribute("src"))));
            for (final WebElement detail : row.findElements(By.cssSelector(".my-lib-details .titledetail-specs > div"))) {
                final String label = detail.findElement(By.cssSelector(".titledetail-label")).getText();
                final String value = detail.findElement(By.cssSelector(".titledetail-value")).getText();
                switch (label) {
                    case "Title:":
                        bookBuilder.title(value);
                        break;
                    case "Author:":
                        bookBuilder.authorName(value);
                        break;
                    case "Reader:":
                        bookBuilder.narratorName(value);
                        break;
                    case "Run time:":
                        final Duration duration = Duration.parse(detail.findElement(By.cssSelector("time[itemprop=timeRequired]")).getAttribute("datetime"));
                        bookBuilder.durationHours(duration.toMinutes() / 60d);
                        break;
                    case "Purchased:":
                        bookBuilder.datePurchase(LocalDate.parse(value, DateTimeFormatter.ofPattern("MM-dd-yyyy")));
                        break;
                }
            }
            preorders.add(bookBuilder.build());
        }
        return preorders;
    }

    public List<BookModel> getWishlist(@NonNull final WebDriver browser) {
        String nextPage = AudiobookStore.WISHLIST_URL;
        browser.navigate().to(nextPage);
        final LinkedList<BookModel> books = new LinkedList<>();
        boolean morePages;
        do {
            morePages = false;
            final WebDriverWait wait = new WebDriverWait(browser, Duration.ofMillis(DELAY_MS));
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("section.features-books-cat")));
            ((JavascriptExecutor) browser).executeScript("return window.stop()");
            final WebElement table = browser.findElement(By.cssSelector("section.features-books-cat"));
            final List<WebElement> figures = table.findElements(By.cssSelector("figure.wishlist"));
            if (figures == null || figures.isEmpty()) {
                break;
            }
            for (final WebElement figure : figures) {
                final BookModel.BookModelBuilder bookBuilder = BookModel.builder();
                final WebElement link = figure.findElement(By.cssSelector("span.title a"));
                bookBuilder
                    .imageUrl(urlFromString(UrlRank.fixup(figure
                        .findElement(By.cssSelector("img.pro-img.categoryImage"))
                        .getAttribute("src"))))
                    .audiobookStoreUrl(urlFromString(link.getAttribute("href")))
                    .title(link.getAttribute("title").trim())
                    .authorName(figure.findElement(By.cssSelector(".titledetail-author.authorName")).getText().trim())
                ;
                books.add(bookBuilder.build());
            }
            ((JavascriptExecutor) browser).executeScript("window.scrollBy(0,document.body.scrollHeight)");
            final List<WebElement> pagination = browser.findElements(By.cssSelector("a#ctl00_contentMain_lstProductsGridPager_ctl00_lnkMobileNext"));
            for (final WebElement link : pagination) {
                if (("Next".equals(link.getText()) || link.getText().isBlank()) && link.isDisplayed() && link.isEnabled()) {
                    final String path = link.getAttribute("href");
                    browser.navigate().to(path);
                    morePages = true;
                    break;
                }
            }
        } while (morePages);
        return books;
    }

    public void headlessSignIn(
        @NonNull final WebDriver browser,
        @NonNull final String email,
        @NonNull final String password
    ) {
        browser.navigate().to(AudiobookStore.SIGN_IN_URL);
        final WebDriverWait wait = new WebDriverWait(browser, Duration.ofMillis(DELAY_MS));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_contentMain_txtEmailLogin")));
        final WebElement emailInput = browser.findElement(By.id("ctl00_contentMain_txtEmailLogin"));
        Objects.requireNonNull(emailInput, "Could not find email input box");
        emailInput.click();
        emailInput.clear();
        emailInput.sendKeys(email);
        final WebElement passwordInput = browser.findElement(By.id("ctl00_contentMain_txtPasswordLogin"));
        Objects.requireNonNull(passwordInput, "Could not find password input box");
        passwordInput.click();
        passwordInput.clear();
        passwordInput.sendKeys(password);
        final WebElement loginButton = browser.findElement(By.id("ctl00_contentMain_btnLogin"));
        Objects.requireNonNull(loginButton, "Could not find login button");
        loginButton.click();
        final WebDriverWait wait2 = new WebDriverWait(browser, Duration.ofMillis(DELAY_MS * 2));
        wait2.until(ExpectedConditions.visibilityOfElementLocated(By.id("hypTopSubMenu")));
    }

    public boolean isRomance(final BookModel book) {
        if (book == null) {
            return false;
        } else if (book.getGenre() == null) {
            return true;
        } else {
            return !book.getGenre().contains("not a romance");
        }
    }

    public void visitGuestPaginated(
        @NonNull final URL url,
        @NonNull final AbsPaginatedVisitor visitor
    ) {
        final List<BookModel> previews = getGuestPaginatedBooks(url);
        previews.stream().map(preview -> {
                if (visitor.onPreviewBook(preview)) {
                    return preview;
                }
                return null;
            })
            .filter(Objects::nonNull)
            .forEach(preview -> {
                final BookModel bookDetails = getBookModelFromBook(preview.getAudiobookStoreUrl());
            });
        final HtmlScraper scraper = HtmlScraper.scrape(HtmlScraper.Scrape.builder()
            .url(url)
            .cachePath(cachePath)
            .delay(DELAY_MS)
            .maxAge(MAX_AGE)
            .build());
    }

    public <T> T withBrowser(final Function<WebDriver, T> block) {
        final WebDriver browser = BrowserStuff.getBrowser();
        try {
            return block.apply(browser);
        } finally {
            browser.quit();
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum AuthorModelHtml implements HtmlData<AuthorModel> {
        AuthorNamesAndLinks((author, html) -> {
            final Hyperlink link;
            final List<Hyperlink> links = collectLinks(AUTHOR_LINK_SELECTOR, html);
            if (links.isEmpty()) {
                return;
            } else if (links.size() == 1) {
                link = links.get(0);
            } else {
                link = links.stream()
                    .filter(l -> StringStuff.fuzzyMatch(l.getText(), author.getName()))
                    .findAny()
                    .orElse(null);
            }
            if (link != null) {
                author.setName(link.getText());
                author.setAudiobookStoreUrl(link.urlFromHref());
            }
        }),
        ;
        private final BiConsumer<AuthorModel, HtmlScraper> setter;
    }

    @Getter
    @RequiredArgsConstructor
    enum AuthorModelLD implements LinkedData<AuthorModel> {
        // AuthorName("/mainEntity/author/name", AuthorModel::setName),
        // TabsLink("/mainEntity/author/url", (a, v) -> a.setAudiobookStoreUrl(urlFromString(v))),
        ;
        private final String ldPath;
        private final BiConsumer<AuthorModel, String> setter;
    }

    @Getter
    @RequiredArgsConstructor
    enum BookModelHtml implements HtmlData<BookModel> {
        AuthorNames((book, html) -> collectNames(AUTHOR_LINK_SELECTOR, html).ifPresent(book::setAuthorName)),
        NarratorNames((book, html) -> collectNames(NARRATOR_LINK_SELECTOR, html).ifPresent(book::setNarratorName)),
        SeriesName((book, html) -> collectNames(SERIES_LINK_SELECTOR, html).ifPresent(book::setSeriesName)),
        ;
        private final BiConsumer<BookModel, HtmlScraper> setter;
    }

    @Getter
    @RequiredArgsConstructor
    enum BookModelLD implements LinkedData<BookModel> {
        // AuthorName("/mainEntity/author/name", BookModel::setAuthorName),
        // NarratorName("/mainEntity/readBy/name", BookModel::setNarratorName),
        DatePublished("/mainEntity/datePublished", (b, d) -> b.setDatePublish(earlierSameYear(b.getDatePublish(), StringStuff.toLocalDate(d)))),
        Duration("/mainEntity/timeRequired", (b, t) -> b.setDurationHours(doubleFromDuration(t))),
        Publisher("/mainEntity/publisher/name", BookModel::setPublisherName),
        PublisherDescription("/mainEntity/description", setIfEmpty(BookModel::setPublisherDescription, BookModel::getPublisherDescription)),
        Image("/mainEntity/image", (b, i) -> b.setImageUrl(urlFromString(UrlRank.fixup(i)))),
        Gtin13("/mainEntity/gtin13", setButNot(BookModel::setIsbn, "null", "")),
        Isbn2("/mainEntity/isbn", setButNot(BookModel::setIsbn, "null", "")),
        Title("/mainEntity/name", setIfEmpty((b, t) -> b.setTitle(BookStuff.cleanTitle(t)), BookModel::getTitle)),
        Sku("/mainEntity/sku", BookModel::setAudiobookStoreSku),
        Genre("/mainEntity/genre", BookModelLD::tagAsRomance),
        Category("/mainEntity/category", BookModelLD::tagAsRomance);

        private static void tagAsRomance(final BookModel model, final String genreCategory) {
            if (genreCategory == null || genreCategory.isBlank()) {
                return;
            }
            if (ROMANCE_GENRES.contains(genreCategory.toLowerCase())) {
                model.setGenre(removeToken("(not a romance)", model.getGenre()));
            } else {
                model.setGenre(ensureToken(" (not a romance)", model.getGenre()));
            }
        }

        private final String ldPath;
        private final BiConsumer<BookModel, String> setter;
    }

    @Value
    public static class BookDetails {
        List<AuthorModel> authors;
        BookModel book;
        List<NarratorModel> narrators;
        SeriesModel series;
    }
}
