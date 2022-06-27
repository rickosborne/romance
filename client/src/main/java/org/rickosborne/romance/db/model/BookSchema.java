package org.rickosborne.romance.db.model;

import lombok.Getter;
import org.rickosborne.romance.util.StringStuff;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BookSchema implements ModelSchema<BookModel> {
    @Getter
    private final List<BookAttributes> attributes = List.of(BookAttributes.values());

    @Override
    public BookModel buildModel() {
        return BookModel.builder().build();
    }

    @Override
    public List<String> idValuesFromModel(final BookModel model) {
        if (model == null || model.getDatePublish() == null) {
            return Collections.emptyList();
        }
        return Stream.of(
                model.getAuthorName(),
                String.valueOf(model.getDatePublish().getYear()),
                model.getTitle()
            )
            .filter(StringStuff::nonBlank)
            .collect(Collectors.toList());
    }
}
