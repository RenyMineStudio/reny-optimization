# Patch Registry

The patch registry is the policy layer between environment/compatibility detection and the mechanism that implements an optimization.

It intentionally has no Minecraft, Forge, Mixin, ASM, or LWJGL dependencies.

## Patch metadata

Every `PatchDescriptor` has:

- stable lowercase ID;
- module/subsystem;
- side (`CLIENT`, `SERVER`, `BOTH`);
- risk (`SAFE`, `AGGRESSIVE`, `EXPERIMENTAL`, `NUCLEAR`);
- default enabled state;
- required patch IDs;
- conflicting patch IDs;
- affected targets;
- compatibility notes;
- benchmark IDs.

IDs are validated and duplicate registration is rejected.

## Profiles

Profiles are hard compatibility ceilings:

| Profile | Automatically eligible risks |
| --- | --- |
| `COMPATIBLE` | `SAFE` |
| `AGGRESSIVE` | `SAFE`, `AGGRESSIVE` |
| `NUCLEAR` | all risk levels |

An explicit patch enable does **not** bypass the selected profile. To run a nuclear patch, the user must first opt into the `NUCLEAR` profile.

`EXPERIMENTAL` patches are intentionally not auto-eligible in `AGGRESSIVE`; they belong to the research/nuclear envelope until promoted.

## Fail-closed preconditions

Any patch above `SAFE` requires a `SATISFIED` precondition result from the environment/compatibility layer before it can be enabled.

If the result is missing (`UNKNOWN`) or negative (`UNSATISFIED`), the patch stays disabled with an inspectable reason. This gives issue #6 a clean integration point without coupling this package to mod detection.

## Resolution order

A resolution pass is deterministic and uses stable patch IDs rather than registration order:

1. explicit disable;
2. profile ceiling;
3. default/explicit opt-in selection;
4. unsafe precondition proof;
5. missing dependency rejection;
6. dependency cycle rejection;
7. dependency-disable propagation;
8. deterministic conflict resolution;
9. dependency-disable propagation after conflicts.

### Conflict priority

When two eligible patches conflict, priority is:

1. explicitly enabled patch;
2. lower risk;
3. lexicographically smaller stable patch ID.

The rule is deliberately simple and reproducible. It can later be extended with explicit priority metadata only if profiling/compatibility work proves that necessary.

## Diagnostics

`PatchSnapshot` exposes every decision and a stable diagnostic representation suitable for the future `/reny patches` command.

Disabled reasons currently include:

- `EXPLICITLY_DISABLED`;
- `NOT_DEFAULT_ENABLED`;
- `PROFILE_RESTRICTED`;
- `PRECONDITION_UNKNOWN`;
- `PRECONDITION_UNSATISFIED`;
- `MISSING_DEPENDENCY`;
- `DISABLED_DEPENDENCY`;
- `DEPENDENCY_CYCLE`;
- `CONFLICT`.

## Tests before the Gradle scaffold

Issue #2 owns the Forge/RetroFuturaGradle project scaffold. Until it lands, the registry can still be compiled and tested using only a Java 8+ JDK:

```bash
sh tools/test-patch-registry.sh
```

The self-test covers profile selection, fail-closed unsafe patches, opt-in patches, dependencies, missing dependencies, cycles, conflicts, duplicate IDs, invalid config references, and stable diagnostics.
