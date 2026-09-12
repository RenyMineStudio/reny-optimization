package dev.reny.optimization.benchmark;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable scenario-specific metadata that cannot be reliably discovered from the JVM alone. */
public final class BenchmarkContext {

    private final String shaderName;
    private final String shaderVersion;
    private final String shaderPreset;
    private final Integer displayWidth;
    private final Integer displayHeight;
    private final Integer renderDistanceChunks;
    private final Boolean vsync;
    private final Integer fpsCap;
    private final String worldSeed;
    private final String worldDescriptor;
    private final String playerRoute;
    private final String timeWeather;
    private final String configHash;
    private final Map<String, String> extras;

    private BenchmarkContext(Builder builder) {
        shaderName = builder.shaderName;
        shaderVersion = builder.shaderVersion;
        shaderPreset = builder.shaderPreset;
        displayWidth = builder.displayWidth;
        displayHeight = builder.displayHeight;
        renderDistanceChunks = builder.renderDistanceChunks;
        vsync = builder.vsync;
        fpsCap = builder.fpsCap;
        worldSeed = builder.worldSeed;
        worldDescriptor = builder.worldDescriptor;
        playerRoute = builder.playerRoute;
        timeWeather = builder.timeWeather;
        configHash = builder.configHash;
        extras = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.extras));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getShaderName() {
        return shaderName;
    }

    public String getShaderVersion() {
        return shaderVersion;
    }

    public String getShaderPreset() {
        return shaderPreset;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    public Integer getDisplayHeight() {
        return displayHeight;
    }

    public Integer getRenderDistanceChunks() {
        return renderDistanceChunks;
    }

    public Boolean getVsync() {
        return vsync;
    }

    public Integer getFpsCap() {
        return fpsCap;
    }

    public String getWorldSeed() {
        return worldSeed;
    }

    public String getWorldDescriptor() {
        return worldDescriptor;
    }

    public String getPlayerRoute() {
        return playerRoute;
    }

    public String getTimeWeather() {
        return timeWeather;
    }

    public String getConfigHash() {
        return configHash;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public static final class Builder {

        private String shaderName = "none";
        private String shaderVersion = "none";
        private String shaderPreset = "none";
        private Integer displayWidth;
        private Integer displayHeight;
        private Integer renderDistanceChunks;
        private Boolean vsync;
        private Integer fpsCap;
        private String worldSeed = "unknown";
        private String worldDescriptor = "unknown";
        private String playerRoute = "unknown";
        private String timeWeather = "unknown";
        private String configHash = "unknown";
        private final Map<String, String> extras = new LinkedHashMap<String, String>();

        private Builder() {}

        public Builder shader(String name, String version, String preset) {
            shaderName = valueOrUnknown(name);
            shaderVersion = valueOrUnknown(version);
            shaderPreset = valueOrUnknown(preset);
            return this;
        }

        public Builder display(int width, int height, int renderDistanceChunks, boolean vsync, int fpsCap) {
            displayWidth = width;
            displayHeight = height;
            this.renderDistanceChunks = renderDistanceChunks;
            this.vsync = vsync;
            this.fpsCap = fpsCap;
            return this;
        }

        public Builder world(String seed, String descriptor, String route, String timeWeather) {
            worldSeed = valueOrUnknown(seed);
            worldDescriptor = valueOrUnknown(descriptor);
            playerRoute = valueOrUnknown(route);
            this.timeWeather = valueOrUnknown(timeWeather);
            return this;
        }

        public Builder configHash(String configHash) {
            this.configHash = valueOrUnknown(configHash);
            return this;
        }

        public Builder extra(String key, String value) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("extra key must not be blank");
            }
            extras.put(key, valueOrUnknown(value));
            return this;
        }

        public BenchmarkContext build() {
            return new BenchmarkContext(this);
        }

        private static String valueOrUnknown(String value) {
            return value == null || value.trim().isEmpty() ? "unknown" : value;
        }
    }
}
