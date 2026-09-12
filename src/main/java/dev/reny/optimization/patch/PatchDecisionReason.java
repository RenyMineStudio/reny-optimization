package dev.reny.optimization.patch;

/** Stable machine-readable reason for a patch decision. */
public enum PatchDecisionReason {
    ENABLED,
    EXPLICITLY_DISABLED,
    NOT_DEFAULT_ENABLED,
    PROFILE_RESTRICTED,
    PRECONDITION_UNKNOWN,
    PRECONDITION_UNSATISFIED,
    MISSING_DEPENDENCY,
    DISABLED_DEPENDENCY,
    DEPENDENCY_CYCLE,
    CONFLICT
}
