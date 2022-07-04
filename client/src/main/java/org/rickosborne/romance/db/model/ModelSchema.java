package org.rickosborne.romance.db.model;

import org.rickosborne.romance.db.Importable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ModelSchema<M> {
    M buildModel();

    <SA extends SchemaAttribute<M, Object>> List<SA> getAttributes();

    List<String> idValuesFromModel(final M model);

    default M mergeModels(final M before, final M after) {
        if (before == null) {
            return after;
        } else if (after == null) {
            return before;
        }
        final M result = buildModel();
        for (final SchemaAttribute<M, Object> attribute : getAttributes()) {
            final Object beforeValue = attribute.getAttribute(before);
            final Object afterValue = attribute.getAttribute(after);
            // final Object resultValue = beforeValue == null ? afterValue : beforeValue;
            final Object resultValue = attribute.chooseAttributeValue(beforeValue, afterValue);
            if (resultValue == null) {
                continue;
            }
            final Class<?> attributeType = attribute.getAttributeType();
            if (attributeType == Set.class && beforeValue != null) {
                @SuppressWarnings("unchecked") final Set<Object> beforeSet = (Set<Object>) beforeValue;
                @SuppressWarnings("unchecked") final Set<Object> afterSet = (Set<Object>) afterValue;
                @SuppressWarnings("unchecked") final Set<Object> resultSet = (Set<Object>) attribute.getAttribute(result);
                resultSet.addAll(beforeSet);
                resultSet.addAll(afterSet);
            } else if (attributeType == Map.class && beforeValue != null) {
                @SuppressWarnings("unchecked") final Map<Object, Object> beforeMap = (Map<Object, Object>) beforeValue;
                @SuppressWarnings("unchecked") final Map<Object, Object> afterMap = (Map<Object, Object>) afterValue;
                @SuppressWarnings("unchecked") final Map<Object, Object> resultMap = (Map<Object, Object>) attribute.getAttribute(result);
                resultMap.putAll(beforeMap);
                resultMap.putAll(afterMap);
            } else if (Importable.class.isAssignableFrom(attributeType) && beforeValue != null) {
                @SuppressWarnings("unchecked") final Importable<Object> beforeImportable = (Importable<Object>) beforeValue;
                @SuppressWarnings("unchecked") final Importable<Object> afterImportable = (Importable<Object>) afterValue;
                @SuppressWarnings("unchecked") final Importable<Object> resultImportable = (Importable<Object>) attribute.getAttribute(result);
                resultImportable.importFrom(beforeImportable);
                resultImportable.importFrom(afterImportable);
            } else {
                attribute.setAttribute(result, resultValue);
            }
        }
        return result;
    }
}
