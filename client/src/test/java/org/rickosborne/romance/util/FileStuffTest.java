package org.rickosborne.romance.util;

import org.junit.jupiter.api.Test;
import org.rickosborne.romance.db.model.BookModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.rickosborne.romance.util.FileStuff.interpretFileParts;

class FileStuffTest {

    @Test
    void interpretFileParts_MisleadingTitle() {
        assertEquals(new FileStuff.TrackAndCount(null, 2), interpretFileParts("Foo part 1 of 2.mp4", BookModel.builder().title("Foo part 1 of").build()));
    }

    @Test
    void interpretFileParts_NumberTooBig() {
        assertNull(interpretFileParts("Foo 27", null));
    }

    @Test
    void interpretFileParts_PartOnly() {
        assertEquals(new FileStuff.TrackAndCount(null, 1), interpretFileParts("Foo part 1 (2009) 7.mp4", null));
    }

    @Test
    void interpretFileParts_TrackAndCountOfBrackets() {
        assertEquals(new FileStuff.TrackAndCount(3, 2), interpretFileParts("Foo part 2 of 3", null));
    }

    @Test
    void interpretFileParts_TrackAndCountOfPart() {
        assertEquals(new FileStuff.TrackAndCount(3, 2), interpretFileParts("Foo part 2 of 3", null));
    }

    @Test
    void interpretFileParts_TrackOnly() {
        assertEquals(new FileStuff.TrackAndCount(null, 2), interpretFileParts("Foo 2", null));
    }
}
