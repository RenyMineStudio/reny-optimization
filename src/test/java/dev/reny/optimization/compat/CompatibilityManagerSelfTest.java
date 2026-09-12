package dev.reny.optimization.compat;

import java.util.List;

import dev.reny.optimization.patch.OptimizationProfile;
import dev.reny.optimization.patch.PatchDecision;
import dev.reny.optimization.patch.PatchDecisionReason;
import dev.reny.optimization.patch.PatchDescriptor;
import dev.reny.optimization.patch.PatchRegistry;
import dev.reny.optimization.patch.PatchResolutionRequest;
import dev.reny.optimization.patch.PatchRisk;
import dev.reny.optimization.patch.PatchSide;
import dev.reny.optimization.patch.PatchSnapshot;

/** Dependency-free compatibility/environment verification suite. */
public final class CompatibilityManagerSelfTest {

    private int passed;

    public static void main(String[] args) {
        new CompatibilityManagerSelfTest().run();
    }

    private void run() {
        testSnapshotNormalizesModsAndCapabilities();
        testManagerCanProveUnsafePatchPreconditions();
        testAngelicaReplacesGraphicsPatch();
        testOptiFineConflictsWithGraphicsPatch();
        testArchaicFixReplacesLightingPatch();
        testFalseTweaksReplacesChunkRenderer();
        testUnknownLwjgl3ifyVersionFailsClosedForInvasivePatch();
        testKnownLwjgl3ifyVersionAllowsPartialCompatibility();
        testMissingRequiredCapabilityFailsClosed();
        testDedicatedServerRejectsClientOnlyPatch();
        testDiagnosticsExplainKnownModConflict();
        System.out.println("CompatibilityManagerSelfTest: " + passed + " tests passed");
    }

    private void testSnapshotNormalizesModsAndCapabilities() {
        EnvironmentSnapshot snapshot = baseEnvironment().addMod("AnGeLiCa", "Angelica", "2.0.0")
            .capability(EnvironmentCapability.SHADERS)
            .renderer(RendererPath.ANGELICA, ShaderPath.ANGELICA)
            .build();
        assertTrue(snapshot.hasMod(KnownMod.ANGELICA));
        assertEquals("2.0.0", snapshot.findMod(KnownMod.ANGELICA)
            .getVersion());
        assertTrue(snapshot.hasCapability(EnvironmentCapability.FORGE_FML));
        assertTrue(snapshot.hasCapability(EnvironmentCapability.SHADERS));
        pass();
    }

    private void testManagerCanProveUnsafePatchPreconditions() {
        CompatibilityManager manager = new CompatibilityManager(baseEnvironment().build());
        PatchDescriptor patch = patch("tick.fast", PatchRisk.AGGRESSIVE).build();
        PatchSnapshot snapshot = resolve(manager, OptimizationProfile.AGGRESSIVE, patch);
        assertEnabled(snapshot, patch.getId());
        pass();
    }

    private void testAngelicaReplacesGraphicsPatch() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("angelica", "Angelica", "2.0.0")
                .renderer(RendererPath.ANGELICA, ShaderPath.ANGELICA)
                .build());
        PatchDescriptor patch = patch("render.native", PatchRisk.NUCLEAR)
            .compatibilityFeature(CompatibilityFeature.RENDERER)
            .build();
        assertReason(resolve(manager, OptimizationProfile.NUCLEAR, patch), patch.getId(),
            PatchDecisionReason.COMPATIBILITY_REPLACED);
        pass();
    }

    private void testOptiFineConflictsWithGraphicsPatch() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("optifine", "OptiFine", "HD_U_E7")
                .renderer(RendererPath.OPTIFINE, ShaderPath.OPTIFINE)
                .build());
        PatchDescriptor patch = patch("shader.pipeline", PatchRisk.EXPERIMENTAL)
            .compatibilityFeature(CompatibilityFeature.SHADER_PIPELINE)
            .build();
        assertReason(resolve(manager, OptimizationProfile.NUCLEAR, patch), patch.getId(),
            PatchDecisionReason.COMPATIBILITY_CONFLICT);
        pass();
    }

    private void testArchaicFixReplacesLightingPatch() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("archaicfix", "ArchaicFix", "0.7.0")
                .build());
        PatchDescriptor patch = patch("world.lighting", PatchRisk.AGGRESSIVE)
            .compatibilityFeature(CompatibilityFeature.LIGHTING_ENGINE)
            .build();
        assertReason(resolve(manager, OptimizationProfile.AGGRESSIVE, patch), patch.getId(),
            PatchDecisionReason.COMPATIBILITY_REPLACED);
        pass();
    }

    private void testFalseTweaksReplacesChunkRenderer() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("falsetweaks", "FalseTweaks", "4.4.5")
                .build());
        PatchDescriptor patch = patch("render.chunks", PatchRisk.EXPERIMENTAL)
            .compatibilityFeature(CompatibilityFeature.CHUNK_RENDERER)
            .build();
        assertReason(resolve(manager, OptimizationProfile.NUCLEAR, patch), patch.getId(),
            PatchDecisionReason.COMPATIBILITY_REPLACED);
        pass();
    }

    private void testUnknownLwjgl3ifyVersionFailsClosedForInvasivePatch() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("lwjgl3ify", "LWJGL3ify", "unknown")
                .capability(EnvironmentCapability.LWJGL3)
                .build());
        PatchDescriptor patch = patch("render.opengl_backend", PatchRisk.AGGRESSIVE)
            .compatibilityFeature(CompatibilityFeature.OPENGL_BACKEND)
            .build();
        assertReason(resolve(manager, OptimizationProfile.AGGRESSIVE, patch), patch.getId(),
            PatchDecisionReason.COMPATIBILITY_UNKNOWN);
        pass();
    }

    private void testKnownLwjgl3ifyVersionAllowsPartialCompatibility() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("lwjgl3ify", "LWJGL3ify", "2.1.0")
                .capability(EnvironmentCapability.LWJGL3)
                .build());
        PatchDescriptor patch = patch("render.opengl_backend", PatchRisk.AGGRESSIVE)
            .compatibilityFeature(CompatibilityFeature.OPENGL_BACKEND)
            .build();
        PatchSnapshot snapshot = resolve(manager, OptimizationProfile.AGGRESSIVE, patch);
        assertEnabled(snapshot, patch.getId());
        assertTrue(snapshot.getDecision(patch.getId())
            .getDetail()
            .contains("compatibility=PARTIAL"));
        pass();
    }

    private void testMissingRequiredCapabilityFailsClosed() {
        CompatibilityManager manager = new CompatibilityManager(baseEnvironment().build());
        PatchDescriptor patch = patch("mixin.requires_unimixins", PatchRisk.SAFE)
            .requiresCapability(EnvironmentCapability.UNIMIXINS)
            .build();
        PatchSnapshot snapshot = resolve(manager, OptimizationProfile.COMPATIBLE, patch);
        assertReason(snapshot, patch.getId(), PatchDecisionReason.COMPATIBILITY_CONFLICT);
        assertTrue(snapshot.getDecision(patch.getId())
            .getDetail()
            .contains("UNIMIXINS"));
        pass();
    }

    private void testDedicatedServerRejectsClientOnlyPatch() {
        CompatibilityManager manager = new CompatibilityManager(
            EnvironmentSnapshot.builder()
                .side(EnvironmentSide.DEDICATED_SERVER)
                .javaInfo("test", "8")
                .osInfo("test", "1", "x86_64")
                .renderer(RendererPath.VANILLA, ShaderPath.NONE)
                .capability(EnvironmentCapability.FORGE_FML)
                .build());
        PatchDescriptor patch = patch("render.client_only", PatchRisk.SAFE).side(PatchSide.CLIENT)
            .build();
        assertReason(resolve(manager, OptimizationProfile.COMPATIBLE, patch), patch.getId(),
            PatchDecisionReason.COMPATIBILITY_CONFLICT);
        pass();
    }

    private void testDiagnosticsExplainKnownModConflict() {
        CompatibilityManager manager = new CompatibilityManager(
            baseEnvironment().addMod("angelica", "Angelica", "2.0.0")
                .addMod("optifine", "OptiFine", "HD_U_E7")
                .renderer(RendererPath.UNKNOWN, ShaderPath.UNKNOWN)
                .build());
        List<String> lines = manager.toDiagnosticLines();
        assertContains(lines, "Angelica and OptiFine");
        assertEquals(
            CompatibilityState.CONFLICT,
            manager.inspectKnownTargets()
                .get(KnownMod.ANGELICA.getKey())
                .getState());
        pass();
    }

    private static EnvironmentSnapshot.Builder baseEnvironment() {
        return EnvironmentSnapshot.builder()
            .side(EnvironmentSide.CLIENT)
            .javaInfo("TestJVM", "8")
            .osInfo("Linux", "test", "x86_64")
            .renderer(RendererPath.VANILLA, ShaderPath.NONE)
            .capability(EnvironmentCapability.FORGE_FML);
    }

    private static PatchDescriptor.Builder patch(String id, PatchRisk risk) {
        return PatchDescriptor.builder(id, id.substring(0, id.indexOf('.')))
            .risk(risk);
    }

    private static PatchSnapshot resolve(CompatibilityManager manager, OptimizationProfile profile,
        PatchDescriptor patch) {
        PatchRegistry registry = new PatchRegistry().register(patch);
        return registry.resolve(
            PatchResolutionRequest.builder(profile)
                .compatibilityManager(manager)
                .build());
    }

    private static void assertEnabled(PatchSnapshot snapshot, String id) {
        PatchDecision decision = snapshot.getDecision(id);
        assertTrue(decision != null && decision.isEnabled());
    }

    private static void assertReason(PatchSnapshot snapshot, String id, PatchDecisionReason expected) {
        PatchDecision decision = snapshot.getDecision(id);
        assertTrue(decision != null && !decision.isEnabled());
        assertEquals(expected, decision.getReason());
    }

    private static void assertContains(List<String> lines, String fragment) {
        for (String line : lines) {
            if (line.contains(fragment)) {
                return;
            }
        }
        throw new AssertionError("Expected diagnostics to contain: " + fragment + " but got " + lines);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Assertion failed");
        }
    }

    private void pass() {
        passed++;
    }
}
