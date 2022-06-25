package org.rickosborne.romance.db;

public interface ModelStore<M> {
    default Diff<M> diffFromCache(final M model) {
        final M cached = findLikeFromCache(model);
        return diffModels(cached, model);
    }

    default Diff<M> diffModels(final M before, final M after) {
        return new SchemaDiff().diffModels(before, after);
    }

    M findById(final String id);

    M findByIdFromCache(final String id);

    default M findLike(final M model) {
        if (model == null) {
            return null;
        }
        final String id = idFromModel(model);
        if (id == null) {
            throw new NullPointerException("Model has no id: " + model.getClass().getSimpleName());
        }
        return findById(id);
    }

    default M findLikeFromCache(final M model) {
        if (model == null) {
            return null;
        }
        final String id = idFromModel(model);
        if (id == null) {
            throw new NullPointerException("Model has no id: " + model.getClass().getSimpleName());
        }
        return findByIdFromCache(id);
    }

    DbModel getDbModel();

    Class<M> getModelType();

    String idFromModel(final M model);

    M save(final M model);

    default M saveIfChanged(final M model) {
        final Diff<M> diff = diffFromCache(model);
        if (diff.hasChanged()) {
            final String id = idFromModel(model);
            System.out.println("~~~ " + getDbModel().getTypeName() + "/" + id);
            System.out.println(diff.asDiffLines());
            return save(model);
        }
        return model;
    }
}
