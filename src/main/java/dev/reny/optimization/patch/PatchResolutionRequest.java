package dev.reny.optimization.patch;

import java.util.Collections;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable input to one deterministic registry resolution pass. */
public final class PatchResolutionRequest {

    private final OptimizationProfile profile;
    private final SortedSet<String> explicitlyEnabled;
    private final SortedSet<String> explicitlyDisabled;
    private final SortedMap<String, PreconditionStatus> preconditions;

    private PatchResolutionRequest(Builder builder) {
        if (builder.profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        TreeSet<String> both = new TreeSet<String>(builder.explicitlyEnabled);
        both.retainAll(builder.explicitlyDisabled);
        if (!both.isEmpty()) {
            throw new IllegalArgumentException("Patch cannot be both explicitly enabled and disabled: " + both.first());
        }

        this.profile = builder.profile;
        this.explicitlyEnabled = Collections.unmodifiableSortedSet(new TreeSet<String>(builder.explicitlyEnabled));
        this.explicitlyDisabled = Collections.unmodifiableSortedSet(new TreeSet<String>(builder.explicitlyDisabled));
        this.preconditions = Collections
            .unmodifiableSortedMap(new TreeMap<String, PreconditionStatus>(builder.preconditions));
    }

    public static Builder builder(OptimizationProfile profile) {
        return new Builder(profile);
    }

    public OptimizationProfile getProfile() {
        return profile;
    }

    public SortedSet<String> getExplicitlyEnabled() {
        return explicitlyEnabled;
    }

    public SortedSet<String> getExplicitlyDisabled() {
        return explicitlyDisabled;
    }

    public SortedMap<String, PreconditionStatus> getPreconditions() {
        return preconditions;
    }

    public boolean isExplicitlyEnabled(String id) {
        return explicitlyEnabled.contains(id);
    }

    public boolean isExplicitlyDisabled(String id) {
        return explicitlyDisabled.contains(id);
    }

    public PreconditionStatus getPreconditionStatus(String id) {
        PreconditionStatus status = preconditions.get(id);
        return status == null ? PreconditionStatus.UNKNOWN : status;
    }

    public static final class Builder {

        private final OptimizationProfile profile;
        private final SortedSet<String> explicitlyEnabled = new TreeSet<String>();
        private final SortedSet<String> explicitlyDisabled = new TreeSet<String>();
        private final SortedMap<String, PreconditionStatus> preconditions = new TreeMap<String, PreconditionStatus>();

        private Builder(OptimizationProfile profile) {
            this.profile = profile;
        }

        public Builder enable(String patchId) {
            explicitlyEnabled.add(PatchDescriptor.validateId(patchId));
            return this;
        }

        public Builder disable(String patchId) {
            explicitlyDisabled.add(PatchDescriptor.validateId(patchId));
            return this;
        }

        public Builder precondition(String patchId, PreconditionStatus status) {
            String id = PatchDescriptor.validateId(patchId);
            if (status == null) {
                throw new IllegalArgumentException("precondition status must not be null");
            }
            preconditions.put(id, status);
            return this;
        }

        public PatchResolutionRequest build() {
            return new PatchResolutionRequest(this);
        }
    }
}
