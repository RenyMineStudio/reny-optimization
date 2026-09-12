# Environment Detection and Compatibility Manager

Reny centralizes compatibility policy in `dev.reny.optimization.compat`. Patches and mixins must not make ad-hoc `isModLoaded` decisions. A patch declares abstract overlapping features and required capabilities in its `PatchDescriptor`; the `CompatibilityManager` evaluates those constraints against one immutable `EnvironmentSnapshot`, and `PatchRegistry` records the resulting decision.

## Environment snapshot

`EnvironmentDetector.capture()` is best-effort and fail-soft. It records:

- physical client/dedicated-server side when FML exposes it;
- Java vendor/version;
- OS name/version/architecture;
- active FML mod IDs, names, and versions;
- synthetic OptiFine/LWJGL3ify presence when identifiable by well-known classes;
- renderer path (`VANILLA`, `ANGELICA`, `OPTIFINE`, `UNKNOWN`);
- shader path (`NONE`, `ANGELICA`, `OPTIFINE`, `UNKNOWN`);
- capabilities such as Forge/FML, Mixin, UniMixins, LWJGL3, modern OpenGL, and shaders.

Detection never performs network access. Missing bootstrap state or unavailable metadata becomes `UNKNOWN`/an absent capability rather than crashing the game.

## Compatibility states

- `COMPATIBLE`: no known restriction blocks the patch.
- `PARTIAL`: coexistence is allowed, but the detected environment changes assumptions and is reported in diagnostics.
- `REPLACED`: another mod already owns equivalent functionality; Reny's duplicate patch is disabled.
- `CONFLICT`: the patch would overlap an incompatible implementation or lacks a required environment capability; it is disabled.
- `UNKNOWN`: Reny cannot prove the compatibility envelope; the patch fails closed.

`COMPATIBLE` and `PARTIAL` decisions from the manager are accepted as environment proof for invasive patches. Without a compatibility manager, the pre-existing explicit precondition mechanism remains unchanged and invasive patches still fail closed.

## Initial known-mod policy

| Target | Detection | General state when present | Overlapping Reny behavior |
| --- | --- | --- | --- |
| Forge/FML | FML loader/capability | `COMPATIBLE` | Required platform. Missing capability disables patches rather than crashing. |
| UniMixins | active mod + Mixin capability | `COMPATIBLE` | Can satisfy `UNIMIXINS`/`MIXIN` capability requirements. |
| Angelica | active mod | `PARTIAL` | Reny renderer, chunk renderer, shader, texture, and OpenGL backend features are `REPLACED`. CPU/memory/tick/world work may coexist. |
| ArchaicFix | active mod | `PARTIAL` | Reny's overlapping lighting-engine feature is `REPLACED`. |
| FalseTweaks | active mod | `PARTIAL` | Overlapping chunk renderer is `REPLACED`; renderer/texture overlap is `PARTIAL`, or `UNKNOWN` for invasive patches when the FalseTweaks version is unknown. |
| OptiFine | active mod or class detection | `PARTIAL` | Equivalent renderer/chunk/shader/texture/OpenGL patches are `CONFLICT`; unrelated CPU/memory/tick patches may coexist. |
| LWJGL3ify | active mod or LWJGL3 class detection | `PARTIAL` | OpenGL-backend patches are `PARTIAL`; unknown LWJGL3ify versions are `UNKNOWN` for invasive backend patches. |

Angelica plus OptiFine is reported as a known renderer `CONFLICT`, but Reny does not deliberately hard-crash. The compatibility report explains the unsupported coexistence and individual Reny patches still fail closed according to their declared feature overlap.

## Patch metadata

Example:

```java
PatchDescriptor.builder("render.native", "renderer")
    .side(PatchSide.CLIENT)
    .risk(PatchRisk.NUCLEAR)
    .compatibilityFeature(CompatibilityFeature.RENDERER)
    .compatibilityFeature(CompatibilityFeature.OPENGL_BACKEND)
    .requiresCapability(EnvironmentCapability.MODERN_OPENGL)
    .build();
```

Feature tags express duplicate/overlapping functionality without naming third-party mods inside the patch or mixin. `CompatibilityManager` is the only place that maps those features to Angelica, ArchaicFix, FalseTweaks, OptiFine, LWJGL3ify, and future targets.

## Registry and diagnostics

Create the manager from the Forge-specific detector, then supply it to a resolution request:

```java
CompatibilityManager compatibility = new CompatibilityManager(EnvironmentDetector.capture());
PatchSnapshot snapshot = registry.resolve(
    PatchResolutionRequest.builder(OptimizationProfile.AGGRESSIVE)
        .compatibilityManager(compatibility)
        .build());
```

The policy/model classes deliberately do not depend on Forge. Only `EnvironmentDetector` imports FML classes, so the patch-registry core can still be compiled and tested directly on plain Java 8.

Disabled patches use stable reasons:

- `COMPATIBILITY_REPLACED`
- `COMPATIBILITY_CONFLICT`
- `COMPATIBILITY_UNKNOWN`

The `PatchDecision.detail` explains the exact detected mod/capability/side condition, and `CompatibilityManager.toDiagnosticLines()` exposes the environment plus the known-target matrix for the future `/opt compat` command and crash diagnostics.

## Validation

Run:

```bash
./gradlew compatibilitySelfTest
```

The self-test covers environment normalization/capabilities, registry integration, automatic environment proof for invasive patches, Angelica/ArchaicFix/FalseTweaks replacement rules, OptiFine conflict rules, LWJGL3ify partial/unknown-version behavior, required capabilities, physical-side gating, and inspectable diagnostics.
