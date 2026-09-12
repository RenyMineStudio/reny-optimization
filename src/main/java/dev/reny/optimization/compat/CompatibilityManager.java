package dev.reny.optimization.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import dev.reny.optimization.patch.PatchDescriptor;
import dev.reny.optimization.patch.PatchRisk;
import dev.reny.optimization.patch.PatchSide;

/** Central compatibility policy for the detected Minecraft 1.7.10 environment. */
public final class CompatibilityManager {

    private static final Set<CompatibilityFeature> GRAPHICS_FEATURES = Collections.unmodifiableSet(
        EnumSet.of(
            CompatibilityFeature.RENDERER,
            CompatibilityFeature.CHUNK_RENDERER,
            CompatibilityFeature.SHADER_PIPELINE,
            CompatibilityFeature.TEXTURE_PIPELINE,
            CompatibilityFeature.OPENGL_BACKEND));

    private final EnvironmentSnapshot environment;

    public CompatibilityManager(EnvironmentSnapshot environment) {
        if (environment == null) {
            throw new IllegalArgumentException("environment must not be null");
        }
        this.environment = environment;
    }

    public static CompatibilityManager detect() {
        return new CompatibilityManager(EnvironmentDetector.capture());
    }

    public EnvironmentSnapshot getEnvironment() {
        return environment;
    }

    public CompatibilityDecision evaluatePatch(PatchDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }

        if (!environment.hasCapability(EnvironmentCapability.FORGE_FML)) {
            return decision(
                descriptor,
                CompatibilityState.CONFLICT,
                "Forge/FML capability was not detected; patch activation fails closed");
        }

        if (descriptor.getSide() == PatchSide.CLIENT && environment.getSide() == EnvironmentSide.DEDICATED_SERVER) {
            return decision(descriptor, CompatibilityState.CONFLICT, "client-only patch cannot run on a dedicated server");
        }
        if (descriptor.getSide() == PatchSide.CLIENT && environment.getSide() == EnvironmentSide.UNKNOWN) {
            return decision(descriptor, CompatibilityState.UNKNOWN, "physical side is unknown for a client-only patch");
        }

        for (EnvironmentCapability capability : descriptor.getRequiredCapabilities()) {
            if (!environment.hasCapability(capability)) {
                return decision(
                    descriptor,
                    CompatibilityState.CONFLICT,
                    "required environment capability is unavailable: " + capability);
            }
        }

        CompatibilityDecision result = decision(
            descriptor,
            CompatibilityState.COMPATIBLE,
            "no active compatibility rule blocks this patch");
        result = combine(result, evaluateOptiFine(descriptor));
        result = combine(result, evaluateAngelica(descriptor));
        result = combine(result, evaluateArchaicFix(descriptor));
        result = combine(result, evaluateFalseTweaks(descriptor));
        result = combine(result, evaluateLwjgl3ify(descriptor));
        return result;
    }

    public SortedMap<String, CompatibilityDecision> inspectKnownTargets() {
        TreeMap<String, CompatibilityDecision> decisions = new TreeMap<String, CompatibilityDecision>();
        for (KnownMod knownMod : KnownMod.values()) {
            decisions.put(knownMod.getKey(), inspectKnownTarget(knownMod));
        }
        return Collections.unmodifiableSortedMap(decisions);
    }

    public List<String> toDiagnosticLines() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.addAll(environment.toDiagnosticLines());
        for (CompatibilityDecision decision : inspectKnownTargets().values()) {
            lines.add(decision.toDiagnosticLine());
        }
        return Collections.unmodifiableList(lines);
    }

    private CompatibilityDecision inspectKnownTarget(KnownMod knownMod) {
        DetectedMod mod = environment.findMod(knownMod);
        if (knownMod == KnownMod.FORGE_FML) {
            return environment.hasCapability(EnvironmentCapability.FORGE_FML)
                ? targetDecision(knownMod, CompatibilityState.COMPATIBLE, "Forge/FML runtime detected")
                : targetDecision(knownMod, CompatibilityState.CONFLICT, "Forge/FML runtime was not detected");
        }
        if (mod == null) {
            if (knownMod == KnownMod.UNIMIXINS && environment.hasCapability(EnvironmentCapability.MIXIN)) {
                return targetDecision(
                    knownMod,
                    CompatibilityState.PARTIAL,
                    "Mixin is available but UniMixins itself was not identified");
            }
            return targetDecision(knownMod, CompatibilityState.COMPATIBLE, "not detected; no coexistence rule is active");
        }

        if (knownMod == KnownMod.ANGELICA && environment.hasMod(KnownMod.OPTIFINE)
            || knownMod == KnownMod.OPTIFINE && environment.hasMod(KnownMod.ANGELICA)) {
            return targetDecision(
                knownMod,
                CompatibilityState.CONFLICT,
                "Angelica and OptiFine were detected together; their renderer paths are not supported together");
        }

        if (!mod.isVersionKnown() && (knownMod == KnownMod.LWJGL3IFY || knownMod == KnownMod.FALSE_TWEAKS)) {
            return targetDecision(
                knownMod,
                CompatibilityState.UNKNOWN,
                "detected with unknown version; invasive overlapping patches fail closed");
        }

        switch (knownMod) {
            case UNIMIXINS:
                return targetDecision(
                    knownMod,
                    CompatibilityState.COMPATIBLE,
                    "detected " + mod.getVersion() + "; used as a supported Mixin bootstrap path");
            case ANGELICA:
                return targetDecision(
                    knownMod,
                    CompatibilityState.PARTIAL,
                    "detected " + mod.getVersion() + "; Angelica owns renderer, chunk-renderer, shader, texture, and GL paths");
            case ARCHAIC_FIX:
                return targetDecision(
                    knownMod,
                    CompatibilityState.PARTIAL,
                    "detected " + mod.getVersion() + "; ArchaicFix owns the overlapping lighting-engine path");
            case FALSE_TWEAKS:
                return targetDecision(
                    knownMod,
                    CompatibilityState.PARTIAL,
                    "detected " + mod.getVersion() + "; chunk-rendering and selected renderer/texture features may overlap");
            case OPTIFINE:
                return targetDecision(
                    knownMod,
                    CompatibilityState.PARTIAL,
                    "detected " + mod.getVersion() + "; non-graphics patches may coexist while equivalent graphics paths conflict");
            case LWJGL3IFY:
                return targetDecision(
                    knownMod,
                    CompatibilityState.PARTIAL,
                    "detected " + mod.getVersion() + "; OpenGL/runtime backend assumptions differ from legacy LWJGL2");
            default:
                return targetDecision(knownMod, CompatibilityState.COMPATIBLE, "detected " + mod.getVersion());
        }
    }

    private CompatibilityDecision evaluateOptiFine(PatchDescriptor descriptor) {
        DetectedMod mod = environment.findMod(KnownMod.OPTIFINE);
        if (mod == null || !overlaps(descriptor, GRAPHICS_FEATURES)) {
            return null;
        }
        return ruleDecision(
            descriptor,
            CompatibilityState.CONFLICT,
            mod,
            "OptiFine owns an overlapping renderer/shader/OpenGL path");
    }

    private CompatibilityDecision evaluateAngelica(PatchDescriptor descriptor) {
        DetectedMod mod = environment.findMod(KnownMod.ANGELICA);
        if (mod == null || !overlaps(descriptor, GRAPHICS_FEATURES)) {
            return null;
        }
        return ruleDecision(
            descriptor,
            CompatibilityState.REPLACED,
            mod,
            "Angelica already provides the overlapping renderer/shader/OpenGL functionality");
    }

    private CompatibilityDecision evaluateArchaicFix(PatchDescriptor descriptor) {
        DetectedMod mod = environment.findMod(KnownMod.ARCHAIC_FIX);
        if (mod == null || !descriptor.getCompatibilityFeatures()
            .contains(CompatibilityFeature.LIGHTING_ENGINE)) {
            return null;
        }
        return ruleDecision(
            descriptor,
            CompatibilityState.REPLACED,
            mod,
            "ArchaicFix already provides the overlapping lighting-engine functionality");
    }

    private CompatibilityDecision evaluateFalseTweaks(PatchDescriptor descriptor) {
        DetectedMod mod = environment.findMod(KnownMod.FALSE_TWEAKS);
        if (mod == null) {
            return null;
        }
        Set<CompatibilityFeature> features = descriptor.getCompatibilityFeatures();
        if (features.contains(CompatibilityFeature.CHUNK_RENDERER)) {
            return ruleDecision(
                descriptor,
                CompatibilityState.REPLACED,
                mod,
                "FalseTweaks already provides an overlapping chunk-renderer path");
        }
        if (features.contains(CompatibilityFeature.RENDERER) || features.contains(CompatibilityFeature.TEXTURE_PIPELINE)) {
            if (!mod.isVersionKnown() && descriptor.getRisk() != PatchRisk.SAFE) {
                return ruleDecision(
                    descriptor,
                    CompatibilityState.UNKNOWN,
                    mod,
                    "FalseTweaks version is unknown for an invasive overlapping renderer/texture patch");
            }
            return ruleDecision(
                descriptor,
                CompatibilityState.PARTIAL,
                mod,
                "FalseTweaks may overlap selected renderer/texture modules; duplicate chunk-rendering is disabled separately");
        }
        return null;
    }

    private CompatibilityDecision evaluateLwjgl3ify(PatchDescriptor descriptor) {
        DetectedMod mod = environment.findMod(KnownMod.LWJGL3IFY);
        if (mod == null || !descriptor.getCompatibilityFeatures()
            .contains(CompatibilityFeature.OPENGL_BACKEND)) {
            return null;
        }
        if (!mod.isVersionKnown() && descriptor.getRisk() != PatchRisk.SAFE) {
            return ruleDecision(
                descriptor,
                CompatibilityState.UNKNOWN,
                mod,
                "LWJGL3ify version is unknown for an invasive OpenGL-backend patch");
        }
        return ruleDecision(
            descriptor,
            CompatibilityState.PARTIAL,
            mod,
            "LWJGL3ify changes the OpenGL/runtime backend; this patch may run only within that detected backend");
    }

    private static boolean overlaps(PatchDescriptor descriptor, Set<CompatibilityFeature> features) {
        for (CompatibilityFeature feature : descriptor.getCompatibilityFeatures()) {
            if (features.contains(feature)) {
                return true;
            }
        }
        return false;
    }

    private static CompatibilityDecision combine(CompatibilityDecision current, CompatibilityDecision candidate) {
        if (candidate == null) {
            return current;
        }
        int currentSeverity = severity(current.getState());
        int candidateSeverity = severity(candidate.getState());
        if (candidateSeverity > currentSeverity) {
            return candidate;
        }
        if (candidateSeverity == currentSeverity && candidate.getState() != CompatibilityState.COMPATIBLE) {
            return new CompatibilityDecision(
                current.getTarget(),
                current.getState(),
                current.getDetail() + "; " + candidate.getDetail());
        }
        return current;
    }

    private static int severity(CompatibilityState state) {
        switch (state) {
            case CONFLICT:
                return 5;
            case REPLACED:
                return 4;
            case UNKNOWN:
                return 3;
            case PARTIAL:
                return 2;
            case COMPATIBLE:
                return 1;
            default:
                throw new AssertionError("Unhandled compatibility state: " + state);
        }
    }

    private static CompatibilityDecision ruleDecision(PatchDescriptor descriptor, CompatibilityState state,
        DetectedMod mod, String detail) {
        return decision(
            descriptor,
            state,
            detail + " (detected " + mod.getName() + " " + mod.getVersion() + ")");
    }

    private static CompatibilityDecision decision(PatchDescriptor descriptor, CompatibilityState state, String detail) {
        return new CompatibilityDecision("patch." + descriptor.getId(), state, detail);
    }

    private static CompatibilityDecision targetDecision(KnownMod mod, CompatibilityState state, String detail) {
        return new CompatibilityDecision("mod." + mod.getKey(), state, detail);
    }
}
