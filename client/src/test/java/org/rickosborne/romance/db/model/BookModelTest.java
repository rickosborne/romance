package org.rickosborne.romance.db.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookModelTest {
    @Test
    void hashCodeAccountsForSpellingInconsistency() {
        assertEquals(
            BookModel.builder().authorName("A B Example").title("some title").build().hashCode(),
            BookModel.builder().authorName("a b Example").title("Some   Title").build().hashCode()
        );
    }

    @Test
    void setNarratorName() {
        final BookModel book = BookModel.build();
        book.setNarratorName("Bailey Carr & Abby Craden");
        assertEquals("Abby Craden, Bailey Carr", book.getNarratorName());
        book.setNarratorName("Anastasia Watley; Abby Craden");
        assertEquals("Abby Craden, Anastasia Watley", book.getNarratorName());
        book.setNarratorName("Bailey Carr");
        assertEquals("Abby Craden, Anastasia Watley", book.getNarratorName());
    }
}
