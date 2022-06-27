package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.List;

public class SeriesSchema implements ModelSchema<SeriesModel> {
    @Getter
    private final List<SeriesAttributes> attributes = List.of(SeriesAttributes.values());

    @Override
    public SeriesModel buildModel() {
        return new SeriesModel();
    }

    @Override
    public List<String> idValuesFromModel(final SeriesModel model) {
        return List.of(model.getName());
    }
}
