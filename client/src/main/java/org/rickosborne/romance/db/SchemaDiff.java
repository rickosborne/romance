package org.rickosborne.romance.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.model.ModelSchemas;
import org.rickosborne.romance.db.model.SchemaAttribute;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class SchemaDiff {
    private final Map<Class<?>, ModelSchema<?>> schemas = new HashMap<>();

    private <M, A> void diffAttribute(
        final M beforeModel,
        final M afterModel,
        final SchemaAttribute<M, A> attribute,
        final Consumer<Diff.AttributeDiff<M, A>> diffAdder
    ) {
        final Diff.AttributeDiff.AttributeDiffBuilder<M, A> builder = Diff.AttributeDiff.<M, A>builder()
            .beforeModel(beforeModel)
            .afterModel(afterModel)
            .modelType(attribute.getModelType())
            .attributeType(attribute.getAttributeType())
            .attribute(attribute);
        if (beforeModel == null && afterModel == null) {
            builder.operation(Diff.Operation.Keep);
        } else if (beforeModel == null) {
            builder.operation(Diff.Operation.Add)
                .afterValue(attribute.getAttribute(afterModel));
        } else if (afterModel == null) {
            builder.operation(Diff.Operation.Delete)
                .beforeValue(attribute.getAttribute(beforeModel));
        } else {
            final A beforeValue = attribute.getAttribute(beforeModel);
            final A afterValue = attribute.getAttribute(afterModel);
            builder
                .beforeValue(beforeValue)
                .afterValue(afterValue);
            if (Objects.equals(beforeValue, afterValue)) {
                builder.operation(Diff.Operation.Keep);
            } else {
                builder.operation(Diff.Operation.Change);
            }
        }
        final Diff.AttributeDiff<M, A> attributeDiff = builder.build();
        diffAdder.accept(attributeDiff);
    }

    public <M> Diff<M> diffModels(
        final M before,
        final M after
    ) {
        final M one = Optional.ofNullable(before).orElse(after);
        if (one == null) {
            return null;
        }
        @SuppressWarnings("unchecked") final Class<M> type = (Class<M>) one.getClass();
        final ModelSchema<M> modelSchema = getSchemasForModel(type);
        final List<Diff.AttributeDiff<M, ?>> changes = new LinkedList<>();
        for (final SchemaAttribute<M, Object> attribute : modelSchema.getAttributes()) {
            diffAttribute(before, after, attribute, changes::add);
        }
        return new Diff<>(before, after, changes);
    }

    private <M, S extends ModelSchema<M>> S getSchemasForModel(@NonNull final Class<M> modelClass) {
        @SuppressWarnings("unchecked") final S result = (S) schemas.computeIfAbsent(modelClass, c -> {
            for (final ModelSchemas ms : ModelSchemas.values()) {
                if (ms.getModelClass().equals(c)) {
                    return ms.<M, S>getModelSchema();
                }
            }
            throw new IllegalArgumentException("Unknown model class: " + c.getSimpleName());
        });
        return result;
    }

}
