package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.List;

public class TagSchema implements ModelSchema<TagModel> {
    @Getter
    private final List<TagAttributes> attributes = List.of(TagAttributes.values());

    @Override
    public List<String> idValuesFromModel(final TagModel model) {
        return List.of(model.getName());
    }
}
