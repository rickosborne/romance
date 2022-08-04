package org.rickosborne.romance;

import lombok.NonNull;
import org.rickosborne.romance.util.StringStuff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rickosborne.romance.util.StreamStuff.firstOrElse;
import static org.rickosborne.romance.util.StringStuff.FILE_NAME_MAX_LENGTH;
import static org.rickosborne.romance.util.StringStuff.noLongerThan;

public class NamingConvention {
    private final Map<String, String> replacements = readReplacements();

    public String fieldNameFromTexts(final String... texts) {
        final String fieldName = Stream.of(texts)
            .filter(StringStuff::nonBlank)
            .map(this::formatText)
            .map(firstOrElse(v -> v, StringStuff::ucFirst))
            .map(v -> replacements.getOrDefault(v, v))
            .collect(Collectors.joining(""));
        return replacements.getOrDefault(fieldName, fieldName);
    }

    public String fileNameFromTexts(@NonNull final String... parts) {
        return fileNameFromTexts(Stream.of(parts));
    }

    public String fileNameFromTexts(@NonNull final Stream<String> parts) {
        return noLongerThan(FILE_NAME_MAX_LENGTH, parts
            .filter(StringStuff::nonBlank)
            .map(String::trim)
            .map(String::toLowerCase)
            .map(s -> s.replaceAll("['\"`]+", "").replaceAll("[^\\da-z]+", "-"))
            .collect(Collectors.joining("-")));
    }

    public String formatText(@NonNull final String text) {
        final String lower = Optional.ofNullable(replacements.get(text))
            .map(Stream::of)
            .orElseGet(() -> Arrays.stream(text.toLowerCase().split("\\s+")))
            .filter(StringStuff::nonBlank)
            .map(firstOrElse(v -> v, StringStuff::ucFirst))
            .map(s -> s.replaceAll("[^a-zA-Z\\d]", ""))
            .map(v -> replacements.getOrDefault(v, v))
            .collect(Collectors.joining(""));
        return replacements.getOrDefault(lower, lower);
    }

    private Map<String, String> readReplacements() {
        final URL csvUrl = getClass().getClassLoader().getResource("replacements.csv");
        if (csvUrl == null) {
            throw new IllegalStateException("Could not locate replacements.csv");
        }
        final Map<String, String> map = new HashMap<>();
        try (
            final InputStream in = getClass().getClassLoader().getResourceAsStream("replacements.csv");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in))
        ) {
            String line;
            while (reader.ready() && ((line = reader.readLine()) != null)) {
                final String[] parts = line.trim().split(",");
                map.put(parts[0], Optional.ofNullable(parts.length > 1 ? parts[1] : null).map(String::trim).orElse(""));
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
