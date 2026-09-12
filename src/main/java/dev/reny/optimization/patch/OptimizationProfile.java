package dev.reny.optimization.patch;

/** User-selectable optimization envelope. */
public enum OptimizationProfile {
    COMPATIBLE,
    AGGRESSIVE,
    NUCLEAR;

    /**
     * Profiles are hard ceilings. Explicit patch enables do not bypass them.
     * Experimental patches only auto-participate in NUCLEAR, keeping the two
     * safer profiles predictable.
     */
    public boolean allows(PatchRisk risk) {
        switch (this) {
            case COMPATIBLE:
                return risk == PatchRisk.SAFE;
            case AGGRESSIVE:
                return risk == PatchRisk.SAFE || risk == PatchRisk.AGGRESSIVE;
            case NUCLEAR:
                return true;
            default:
                throw new AssertionError("Unhandled profile: " + this);
        }
    }
}
