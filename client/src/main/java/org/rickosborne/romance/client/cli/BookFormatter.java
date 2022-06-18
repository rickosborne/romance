package org.rickosborne.romance.client.cli;

import org.rickosborne.romance.client.client.response.AudiobookStoreSuggestion;
import org.rickosborne.romance.client.client.response.BookInformation;
import org.rickosborne.romance.client.client.response.GoodreadsAutoComplete;

public class BookFormatter {
    public static String asDocTabbed(final BookInformation book) {
        return DocTabbed.fromBookInformation(book).toString();
    }

    public static String asDocTabbed(final GoodreadsAutoComplete ac, final AudiobookStoreSuggestion suggestion) {
        return DocTabbed.fromGoodreadsAutoComplete(ac)
            .merge(DocTabbed.fromAudiobookStoreSuggestion(suggestion))
            .toString();
    }

    public static String asDocTabbed(final GoodreadsAutoComplete ac) {
        return DocTabbed.fromGoodreadsAutoComplete(ac).toString();
    }

    public static String asString(final BookInformation book) {
        return book.toString();
    }
}
