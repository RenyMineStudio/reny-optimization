package dev.reny.optimization.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** Immutable compatibility-relevant view of one Minecraft process. */
public final class EnvironmentSnapshot {

    private final EnvironmentSide side;
    private final String javaVendor;
    private final String javaVersion;
    private final String osName;
    private final String osVersion;
    private final String osArch;
    private final RendererPath rendererPath;
    private final ShaderPath shaderPath;
    private final SortedMap<String, DetectedMod> mods;
    private final Set<EnvironmentCapability> capabilities;

    private EnvironmentSnapshot(Builder builder) {
        side = builder.side;
        javaVendor = valueOrUnknown(builder.javaVendor);
        javaVersion = valueOrUnknown(builder.javaVersion);
        osName = valueOrUnknown(builder.osName);
        osVersion = valueOrUnknown(builder.osVersion);
        osArch = valueOrUnknown(builder.osArch);
        rendererPath = builder.rendererPath;
        shaderPath = builder.shaderPath;
        mods = Collections.unmodifiableSortedMap(new TreeMap<String, DetectedMod>(builder.mods));
        capabilities = Collections.unmodifiableSet(EnumSet.copyOf(builder.capabilities));
    }

    public static Builder builder() {
        return new Builder();
    }

    public EnvironmentSide getSide() {
        return side;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getOsArch() {
        return osArch;
    }

    public RendererPath getRendererPath() {
        return rendererPath;
    }

    public ShaderPath getShaderPath() {
        return shaderPath;
    }

    public SortedMap<String, DetectedMod> getMods() {
        return mods;
    }

    public Set<EnvironmentCapability> getCapabilities() {
        return capabilities;
    }

    public boolean hasCapability(EnvironmentCapability capability) {
        return capabilities.contains(capability);
    }

    public DetectedMod findMod(KnownMod knownMod) {
        for (String alias : knownMod.getAliases()) {
            DetectedMod detected = mods.get(DetectedMod.normalizeId(alias));
            if (detected != null) {
                return detected;
            }
        }
        return null;
    }

    public boolean hasMod(KnownMod knownMod) {
        return findMod(knownMod) != null;
    }

    public List<String> toDiagnosticLines() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(
            "side=" + side + " java=" + javaVendor + " " + javaVersion + " os=" + osName + " " + osVersion + " "
                + osArch + " renderer=" + rendererPath + " shaders=" + shaderPath + " capabilities=" + capabilities);
        for (DetectedMod mod : mods.values()) {
            lines.add(
                "mod " + mod.getId() + " version=" + mod.getVersion() + (mod.isSynthetic() ? " synthetic" : ""));
        }
        return Collections.unmodifiableList(lines);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    public static final class Builder {

        private EnvironmentSide side = EnvironmentSide.UNKNOWN;
        private String javaVendor = "unknown";
        private String javaVersion = "unknown";
        private String osName = "unknown";
        private String osVersion = "unknown";
        private String osArch = "unknown";
        private RendererPath rendererPath = RendererPath.UNKNOWN;
        private ShaderPath shaderPath = ShaderPath.UNKNOWN;
        private final SortedMap<String, DetectedMod> mods = new TreeMap<String, DetectedMod>();
        private final EnumSet<EnvironmentCapability> capabilities = EnumSet.noneOf(EnvironmentCapability.class);

        private Builder() {}

        public Builder side(EnvironmentSide value) {
            if (value == null) {
                throw new IllegalArgumentException("side must not be null");
            }
            side = value;
            return this;
        }

        public Builder javaInfo(String vendor, String version) {
            javaVendor = valueOrUnknown(vendor);
            javaVersion = valueOrUnknown(version);
            return this;
        }

        public Builder osInfo(String name, String version, String arch) {
            osName = valueOrUnknown(name);
            osVersion = valueOrUnknown(version);
            osArch = valueOrUnknown(arch);
            return this;
        }

        public Builder renderer(RendererPath renderer, ShaderPath shaders) {
            if (renderer == null || shaders == null) {
                throw new IllegalArgumentException("renderer and shader paths must not be null");
            }
            rendererPath = renderer;
            shaderPath = shaders;
            return this;
        }

        public Builder addMod(DetectedMod mod) {
            if (mod == null) {
                throw new IllegalArgumentException("mod must not be null");
            }
            DetectedMod existing = mods.get(mod.getNormalizedId());
            if (existing == null || existing.isSynthetic() && !mod.isSynthetic()) {
                mods.put(mod.getNormalizedId(), mod);
            }
            return this;
        }

        public Builder addMod(String id, String name, String version) {
            return addMod(new DetectedMod(id, name, version, false));
        }

        public Builder capability(EnvironmentCapability capability) {
            if (capability == null) {
                throw new IllegalArgumentException("capability must not be null");
            }
            capabilities.add(capability);
            return this;
        }

        public EnvironmentSnapshot build() {
            return new EnvironmentSnapshot(this);
        }
    }
}
