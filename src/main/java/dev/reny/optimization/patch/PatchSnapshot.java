package dev.reny.optimization.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Read-only diagnostic view of a complete patch resolution pass. */
public final class PatchSnapshot {

    private final OptimizationProfile profile;
    private final SortedMap<String, PatchDecision> decisions;
    private final SortedSet<String> enabledPatchIds;

    PatchSnapshot(OptimizationProfile profile, Map<String, PatchDecision> decisions) {
        this.profile = profile;
        TreeMap<String, PatchDecision> ordered = new TreeMap<String, PatchDecision>(decisions);
        this.decisions = Collections.unmodifiableSortedMap(ordered);

        TreeSet<String> enabled = new TreeSet<String>();
        for (Map.Entry<String, PatchDecision> entry : ordered.entrySet()) {
            if (entry.getValue()
                .isEnabled()) {
                enabled.add(entry.getKey());
            }
        }
        this.enabledPatchIds = Collections.unmodifiableSortedSet(enabled);
    }

    public OptimizationProfile getProfile() {
        return profile;
    }

    public SortedMap<String, PatchDecision> getDecisions() {
        return decisions;
    }

    public PatchDecision getDecision(String patchId) {
        return decisions.get(patchId);
    }

    public SortedSet<String> getEnabledPatchIds() {
        return enabledPatchIds;
    }

    public int getEnabledCount() {
        return enabledPatchIds.size();
    }

    public int getDisabledCount() {
        return decisions.size() - enabledPatchIds.size();
    }

    /** Stable lines suitable for future `/reny patches` diagnostics. */
    public List<String> toDiagnosticLines() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("profile=" + profile + " enabled=" + getEnabledCount() + " disabled=" + getDisabledCount());
        for (Map.Entry<String, PatchDecision> entry : decisions.entrySet()) {
            PatchDecision decision = entry.getValue();
            if (decision.isEnabled()) {
                lines.add("[ENABLED] " + entry.getKey() + " - " + decision.getDetail());
            } else {
                lines.add("[DISABLED:" + decision.getReason() + "] " + entry.getKey() + " - " + decision.getDetail());
            }
        }
        return Collections.unmodifiableList(lines);
    }
}
