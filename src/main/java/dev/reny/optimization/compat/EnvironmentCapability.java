package dev.reny.optimization.compat;

/** Capabilities that a patch may require from the detected runtime environment. */
public enum EnvironmentCapability {
    FORGE_FML,
    MIXIN,
    UNIMIXINS,
    LWJGL3,
    MODERN_OPENGL,
    SHADERS
}
