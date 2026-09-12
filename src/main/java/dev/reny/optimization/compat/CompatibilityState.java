package dev.reny.optimization.compat;

/** Stable compatibility states used by environment and patch diagnostics. */
public enum CompatibilityState {
    COMPATIBLE,
    PARTIAL,
    REPLACED,
    CONFLICT,
    UNKNOWN
}
