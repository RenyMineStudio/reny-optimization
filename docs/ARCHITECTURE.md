# Architecture

## 1. Purpose

Reny Optimization is designed as a modular performance runtime for Minecraft 1.7.10 rather than a collection of unrelated tweaks. Every optimization must be independently attributable, benchmarkable, and disableable when practical.

The default profile prioritizes compatibility. More invasive behavior is isolated behind explicit profiles.

## 2. Architectural layers

```text
Minecraft / Forge
      |
      v
Bootstrap
      |
      v
Environment + Compatibility Detection
      |
      v
Patch Registry
      |
      +-------------------------+
      |            |            |
      v            v            v
   Mixin     AccessTransformer  ASM
      |            |            |
      +------------+------------+
                   |
                   v
            Optimization Modules
                   |
     +-------------+-------------+
     |             |             |
     v             v             v
 Diagnostics      World        Rendering
 Memory/Tick   Chunks/IO      Mesh/Shaders
```

## 3. Core modules

### `bootstrap`

Responsible only for bringing Reny up safely:

- environment detection;
- side detection;
- patch registry initialization;
- mixin/coremod wiring;
- compatibility discovery;
- crash diagnostics.

The bootstrap layer must not contain actual performance optimizations.

### `patch`

Central registry for all modifications.

Every patch should eventually expose at least:

- stable patch ID;
- module;
- side (`CLIENT`, `SERVER`, `BOTH`);
- risk level;
- default state;
- required patches;
- conflicting patches;
- affected classes/subsystems;
- compatibility notes;
- benchmark(s) used to validate it.

Proposed risk levels:

- `SAFE` — expected to preserve observable vanilla/Forge semantics;
- `AGGRESSIVE` — changes internal assumptions while preserving normal gameplay behavior;
- `EXPERIMENTAL` — may expose mod compatibility problems;
- `NUCLEAR` — intentionally allows deep subsystem replacement.

### `compat`

Provides one place for mod/environment-specific decisions.

Compatibility states:

- `COMPATIBLE`
- `PARTIAL`
- `REPLACED`
- `CONFLICT`
- `UNKNOWN`

The compatibility layer should detect other optimization mods and disable duplicate/incompatible patches instead of attempting to win transformer ordering races.

Initial compatibility targets include:

- Forge/FML;
- UniMixins;
- Angelica;
- ArchaicFix;
- FalseTweaks;
- OptiFine;
- LWJGL3ify.

### `diagnostics`

Owns internal metrics and profiling hooks.

Initial metric groups:

- frame time;
- tick time;
- entity/TileEntity time;
- chunk load/generation/rebuild/upload;
- lighting work;
- allocation/GC observations;
- renderer counters;
- I/O timing.

### `memory`

Low-risk allocation and data-layout optimizations:

- temporary object elimination;
- collection hot paths;
- cache design;
- NBT allocations;
- repeated buffers;
- safe interning/deduplication.

### `tick`

Simulation-side hot paths:

- entities;
- TileEntities;
- scheduled block updates;
- random ticks;
- Forge event overhead;
- idle work elimination.

Adaptive or event-driven ticking belongs to aggressive/experimental profiles until proven compatible.

### `world`

World-state algorithms:

- block lookup;
- heightmaps;
- lighting;
- world caches;
- generation coordination.

### `chunk`

Chunk lifecycle and task scheduling:

- load/unload;
- generation;
- serialization/deserialization;
- mesh requests;
- task prioritization;
- background work coordination.

### `io`

I/O and compression work:

- region/chunk reads and writes;
- NBT compression/decompression;
- batching;
- buffer reuse;
- safe asynchronous preparation.

### `client.render`

Rendering architecture is intentionally split from simulation optimizations.

Planned areas:

- compatibility adapter;
- culling;
- terrain rendering;
- entity and TileEntity rendering;
- mesh building;
- GPU upload scheduling;
- shader integration;
- future modern OpenGL backend.

The first releases should coexist with modern 1.7.10 render projects where possible rather than immediately replacing them.

### `extreme`

Deep rewrites and parallel simulation live here until mature.

Examples:

- parallel entity processing;
- parallel TileEntity processing;
- asynchronous world subsystems;
- replacement schedulers;
- custom renderer/backend.

No `extreme` patch is enabled by default.

## 4. Optimization profiles

### `COMPATIBLE`

Default. Prioritizes broad mod compatibility and semantic safety.

### `AGGRESSIVE`

Enables optimizations that change internal execution behavior but are expected to work with mainstream packs.

### `NUCLEAR`

Opt-in research/performance mode. Allows subsystem replacement and explicitly accepts a narrower compatibility envelope.

## 5. Transformation policy

Prefer the least invasive mechanism capable of implementing a change:

1. existing Forge/FML hook;
2. AccessTransformer;
3. Mixin/MixinExtras;
4. targeted ASM transformer;
5. complete subsystem replacement.

Direct ASM must be isolated under the bytecode layer and must not become a general-purpose convenience mechanism.

## 6. Package layout

Initial target layout:

```text
src/main/java/.../reny/
  bootstrap/
  compat/
  config/
  diagnostics/
  patch/
  memory/
  tick/
  world/
  chunk/
  io/
  client/
    render/
    mesh/
    culling/
    shader/
  bytecode/
  extreme/
  util/
```

A single distributable JAR is preferred during early development. Internal Gradle subprojects may be introduced later if compile boundaries become useful.

## 7. Acceptance rule for optimizations

An optimization is not accepted solely because average FPS increases.

Each performance PR should identify:

1. the measured bottleneck;
2. a reproducible benchmark scenario;
3. before/after data;
4. compatibility impact;
5. memory/latency regressions, if any;
6. a rollback or feature flag strategy for non-trivial patches.

Tail latency and stability are first-class metrics. A patch that increases average FPS but substantially worsens P99 frame time is normally a regression.
