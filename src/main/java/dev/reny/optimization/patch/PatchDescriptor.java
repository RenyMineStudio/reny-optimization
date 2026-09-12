package dev.reny.optimization.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Immutable metadata for a Reny optimization patch. This object intentionally
 * knows nothing about Mixin, ASM, Forge hooks, or any other implementation
 * mechanism.
 */
public final class PatchDescriptor {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

    private final String id;
    private final String module;
    private final PatchSide side;
    private final PatchRisk risk;
    private final boolean defaultEnabled;
    private final SortedSet<String> requiredPatchIds;
    private final SortedSet<String> conflictingPatchIds;
    private final List<String> affectedTargets;
    private final List<String> compatibilityNotes;
    private final SortedSet<String> benchmarkIds;

    private PatchDescriptor(Builder builder) {
        this.id = validateId(builder.id);
        this.module = validateToken("module", builder.module);
        this.side = requireNonNull("side", builder.side);
        this.risk = requireNonNull("risk", builder.risk);
        this.defaultEnabled = builder.defaultEnabled;
        this.requiredPatchIds = immutableIds(builder.requiredPatchIds, "required patch");
        this.conflictingPatchIds = immutableIds(builder.conflictingPatchIds, "conflicting patch");
        this.affectedTargets = immutableStrings(builder.affectedTargets, "affected target");
        this.compatibilityNotes = immutableStrings(builder.compatibilityNotes, "compatibility note");
        this.benchmarkIds = immutableTokens(builder.benchmarkIds, "benchmark id");

        if (requiredPatchIds.contains(id)) {
            throw new IllegalArgumentException("Patch cannot require itself: " + id);
        }
        if (conflictingPatchIds.contains(id)) {
            throw new IllegalArgumentException("Patch cannot conflict with itself: " + id);
        }
    }

    public static Builder builder(String id, String module) {
        return new Builder(id, module);
    }

    static String validateId(String id) {
        String value = validateToken("patch id", id);
        if (!ID_PATTERN.matcher(value)
            .matches()) {
            throw new IllegalArgumentException(
                "Invalid patch id '" + value + "'. Expected lowercase stable token such as world.fast_block_lookup");
        }
        return value;
    }

    private static String validateToken(String name, String value) {
        if (value == null || value.trim()
            .isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(String name, T value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    private static SortedSet<String> immutableIds(Iterable<String> source, String name) {
        TreeSet<String> values = new TreeSet<String>();
        for (String value : source) {
            values.add(validateId(value));
        }
        return Collections.unmodifiableSortedSet(values);
    }

    private static SortedSet<String> immutableTokens(Iterable<String> source, String name) {
        TreeSet<String> values = new TreeSet<String>();
        for (String value : source) {
            values.add(validateToken(name, value));
        }
        return Collections.unmodifiableSortedSet(values);
    }

    private static List<String> immutableStrings(Iterable<String> source, String name) {
        ArrayList<String> values = new ArrayList<String>();
        for (String value : source) {
            values.add(validateToken(name, value));
        }
        return Collections.unmodifiableList(values);
    }

    public String getId() {
        return id;
    }

    public String getModule() {
        return module;
    }

    public PatchSide getSide() {
        return side;
    }

    public PatchRisk getRisk() {
        return risk;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public SortedSet<String> getRequiredPatchIds() {
        return requiredPatchIds;
    }

    public SortedSet<String> getConflictingPatchIds() {
        return conflictingPatchIds;
    }

    public List<String> getAffectedTargets() {
        return affectedTargets;
    }

    public List<String> getCompatibilityNotes() {
        return compatibilityNotes;
    }

    public SortedSet<String> getBenchmarkIds() {
        return benchmarkIds;
    }

    /**
     * Unsafe patches fail closed until a compatibility/environment layer proves
     * their preconditions. SAFE patches do not require this external proof.
     */
    public boolean requiresVerifiedPreconditions() {
        return risk != PatchRisk.SAFE;
    }

    public static final class Builder {

        private final String id;
        private final String module;
        private PatchSide side = PatchSide.BOTH;
        private PatchRisk risk = PatchRisk.SAFE;
        private boolean defaultEnabled = true;
        private final SortedSet<String> requiredPatchIds = new TreeSet<String>();
        private final SortedSet<String> conflictingPatchIds = new TreeSet<String>();
        private final List<String> affectedTargets = new ArrayList<String>();
        private final List<String> compatibilityNotes = new ArrayList<String>();
        private final SortedSet<String> benchmarkIds = new TreeSet<String>();

        private Builder(String id, String module) {
            this.id = id;
            this.module = module;
        }

        public Builder side(PatchSide value) {
            this.side = value;
            return this;
        }

        public Builder risk(PatchRisk value) {
            this.risk = value;
            return this;
        }

        public Builder defaultEnabled(boolean value) {
            this.defaultEnabled = value;
            return this;
        }

        public Builder requires(String patchId) {
            this.requiredPatchIds.add(validateId(patchId));
            return this;
        }

        public Builder conflictsWith(String patchId) {
            this.conflictingPatchIds.add(validateId(patchId));
            return this;
        }

        public Builder affects(String target) {
            this.affectedTargets.add(validateToken("affected target", target));
            return this;
        }

        public Builder compatibilityNote(String note) {
            this.compatibilityNotes.add(validateToken("compatibility note", note));
            return this;
        }

        public Builder benchmark(String benchmarkId) {
            this.benchmarkIds.add(validateToken("benchmark id", benchmarkId));
            return this;
        }

        public PatchDescriptor build() {
            return new PatchDescriptor(this);
        }
    }
}
