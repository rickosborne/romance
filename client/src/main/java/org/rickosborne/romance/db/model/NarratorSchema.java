package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.List;

public class NarratorSchema implements ModelSchema<NarratorModel> {
    @Getter
    private final List<NarratorAttributes> attributes = List.of(NarratorAttributes.values());

    @Override
    public NarratorModel buildModel() {
        return new NarratorModel();
    }

    @Override
    public List<String> idValuesFromModel(final NarratorModel model) {
        return List.of(model.getName());
    }
}
