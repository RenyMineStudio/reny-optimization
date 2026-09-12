package dev.reny.optimization.patch;

/** Immutable result for one patch after registry resolution. */
public final class PatchDecision {

    private final PatchDescriptor descriptor;
    private final boolean enabled;
    private final PatchDecisionReason reason;
    private final String detail;

    private PatchDecision(
        PatchDescriptor descriptor,
        boolean enabled,
        PatchDecisionReason reason,
        String detail) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        this.descriptor = descriptor;
        this.enabled = enabled;
        this.reason = reason;
        this.detail = detail == null ? "" : detail;
    }

    static PatchDecision enabled(PatchDescriptor descriptor, String detail) {
        return new PatchDecision(descriptor, true, PatchDecisionReason.ENABLED, detail);
    }

    static PatchDecision disabled(PatchDescriptor descriptor, PatchDecisionReason reason, String detail) {
        if (reason == PatchDecisionReason.ENABLED) {
            throw new IllegalArgumentException("disabled decision cannot use ENABLED reason");
        }
        return new PatchDecision(descriptor, false, reason, detail);
    }

    public PatchDescriptor getDescriptor() {
        return descriptor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PatchDecisionReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }
}
