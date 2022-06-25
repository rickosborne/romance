package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.List;

public class WatchSchema implements ModelSchema<WatchModel> {
    @Getter
    private final List<WatchAttributes> attributes = List.of(WatchAttributes.values());

    @Override
    public List<String> idValuesFromModel(final WatchModel model) {
        return List.of(model.getAuthorName(), model.getBookTitle());
    }
}
