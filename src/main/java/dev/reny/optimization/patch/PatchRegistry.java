package dev.reny.optimization.patch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import dev.reny.optimization.compat.CompatibilityDecision;
import dev.reny.optimization.compat.CompatibilityManager;
import dev.reny.optimization.compat.CompatibilityState;

/**
 * Central deterministic registry for all Reny optimization patches.
 *
 * <p>
 * The registry decides whether patches may participate. It deliberately does
 * not know how an enabled patch is implemented.
 * </p>
 */
public final class PatchRegistry {

    private final SortedMap<String, PatchDescriptor> descriptors = new TreeMap<String, PatchDescriptor>();

    public PatchRegistry register(PatchDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        String id = descriptor.getId();
        if (descriptors.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate patch id: " + id);
        }
        descriptors.put(id, descriptor);
        return this;
    }

    public PatchRegistry registerAll(Collection<PatchDescriptor> patches) {
        if (patches == null) {
            throw new IllegalArgumentException("patches must not be null");
        }
        for (PatchDescriptor patch : patches) {
            register(patch);
        }
        return this;
    }

    public SortedMap<String, PatchDescriptor> getRegisteredDescriptors() {
        return Collections.unmodifiableSortedMap(new TreeMap<String, PatchDescriptor>(descriptors));
    }

    public PatchSnapshot resolve(PatchResolutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        validateRequestReferences(request);

        TreeMap<String, PatchDecision> decisions = new TreeMap<String, PatchDecision>();
        TreeSet<String> enabled = new TreeSet<String>();

        selectInitialCandidates(request, decisions, enabled);
        disableMissingDependencies(decisions, enabled);
        disableDependencyCycles(decisions, enabled);
        propagateDisabledDependencies(decisions, enabled);
        resolveConflicts(request, decisions, enabled);
        propagateDisabledDependencies(decisions, enabled);

        return new PatchSnapshot(request.getProfile(), decisions);
    }

    private void validateRequestReferences(PatchResolutionRequest request) {
        TreeSet<String> referenced = new TreeSet<String>();
        referenced.addAll(request.getExplicitlyEnabled());
        referenced.addAll(request.getExplicitlyDisabled());
        referenced.addAll(
            request.getPreconditions()
                .keySet());
        for (String id : referenced) {
            if (!descriptors.containsKey(id)) {
                throw new IllegalArgumentException("Resolution request references unknown patch id: " + id);
            }
        }
    }

    private void selectInitialCandidates(PatchResolutionRequest request, Map<String, PatchDecision> decisions,
        SortedSet<String> enabled) {

        for (PatchDescriptor descriptor : descriptors.values()) {
            String id = descriptor.getId();

            if (request.isExplicitlyDisabled(id)) {
                decisions.put(
                    id,
                    PatchDecision.disabled(
                        descriptor,
                        PatchDecisionReason.EXPLICITLY_DISABLED,
                        "disabled explicitly by configuration"));
                continue;
            }

            if (!request.getProfile()
                .allows(descriptor.getRisk())) {
                decisions.put(
                    id,
                    PatchDecision.disabled(
                        descriptor,
                        PatchDecisionReason.PROFILE_RESTRICTED,
                        "risk " + descriptor.getRisk() + " is outside profile " + request.getProfile()));
                continue;
            }

            if (!descriptor.isDefaultEnabled() && !request.isExplicitlyEnabled(id)) {
                decisions.put(
                    id,
                    PatchDecision.disabled(
                        descriptor,
                        PatchDecisionReason.NOT_DEFAULT_ENABLED,
                        "patch is opt-in and was not explicitly enabled"));
                continue;
            }

            CompatibilityDecision compatibility = evaluateCompatibility(request.getCompatibilityManager(), descriptor);
            if (compatibility != null) {
                PatchDecisionReason compatibilityReason = disabledCompatibilityReason(compatibility.getState());
                if (compatibilityReason != null) {
                    decisions.put(
                        id,
                        PatchDecision.disabled(descriptor, compatibilityReason, compatibility.getDetail()));
                    continue;
                }
            }

            if (descriptor.requiresVerifiedPreconditions()) {
                PreconditionStatus status = request.getPreconditionStatus(id);
                if (status == PreconditionStatus.UNKNOWN && compatibility != null
                    && (compatibility.getState() == CompatibilityState.COMPATIBLE
                        || compatibility.getState() == CompatibilityState.PARTIAL)) {
                    status = PreconditionStatus.SATISFIED;
                }
                if (status == PreconditionStatus.UNKNOWN) {
                    decisions.put(
                        id,
                        PatchDecision.disabled(
                            descriptor,
                            PatchDecisionReason.PRECONDITION_UNKNOWN,
                            "unsafe patch requires explicit compatibility/environment proof"));
                    continue;
                }
                if (status == PreconditionStatus.UNSATISFIED) {
                    decisions.put(
                        id,
                        PatchDecision.disabled(
                            descriptor,
                            PatchDecisionReason.PRECONDITION_UNSATISFIED,
                            "compatibility/environment precondition was not satisfied"));
                    continue;
                }
            }

            String source = request.isExplicitlyEnabled(id)
                ? "explicitly enabled within profile " + request.getProfile()
                : "enabled by default in profile " + request.getProfile();
            if (compatibility != null) {
                source += "; compatibility=" + compatibility.getState() + " (" + compatibility.getDetail() + ')';
            }
            decisions.put(id, PatchDecision.enabled(descriptor, source));
            enabled.add(id);
        }
    }

    private static CompatibilityDecision evaluateCompatibility(CompatibilityManager manager,
        PatchDescriptor descriptor) {
        return manager == null ? null : manager.evaluatePatch(descriptor);
    }

    private static PatchDecisionReason disabledCompatibilityReason(CompatibilityState state) {
        switch (state) {
            case REPLACED:
                return PatchDecisionReason.COMPATIBILITY_REPLACED;
            case CONFLICT:
                return PatchDecisionReason.COMPATIBILITY_CONFLICT;
            case UNKNOWN:
                return PatchDecisionReason.COMPATIBILITY_UNKNOWN;
            case COMPATIBLE:
            case PARTIAL:
                return null;
            default:
                throw new AssertionError("Unhandled compatibility state: " + state);
        }
    }

    private void disableMissingDependencies(Map<String, PatchDecision> decisions, SortedSet<String> enabled) {

        for (String id : new ArrayList<String>(enabled)) {
            PatchDescriptor descriptor = descriptors.get(id);
            for (String requiredId : descriptor.getRequiredPatchIds()) {
                if (!descriptors.containsKey(requiredId)) {
                    disable(
                        id,
                        PatchDecisionReason.MISSING_DEPENDENCY,
                        "required patch is not registered: " + requiredId,
                        decisions,
                        enabled);
                    break;
                }
            }
        }
    }

    private void disableDependencyCycles(Map<String, PatchDecision> decisions, SortedSet<String> enabled) {

        SortedSet<String> cycleMembers = findDependencyCycleMembers(enabled);
        if (cycleMembers.isEmpty()) {
            return;
        }
        String detail = "dependency cycle detected among " + cycleMembers;
        for (String id : new ArrayList<String>(cycleMembers)) {
            if (enabled.contains(id)) {
                disable(id, PatchDecisionReason.DEPENDENCY_CYCLE, detail, decisions, enabled);
            }
        }
    }

    private void propagateDisabledDependencies(Map<String, PatchDecision> decisions, SortedSet<String> enabled) {

        boolean changed;
        do {
            changed = false;
            for (String id : new ArrayList<String>(enabled)) {
                PatchDescriptor descriptor = descriptors.get(id);
                for (String requiredId : descriptor.getRequiredPatchIds()) {
                    if (!enabled.contains(requiredId)) {
                        disable(
                            id,
                            PatchDecisionReason.DISABLED_DEPENDENCY,
                            "required patch is disabled: " + requiredId,
                            decisions,
                            enabled);
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);
    }

    private void resolveConflicts(final PatchResolutionRequest request, Map<String, PatchDecision> decisions,
        SortedSet<String> enabled) {

        ArrayList<String> ordered = new ArrayList<String>(enabled);
        Collections.sort(ordered, new Comparator<String>() {

            @Override
            public int compare(String left, String right) {
                boolean leftExplicit = request.isExplicitlyEnabled(left);
                boolean rightExplicit = request.isExplicitlyEnabled(right);
                if (leftExplicit != rightExplicit) {
                    return leftExplicit ? -1 : 1;
                }

                int risk = descriptors.get(left)
                    .getRisk()
                    .compareTo(
                        descriptors.get(right)
                            .getRisk());
                if (risk != 0) {
                    return risk;
                }
                return left.compareTo(right);
            }
        });

        TreeSet<String> accepted = new TreeSet<String>();
        for (String candidate : ordered) {
            if (!enabled.contains(candidate)) {
                continue;
            }
            String winner = firstConflictingPatch(candidate, accepted);
            if (winner == null) {
                accepted.add(candidate);
                continue;
            }

            disable(
                candidate,
                PatchDecisionReason.CONFLICT,
                "conflicts with higher-priority enabled patch: " + winner,
                decisions,
                enabled);
        }
    }

    private String firstConflictingPatch(String candidate, SortedSet<String> accepted) {
        for (String acceptedId : accepted) {
            if (conflicts(candidate, acceptedId)) {
                return acceptedId;
            }
        }
        return null;
    }

    private boolean conflicts(String left, String right) {
        PatchDescriptor leftDescriptor = descriptors.get(left);
        PatchDescriptor rightDescriptor = descriptors.get(right);
        return leftDescriptor.getConflictingPatchIds()
            .contains(right)
            || rightDescriptor.getConflictingPatchIds()
                .contains(left);
    }

    private void disable(String id, PatchDecisionReason reason, String detail, Map<String, PatchDecision> decisions,
        SortedSet<String> enabled) {

        PatchDescriptor descriptor = descriptors.get(id);
        decisions.put(id, PatchDecision.disabled(descriptor, reason, detail));
        enabled.remove(id);
    }

    private SortedSet<String> findDependencyCycleMembers(SortedSet<String> enabled) {
        TreeSet<String> cycleMembers = new TreeSet<String>();
        HashMap<String, Integer> state = new HashMap<String, Integer>();
        ArrayList<String> stack = new ArrayList<String>();

        for (String id : enabled) {
            if (!state.containsKey(id)) {
                findCyclesDepthFirst(id, enabled, state, stack, cycleMembers);
            }
        }
        return cycleMembers;
    }

    private void findCyclesDepthFirst(String id, SortedSet<String> enabled, Map<String, Integer> state,
        List<String> stack, SortedSet<String> cycleMembers) {

        state.put(id, Integer.valueOf(1));
        stack.add(id);

        PatchDescriptor descriptor = descriptors.get(id);
        for (String requiredId : descriptor.getRequiredPatchIds()) {
            if (!enabled.contains(requiredId)) {
                continue;
            }
            Integer requiredState = state.get(requiredId);
            if (requiredState == null) {
                findCyclesDepthFirst(requiredId, enabled, state, stack, cycleMembers);
            } else if (requiredState.intValue() == 1) {
                int cycleStart = stack.indexOf(requiredId);
                for (int i = cycleStart; i < stack.size(); i++) {
                    cycleMembers.add(stack.get(i));
                }
            }
        }

        stack.remove(stack.size() - 1);
        state.put(id, Integer.valueOf(2));
    }
}
