package org.rickosborne.romance.client.html;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.rickosborne.romance.AudiobookStore;
import org.rickosborne.romance.client.JsonCookieStore;
import org.rickosborne.romance.client.command.HtmlScraper;
import org.rickosborne.romance.db.DbJsonWriter;
import org.rickosborne.romance.db.model.AuthorModel;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.util.BookStuff;
import org.rickosborne.romance.util.BrowserStuff;
import org.rickosborne.romance.util.StringStuff;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.rickosborne.romance.AudiobookStore.DELAY_MS;
import static org.rickosborne.romance.AudiobookStore.MY_LIBRARY_URL;
import static org.rickosborne.romance.db.model.SchemaAttribute.earlierSameYear;
import static org.rickosborne.romance.util.MathStuff.doubleFromDuration;
import static org.rickosborne.romance.util.ModelSetter.setIfEmpty;
import static org.rickosborne.romance.util.StringStuff.setButNot;
import static org.rickosborne.romance.util.StringStuff.urlFromString;

@RequiredArgsConstructor
public class AudiobookStoreHtml {
    private final Path cachePath;
    private final JsonCookieStore cookieStore;

    public AuthorModel getAuthorModelFromBook(@NonNull final URL bookUrl) {
        return getFromBook(AuthorModel.builder().build(), bookUrl, AuthorModelLD.values());
    }

    public BookModel getBookModel(@NonNull final URL url) {
        final BookModel book = BookModel.builder().build();
        book.setAudiobookStoreUrl(url);
        return getFromBook(book, url, BookModelLD.values());
    }

    protected <M, E extends Enum<E> & LinkedData<M>> M getFromBook(
        @NonNull final M model,
        @NonNull final URL bookUrl,
        @NonNull final E[] ldValues
    ) {
        final HtmlScraper scraper = HtmlScraper.forUrl(bookUrl, cachePath);
        final String ld = scraper.selectOne("script[type=application/ld+json]").getHtml();
        final JsonNode ldNode = DbJsonWriter.readTree(ld);
        for (final E ldItem : ldValues) {
            final JsonNode value = ldNode.at(ldItem.getLdPath());
            if (value != null && value.isTextual()) {
                ldItem.getSetter().accept(model, value.asText());
            }
        }
        return model;
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
                .imageUrl(urlFromString(row.findElement(By.cssSelector(".my-lib-img a img")).getAttribute("src")));
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
                    .imageUrl(urlFromString(figure
                        .findElement(By.cssSelector("img.pro-img.categoryImage"))
                        .getAttribute("src")
                        .replace("-square-400.", "-square-1536.")))
                    .audiobookStoreUrl(urlFromString(link.getAttribute("href")))
                    .title(link.getAttribute("title"))
                    .authorName(figure.findElement(By.cssSelector(".titledetail-author .authorName")).getText())
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
    enum AuthorModelLD implements LinkedData<AuthorModel> {
        AuthorName("/mainEntity/author/name", AuthorModel::setName),
        TabsLink("/mainEntity/author/url", (a, v) -> a.setAudiobookStoreUrl(urlFromString(v))),
        ;
        private final String ldPath;
        private final BiConsumer<AuthorModel, String> setter;
    }

    @Getter
    @RequiredArgsConstructor
    enum BookModelLD implements LinkedData<BookModel> {
        AuthorName("/mainEntity/author/name", BookModel::setAuthorName),
        NarratorName("/mainEntity/readBy/name", BookModel::setNarratorName),
        DatePublished("/mainEntity/datePublished", (b, d) -> b.setDatePublish(earlierSameYear(b.getDatePublish(), StringStuff.toLocalDate(d)))),
        Duration("/mainEntity/timeRequired", (b, t) -> b.setDurationHours(doubleFromDuration(t))),
        Publisher("/mainEntity/publisher/name", BookModel::setPublisherName),
        PublisherDescription("/mainEntity/description", setIfEmpty(BookModel::setPublisherDescription, BookModel::getPublisherDescription)),
        Image("/mainEntity/image", (b, i) -> b.setImageUrl(urlFromString(i.replace("-square-400", "-square-1536")))),
        Gtin13("/mainEntity/gtin13", setButNot(BookModel::setIsbn, "null", "")),
        Isbn2("/mainEntity/isbn", setButNot(BookModel::setIsbn, "null", "")),
        Title("/mainEntity/name", setIfEmpty((b, t) -> b.setTitle(BookStuff.cleanTitle(t)), BookModel::getTitle)),
        Sku("/mainEntity/sku", BookModel::setAudiobookStoreSku),
        ;
        private final String ldPath;
        private final BiConsumer<BookModel, String> setter;
    }

    interface LinkedData<M> {
        String getLdPath();

        BiConsumer<M, String> getSetter();
    }
}
