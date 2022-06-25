package org.rickosborne.romance.db.model;

import java.util.List;

public interface ModelSchema<M> {
    <SA extends SchemaAttribute<M, Object>> List<SA> getAttributes();

    List<String> idValuesFromModel(final M model);
}
