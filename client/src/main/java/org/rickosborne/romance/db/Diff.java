package org.rickosborne.romance.db;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.rickosborne.romance.db.model.SchemaAttribute;

import java.util.List;

import static org.rickosborne.romance.util.StringStuff.CRLF;

@Builder
public record Diff<M>(M after, M before, List<AttributeDiff<M, ?>> changes) {
    public String asDiffLines() {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        final List<AttributeDiff<M, ?>> printable = changes.stream().filter(c -> c.operation != Operation.Keep).toList();
        if (printable.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final AttributeDiff<M, ?> change : printable) {
            final String lines = change.asDiffLines();
            if (lines != null && !lines.isBlank()) {
                sb.append(lines);
            }
        }
        return sb.toString();
    }

    public boolean hasChanged() {
        return changes
            .stream()
            .anyMatch(c -> c.operation != Operation.Keep);
    }

    @RequiredArgsConstructor
    @Getter
    public enum Operation {
        Add("+ "),
        Change("~ "),
        Delete("- "),
        Keep("  "),
        ;
        private final String prefix;
    }

    @Builder
    public record AttributeDiff<M, A>(
        M afterModel,
        A afterValue,
        SchemaAttribute<M, A> attribute,
        Class<A> attributeType,
        M beforeModel,
        A beforeValue,
        Class<M> modelType,
        Operation operation
    ) {
        public String asDiffLines() {
            if (operation == Operation.Keep) {
                return null;
            }
            final StringBuilder sb = new StringBuilder();
            if (operation == Operation.Delete || operation == Operation.Change) {
                sb.append(Operation.Delete.getPrefix())
                    .append(attribute.getAttributeName())
                    .append(": ")
                    .append(beforeValue)
                    .append(CRLF);
            }
            if (operation == Operation.Add || operation == Operation.Change) {
                sb.append(Operation.Add.getPrefix())
                    .append(attribute.getAttributeName())
                    .append(": ")
                    .append(afterValue)
                    .append(CRLF);
            }
            return sb.toString();
        }
    }
}
