package org.rickosborne.romance.sheet;

import lombok.NonNull;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.DbModel;
import org.rickosborne.romance.db.Diff;
import org.rickosborne.romance.db.SchemaDiff;
import org.rickosborne.romance.db.model.SchemaAttribute;
import org.rickosborne.romance.util.ModelSetter;
import org.rickosborne.romance.util.Pair;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ModelSheetAdapter<M> extends ModelSetter<M> {

    default M buildModel() {
        return getDbModel().buildModel();
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
        return new SchemaDiff().diffModels(sheetRecord, existingRecord).getChanges().stream()
            .filter(c -> c.getOperation() == Diff.Operation.Add || c.getOperation() == Diff.Operation.Change)
            .filter(c -> attributePredicate == null || attributePredicate.test(c.getAttribute()))
            .map(c -> Pair.build(c, sheetFields.get(c.getAttribute())))
            .filter(p -> p.hasRight() && p.getLeft().getAfterValue() != null)
            .peek(p -> System.out.printf("%s: %s => %s%n", ((Enum<?>) p.getRight()).name(), p.getLeft().getBeforeValue(), p.getLeft().getAfterValue()))
            .collect(Collectors.toMap(p -> ((Enum<?>) p.getRight()).name(), p -> p.getLeft().getAfterValue().toString()));
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
