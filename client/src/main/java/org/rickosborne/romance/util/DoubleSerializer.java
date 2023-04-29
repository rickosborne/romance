package org.rickosborne.romance.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Locale;

import static org.rickosborne.romance.util.MathStuff.intIsh;

public class DoubleSerializer extends JsonSerializer<Double> {
    @Override
    public void serialize(
        final Double value,
        final JsonGenerator jsonGenerator,
        final SerializerProvider serializerProvider
    ) throws IOException {
        if (value == null) {
            jsonGenerator.writeNull();
        } else if (intIsh(value)) {
            jsonGenerator.writeNumber(Math.round(value));
        } else {
            jsonGenerator.writeNumber(String.format(Locale.US, "%.4f", value));
        }
    }
}
