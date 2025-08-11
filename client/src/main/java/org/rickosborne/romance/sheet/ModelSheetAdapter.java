package org.rickosborne.romance.sheet;

import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.Diff;
import org.rickosborne.romance.db.SchemaDiff;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.util.ModelSetter;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ModelSheetAdapter<M> extends ModelSetter<M> {

    default M buildModel() {
        return getDbModel().buildModel();
    }

    default Map<String, String> changesForDiff(
        final Diff<M> diff,
        final Predicate<SchemaAttribute<M, ?>> attributePredicate,
        final Map<SchemaAttribute<M, ?>, ModelSetter<M>> sheetFields
    ) {
        final Map<String, String> map = new HashMap<>();
        for (final Diff.AttributeDiff<M, ?> change : diff.changes()) {
            if (change.operation() != Diff.Operation.Add && change.operation() != Diff.Operation.Change) {
                continue;
            }
            if (attributePredicate != null && !attributePredicate.test(change.attribute())) {
                continue;
            }
            final ModelSetter<M> setter = sheetFields.get(change.attribute());
            final Object afterValue = change.afterValue();
            if (setter == null || afterValue == null) {
                continue;
            }
            final String fieldName = ((Enum<?>) setter).name();
            System.out.printf("%s: %s => %s%n", fieldName, change.beforeValue(), afterValue);
            map.put(fieldName, afterValue.toString());
        }
        return map;
    }

    String fileNameForModel(@NonNull final M model, @NonNull final NamingConvention namingConvention);

    default String fileNameForType(@NonNull final NamingConvention namingConvention) {
        return namingConvention.fileNameFromTexts(getDbModel().getTypeName());
    }

    default Map<String, String> findChangesToSheet(@NonNull final M sheetRecord, @NonNull final M existingRecord) {
        return findChangesToSheet(sheetRecord, existingRecord, null);
    }

    default Map<String, String> findChangesToSheet(
        @NonNull final M sheetRecord,
        @NonNull final M existingRecord,
        final Predicate<SchemaAttribute<M, ?>> attributePredicate
    ) {
        final Map<SchemaAttribute<M, ?>, ModelSetter<M>> sheetFields = getSheetFields();
        return changesForDiff(new SchemaDiff().diffModels(sheetRecord, existingRecord), attributePredicate, sheetFields);
    }

    DbModel getDbModel();

    default Class<M> getModelType() {
        return getDbModel().getModelType();
    }

    Map<String, BiConsumer<M, Object>> getSetters();

    Map<SchemaAttribute<M, ?>, ModelSetter<M>> getSheetFields();

    default void putSetters(
        @NonNull final Map<String, BiConsumer<M, Object>> setters,
        @NonNull final Stream<SchemaAttribute<M, Object>> attributes
    ) {
        attributes.forEach(attr -> {
            final Class<?> attrType = attr.getAttributeType();
            final BiConsumer<M, Object> setter;
            if (attrType == String.class) {
                setter = stringSetter(attr::setAttribute);
            } else if (attrType == LocalDate.class) {
                setter = localDateSetter(attr::setAttribute);
            } else if (attrType == Integer.class) {
                setter = intSetter(attr::setAttribute);
            } else if (attrType == Double.class) {
                setter = doubleSetter(attr::setAttribute);
            } else if (attrType == URL.class) {
                setter = urlSetter(attr::setAttribute);
            } else {
                throw new IllegalArgumentException("No setter for attribute type: " + attr.getModelType() + "#" + attr.getAttributeName() + "<" + attrType.getSimpleName() + ">");
            }
            setters.put(attr.getAttributeName(), setter);
        });
    }

    default void setKeyValue(
        @NonNull final M model,
        @NonNull final String key,
        final Object value
    ) {
        final BiConsumer<M, Object> setter = getSetters().get(key);
        if (setter == null) {
            throw new IllegalArgumentException(String.format("Unknown key for %s: %s", getDbModel().name(), key));
        }
        setter.accept(model, value);
    }


    default M withNewModel(@NonNull final BiConsumer<ModelSheetAdapter<M>, M> block) {
        final M model = buildModel();
        block.accept(this, model);
        return model;
    }
}
