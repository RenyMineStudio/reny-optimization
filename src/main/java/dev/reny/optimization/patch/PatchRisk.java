package dev.reny.optimization.patch;

/**
 * Risk classification for an optimization patch. The ordering is deliberate:
 * later values represent a narrower compatibility envelope.
 */
public enum PatchRisk {
    SAFE,
    AGGRESSIVE,
    EXPERIMENTAL,
    NUCLEAR
}
