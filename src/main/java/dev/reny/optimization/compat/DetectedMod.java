package dev.reny.optimization.compat;

import java.util.Locale;

/** Immutable loaded-mod record used by compatibility decisions. */
public final class DetectedMod {

    private final String id;
    private final String normalizedId;
    private final String name;
    private final String version;
    private final boolean synthetic;

    public DetectedMod(String id, String name, String version, boolean synthetic) {
        this.id = valueOrUnknown(id);
        this.normalizedId = normalizeId(this.id);
        this.name = valueOrUnknown(name);
        this.version = valueOrUnknown(version);
        this.synthetic = synthetic;
    }

    public String getId() {
        return id;
    }

    public String getNormalizedId() {
        return normalizedId;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public boolean isVersionKnown() {
        return !"unknown".equalsIgnoreCase(version) && !"?".equals(version);
    }

    static String normalizeId(String value) {
        return valueOrUnknown(value).trim()
            .toLowerCase(Locale.ROOT);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.trim()
            .isEmpty() ? "unknown" : value.trim();
    }
}
