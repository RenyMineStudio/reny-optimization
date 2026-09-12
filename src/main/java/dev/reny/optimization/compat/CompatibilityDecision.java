package dev.reny.optimization.compat;

/** Inspectable, machine-readable compatibility result. */
public final class CompatibilityDecision {

    private final String target;
    private final CompatibilityState state;
    private final String detail;

    public CompatibilityDecision(String target, CompatibilityState state, String detail) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("target must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        this.target = target.trim();
        this.state = state;
        this.detail = detail == null || detail.trim().isEmpty() ? "no detail" : detail.trim();
    }

    public String getTarget() {
        return target;
    }

    public CompatibilityState getState() {
        return state;
    }

    public String getDetail() {
        return detail;
    }

    public String toDiagnosticLine() {
        return '[' + state.name() + "] " + target + ": " + detail;
    }
}
