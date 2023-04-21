package org.rickosborne.romance.util;

import lombok.NonNull;
import org.rickosborne.romance.db.model.BookModel;

import java.util.Comparator;
import java.util.Optional;

import static org.rickosborne.romance.util.StringStuff.fuzzyMatch;
import static org.rickosborne.romance.util.StringStuff.nonBlank;

public class MainCharRank {
    public static final int AGE_MISSING = 0;
    public static final int AGE_PRESENT = 3;
    public static final int GENDER_MISSING = 0;
    public static final int GENDER_PRESENT = 4;
    public static final int JOB_MISSING = 0;
    public static final int JOB_PRESENT = 2;
    public static final int NAME_LIKE_AUTHOR = -4;
    public static final int NAME_MISSING = -20;
    public static final int NAME_NOT_LIKE_AUTHOR = 0;
    public static final int NAME_PRESENT = 6;
    public static final int PRONOUNS_MISSING = 0;
    public static final int PRONOUNS_PRESENT = 4;

    public static Comparator<BookModel.MainChar> compareWithAuthor(
        final String authorName
    ) {
        return (a, b) -> {
            if (a == b) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            final int ra = rank(a, authorName);
            final int rb = rank(b, authorName);
            if (ra == rb) {
                return Optional.ofNullable(a.getName())
                    .orElse("")
                    .compareTo(Optional.ofNullable(b.getName()).orElse(""));
            }
            return rb - ra;  // reversed
        };
    }

    public static int rank(
        @NonNull final BookModel.MainChar mc
    ) {
        return rank(mc, null);
    }

    public static int rank(
        @NonNull final BookModel.MainChar mc,
        final String authorName
    ) {
        return (nonBlank(mc.getName()) ? NAME_PRESENT : NAME_MISSING)
            + (nonBlank(mc.getAge()) ? AGE_PRESENT : AGE_MISSING)
            + (nonBlank(mc.getGender()) ? GENDER_PRESENT : GENDER_MISSING)
            + (nonBlank(mc.getProfession()) ? JOB_PRESENT : JOB_MISSING)
            + (nonBlank(mc.getPronouns()) ? PRONOUNS_PRESENT : PRONOUNS_MISSING)
            + (authorName != null && fuzzyMatch(mc.getName(), authorName) ? NAME_LIKE_AUTHOR : NAME_NOT_LIKE_AUTHOR);
    }
}
