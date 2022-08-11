package org.rickosborne.romance.db.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class AuthorSchema implements ModelSchema<AuthorModel> {
    @Getter
    private final List<AuthorAttributes> attributes = List.of(AuthorAttributes.values());

    @Override
    public AuthorModel buildModel() {
        return AuthorModel.build();
    }

    @Override
    public List<String> idValuesFromModel(final AuthorModel model) {
        if (model == null) {
            return Collections.emptyList();
        }
        return List.of(model.getName());
    }
}
