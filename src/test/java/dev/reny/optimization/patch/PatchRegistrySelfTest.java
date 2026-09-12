package dev.reny.optimization.patch;

/**
 * Dependency-free unit test harness for the registry core. It can run before
 * the Forge/Gradle scaffold from issue #2 exists.
 */
public final class PatchRegistrySelfTest {

    private int passed;

    public static void main(String[] args) {
        new PatchRegistrySelfTest().run();
    }

    private void run() {
        testCompatibleProfileOnlyAllowsSafePatches();
        testUnsafePatchFailsClosedWithoutPreconditionProof();
        testProfileIsHardCeilingEvenForExplicitEnable();
        testExplicitEnableActivatesOptInPatch();
        testDependenciesResolveAndPropagateDisablement();
        testMissingDependencyFailsClosed();
        testDependencyCycleFailsClosed();
        testConflictsResolveDeterministically();
        testExplicitEnableWinsConflictPriority();
        testDuplicateIdsAreRejected();
        testUnknownConfigReferenceIsRejected();
        testDiagnosticSnapshotIsStable();

        System.out.println("PatchRegistrySelfTest: " + passed + " tests passed");
    }

    private void testCompatibleProfileOnlyAllowsSafePatches() {
        PatchRegistry registry = new PatchRegistry().register(patch("memory.safe", PatchRisk.SAFE).build())
            .register(patch("tick.aggressive", PatchRisk.AGGRESSIVE).build())
            .register(patch("world.experimental", PatchRisk.EXPERIMENTAL).build())
            .register(patch("render.nuclear", PatchRisk.NUCLEAR).build());

        PatchSnapshot snapshot = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());

        assertEnabled(snapshot, "memory.safe");
        assertReason(snapshot, "tick.aggressive", PatchDecisionReason.PROFILE_RESTRICTED);
        assertReason(snapshot, "world.experimental", PatchDecisionReason.PROFILE_RESTRICTED);
        assertReason(snapshot, "render.nuclear", PatchDecisionReason.PROFILE_RESTRICTED);
        pass();
    }

    private void testUnsafePatchFailsClosedWithoutPreconditionProof() {
        PatchRegistry registry = new PatchRegistry().register(patch("tick.fast", PatchRisk.AGGRESSIVE).build());

        PatchSnapshot unknown = registry.resolve(request(OptimizationProfile.AGGRESSIVE).build());
        assertReason(unknown, "tick.fast", PatchDecisionReason.PRECONDITION_UNKNOWN);

        PatchSnapshot satisfied = registry.resolve(
            request(OptimizationProfile.AGGRESSIVE).precondition("tick.fast", PreconditionStatus.SATISFIED)
                .build());
        assertEnabled(satisfied, "tick.fast");
        pass();
    }

    private void testProfileIsHardCeilingEvenForExplicitEnable() {
        PatchRegistry registry = new PatchRegistry().register(
            patch("render.rewrite", PatchRisk.NUCLEAR).defaultEnabled(false)
                .build());

        PatchSnapshot snapshot = registry.resolve(
            request(OptimizationProfile.COMPATIBLE).enable("render.rewrite")
                .precondition("render.rewrite", PreconditionStatus.SATISFIED)
                .build());

        assertReason(snapshot, "render.rewrite", PatchDecisionReason.PROFILE_RESTRICTED);
        pass();
    }

    private void testExplicitEnableActivatesOptInPatch() {
        PatchRegistry registry = new PatchRegistry().register(
            patch("memory.opt_in", PatchRisk.SAFE).defaultEnabled(false)
                .build());

        PatchSnapshot defaultSnapshot = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());
        assertReason(defaultSnapshot, "memory.opt_in", PatchDecisionReason.NOT_DEFAULT_ENABLED);

        PatchSnapshot enabledSnapshot = registry.resolve(
            request(OptimizationProfile.COMPATIBLE).enable("memory.opt_in")
                .build());
        assertEnabled(enabledSnapshot, "memory.opt_in");
        pass();
    }

    private void testDependenciesResolveAndPropagateDisablement() {
        PatchRegistry registry = new PatchRegistry().register(
            patch("core.base", PatchRisk.SAFE).defaultEnabled(false)
                .build())
            .register(
                patch("world.child", PatchRisk.SAFE).requires("core.base")
                    .build());

        PatchSnapshot withoutBase = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());
        assertReason(withoutBase, "world.child", PatchDecisionReason.DISABLED_DEPENDENCY);

        PatchSnapshot withBase = registry.resolve(
            request(OptimizationProfile.COMPATIBLE).enable("core.base")
                .build());
        assertEnabled(withBase, "core.base");
        assertEnabled(withBase, "world.child");
        pass();
    }

    private void testMissingDependencyFailsClosed() {
        PatchRegistry registry = new PatchRegistry().register(
            patch("world.child", PatchRisk.SAFE).requires("core.not_registered")
                .build());

        PatchSnapshot snapshot = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());
        assertReason(snapshot, "world.child", PatchDecisionReason.MISSING_DEPENDENCY);
        pass();
    }

    private void testDependencyCycleFailsClosed() {
        PatchRegistry registry = new PatchRegistry()
            .register(
                patch("cycle.alpha", PatchRisk.SAFE).requires("cycle.beta")
                    .build())
            .register(
                patch("cycle.beta", PatchRisk.SAFE).requires("cycle.alpha")
                    .build());

        PatchSnapshot snapshot = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());
        assertReason(snapshot, "cycle.alpha", PatchDecisionReason.DEPENDENCY_CYCLE);
        assertReason(snapshot, "cycle.beta", PatchDecisionReason.DEPENDENCY_CYCLE);
        pass();
    }

    private void testConflictsResolveDeterministically() {
        PatchRegistry registry = new PatchRegistry()
            .register(
                patch("memory.alpha", PatchRisk.SAFE).conflictsWith("memory.beta")
                    .build())
            .register(
                patch("memory.beta", PatchRisk.SAFE).conflictsWith("memory.alpha")
                    .build());

        PatchSnapshot snapshot = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());
        assertEnabled(snapshot, "memory.alpha");
        assertReason(snapshot, "memory.beta", PatchDecisionReason.CONFLICT);
        pass();
    }

    private void testExplicitEnableWinsConflictPriority() {
        PatchRegistry registry = new PatchRegistry()
            .register(
                patch("memory.alpha", PatchRisk.SAFE).conflictsWith("memory.beta")
                    .build())
            .register(
                patch("memory.beta", PatchRisk.SAFE).conflictsWith("memory.alpha")
                    .build());

        PatchSnapshot snapshot = registry.resolve(
            request(OptimizationProfile.COMPATIBLE).enable("memory.beta")
                .build());
        assertEnabled(snapshot, "memory.beta");
        assertReason(snapshot, "memory.alpha", PatchDecisionReason.CONFLICT);
        pass();
    }

    private void testDuplicateIdsAreRejected() {
        PatchRegistry registry = new PatchRegistry().register(patch("memory.same", PatchRisk.SAFE).build());
        expectIllegalArgument(new ThrowingAction() {

            @Override
            public void run() {
                registry.register(patch("memory.same", PatchRisk.SAFE).build());
            }
        });
        pass();
    }

    private void testUnknownConfigReferenceIsRejected() {
        final PatchRegistry registry = new PatchRegistry().register(patch("memory.known", PatchRisk.SAFE).build());
        expectIllegalArgument(new ThrowingAction() {

            @Override
            public void run() {
                registry.resolve(
                    request(OptimizationProfile.COMPATIBLE).enable("memory.unknown")
                        .build());
            }
        });
        pass();
    }

    private void testDiagnosticSnapshotIsStable() {
        PatchRegistry registry = new PatchRegistry().register(patch("z.patch", PatchRisk.SAFE).build())
            .register(
                patch("a.patch", PatchRisk.SAFE).defaultEnabled(false)
                    .build());

        PatchSnapshot snapshot = registry.resolve(request(OptimizationProfile.COMPATIBLE).build());
        assertEquals(
            "profile=COMPATIBLE enabled=1 disabled=1",
            snapshot.toDiagnosticLines()
                .get(0));
        assertTrue(
            snapshot.toDiagnosticLines()
                .get(1)
                .startsWith("[DISABLED:NOT_DEFAULT_ENABLED] a.patch"));
        assertTrue(
            snapshot.toDiagnosticLines()
                .get(2)
                .startsWith("[ENABLED] z.patch"));
        pass();
    }

    private static PatchDescriptor.Builder patch(String id, PatchRisk risk) {
        return PatchDescriptor.builder(id, id.substring(0, id.indexOf('.')))
            .risk(risk);
    }

    private static PatchResolutionRequest.Builder request(OptimizationProfile profile) {
        return PatchResolutionRequest.builder(profile);
    }

    private static void assertEnabled(PatchSnapshot snapshot, String id) {
        PatchDecision decision = snapshot.getDecision(id);
        assertTrue(decision != null && decision.isEnabled());
    }

    private static void assertReason(PatchSnapshot snapshot, String id, PatchDecisionReason reason) {
        PatchDecision decision = snapshot.getDecision(id);
        assertTrue(decision != null);
        assertEquals(reason, decision.getReason());
        assertTrue(!decision.isEnabled());
    }

    private static void expectIllegalArgument(ThrowingAction action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
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

    private interface ThrowingAction {

        void run();
    }
}
