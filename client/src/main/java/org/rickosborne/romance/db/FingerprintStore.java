package org.rickosborne.romance.db;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.NamingConvention;
import org.rickosborne.romance.db.model.ModelSchema;
import org.rickosborne.romance.db.model.ModelSchemas;
import org.rickosborne.romance.db.model.SchemaAttribute;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FingerprintStore<M> {
    private static final Path PRINTS_DIR = Path.of("book-data", "fingerprint");
    private final Path jsonPath;
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final Map<String, String> fingerprints = loadFingerprints();
    private final ModelSchema<M> modelSchema;
    private final Class<M> modelType;
    private final NamingConvention namingConvention = new NamingConvention();

    public FingerprintStore(
        final Class<M> modelType,
        final String bucket
    ) {
        this.modelType = modelType;
        this.modelSchema = ModelSchemas.schemaForModelType(modelType);
        this.jsonPath = PRINTS_DIR.resolve(bucket + "-" + modelType.getSimpleName() + ".json");
    }

    @SneakyThrows
    public boolean hasChanged(final M model) {
        final String id = modelId(model);
        final String expected = getFingerprints().get(id);
        if (expected == null) {
            return true;
        }
        final String actual = takeFingerprint(model);
        return !expected.equals(actual);
    }

    private Map<String, String> loadFingerprints() {
        if (PRINTS_DIR.toFile().mkdirs()) {
            log.info("mkdir -p {}", PRINTS_DIR);
        }
        assert jsonPath != null;
        if (jsonPath.toFile().isFile()) {
            @SuppressWarnings("unchecked") final Map<String, String> fingerPrints = DbJsonWriter.readFile(jsonPath.toFile(), HashMap.class);
            return fingerPrints;
        }
        return new HashMap<>();
    }

    private String modelId(final M model) {
        return namingConvention.fileNameFromTexts(modelSchema.idValuesFromModel(model).stream());
    }

    @SneakyThrows
    private String takeFingerprint(final M model) {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (final SchemaAttribute<M, Object> attribute : modelSchema.getAttributes()) {
            final String name = attribute.getAttributeName();
            digest.update(name.getBytes());
            final Object value = attribute.getAttribute(model);
            if (value != null) {
                digest.update(value.toString().getBytes());
            }
        }
        final BigInteger bigInt = new BigInteger(1, digest.digest());
        final StringBuilder sb = new StringBuilder(bigInt.toString(16));
        while (sb.length() < 64) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

    @SneakyThrows
    public void updateFingerprint(final M model) {
        final String id = modelId(model);
        final String fingerprint = takeFingerprint(model);
        final Map<String, String> fingerprints = getFingerprints();
        final String existing = fingerprints.get(id);
        if (fingerprint.equals(existing)) {
            return;
        }
        fingerprints.put(id, fingerprint);
        DbJsonWriter.getJsonWriter().writeValue(jsonPath.toFile(), fingerprints);
    }
}
