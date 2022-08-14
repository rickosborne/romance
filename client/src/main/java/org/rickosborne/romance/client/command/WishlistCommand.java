package org.rickosborne.romance.client.command;

import lombok.extern.java.Log;
import org.rickosborne.romance.client.html.AudiobookStoreHtml;
import org.rickosborne.romance.db.model.BookModel;
import org.rickosborne.romance.db.sheet.SheetStore;
import org.rickosborne.romance.util.SheetStuff;
import picocli.CommandLine;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "wishlist",
    description = "Fetch your wishlist from AudiobookStore"
)
@Log
public class WishlistCommand extends ASheetCommand {
    @Override
    public Integer doWithSheets() {
        final String absUsername = getAuth().getAbsUsername();
        final String absPassword = getAuth().getAbsPassword();
        Objects.requireNonNull(absUsername, "TABS username is required.");
        Objects.requireNonNull(absPassword, "TABS password is required");
        final SheetStore<BookModel> sheetStore = getSheetStoreFactory().buildSheetStore(BookModel.class);
        final AudiobookStoreHtml storeHtml = getAudiobookStoreHtml();
        final List<BookModel> wishlist = storeHtml.withBrowser(browser -> {
            storeHtml.headlessSignIn(browser, absUsername, absPassword);
            return storeHtml.getWishlist(browser);
        });
        final Set<String> bookTitlesAuthors = sheetStore.getRecords().stream()
            .map(SheetStuff.Indexed::getModel)
            .map(this::bookTitleAuthor)
            .collect(Collectors.toSet());
        wishlist.removeIf(book -> bookTitlesAuthors.contains(bookTitleAuthor(book)));
        if (wishlist.isEmpty()) {
            System.out.println("(no wishlist)");
            return 0;
        }
        for (final BookModel book : wishlist) {
            final DocTabbed docTabbed = DocTabbed.fromBookModel(book);
            System.out.println(docTabbed.toString());
        }
        return 0;
    }

    private String bookTitleAuthor(final BookModel bookModel) {
        return bookModel.getAuthorName() + "\t" + bookModel.getTitle();
    }
}
