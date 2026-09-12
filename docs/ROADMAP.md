# Roadmap

The roadmap is deliberately ordered by evidence and dependency. Reny should not begin with a renderer rewrite before it can reliably prove where time is being spent.

## Milestone 0.1 — Instrumented Core

Goal: create a trustworthy foundation for later optimization work.

Deliverables:

- RetroFuturaGradle/Forge 1.7.10 project bootstrap;
- Java 8 bytecode baseline;
- UniMixins configuration;
- minimal FML core plugin only if required by bootstrap;
- environment/mod detection;
- patch registry;
- compatibility manager;
- configuration profiles;
- internal timer/counter framework;
- frame/tick correlation IDs;
- `/reny` diagnostic command surface or equivalent;
- benchmark result schema;
- first reproducible baseline workloads.

Exit criteria:

- project builds reproducibly;
- client starts in a controlled dev environment;
- instrumentation overhead is measured;
- patches can be enabled/disabled independently;
- baseline results can be exported in machine-readable form.

## Milestone 0.2 — Allocation and low-risk CPU work

Goal: attack high-confidence hot paths without major semantic changes.

Candidate areas:

- temporary object allocation;
- collection hot paths;
- reusable buffers;
- NBT allocation/serialization overhead;
- duplicate caches/data;
- repeated lookups;
- avoidable Forge/event overhead identified by profiling.

Exit criteria:

- measurable allocation-rate reduction in at least one canonical workload;
- no material P99/MSPT regressions;
- compatible profile remains broad.

## Milestone 0.3 — Tick engine

Goal: reduce simulation work in heavy packs.

Areas:

- entity hot paths;
- TileEntity hot paths;
- scheduled updates;
- random ticks;
- event dispatch;
- detection of repeated/idle work.

Experimental work:

- conservative idle/dormant ticking;
- event-driven invalidation where compatibility allows.

## Milestone 0.4 — Chunks and I/O

Goal: reduce traversal/exploration stutter.

Areas:

- chunk load/unload;
- serialization/deserialization;
- region I/O;
- compression/decompression;
- queueing/prioritization;
- safe asynchronous preparation;
- task cancellation/deduplication.

## Milestone 0.5 — Lighting

Goal: reduce light-update cost and tail-latency spikes.

Areas:

- duplicate update elimination;
- queue/data-structure design;
- propagation hot paths;
- batching/coalescing;
- compatibility with existing lighting fixes.

Lighting algorithm replacement is allowed only after behavior/regression tests exist.

## Milestone 0.6 — Rendering compatibility and culling

Goal: improve render-side work without immediately replacing the complete renderer.

Areas:

- renderer capability detection;
- Angelica/OptiFine/FalseTweaks compatibility decisions;
- entity/TileEntity culling;
- chunk/section visibility;
- state-change instrumentation;
- draw-call and upload counters.

## Milestone 0.7 — Chunk mesh pipeline

Goal: move expensive terrain preparation out of latency-critical paths.

Areas:

- mesh task scheduler;
- worker pool;
- chunk build prioritization;
- buffer allocation/reuse;
- upload queue;
- stale-task cancellation;
- camera/distance-aware priority.

This milestone has a higher compatibility risk and should remain feature-gated.

## Milestone 0.8 — Reny renderer experiments

Goal: determine whether replacing existing 1.7.10 render backends is justified by data.

Research areas:

- persistent/efficient GPU buffers;
- terrain batching;
- modernized OpenGL state model;
- entity/TileEntity batching where valid;
- reduced state churn;
- shader pipeline abstraction.

Reny should coexist with established render optimization projects until its own backend demonstrates superior performance and acceptable compatibility.

## Milestone 0.9 — Shader pipeline

Goal: optimize the complete shader-heavy path.

Areas:

- per-pass profiling;
- shadow-pass scaling;
- framebuffer lifecycle;
- shader state/uniform overhead;
- redundant work elimination;
- compatibility layer for shader packs.

## Milestone 1.0 — Stable performance runtime

Candidate release criteria:

- reproducible benchmark suite;
- documented compatibility matrix;
- stable `COMPATIBLE` profile;
- automated regression checks for correctness;
- proven wins in heavy modpack + shader workloads;
- no known catastrophic world-corruption paths;
- clear separation between stable and experimental/nuclear features.

## Experimental / Nuclear track

This track is intentionally decoupled from normal milestone completion.

Research subjects:

- parallel entity processing;
- parallel TileEntity processing;
- asynchronous world algorithms;
- replacement chunk scheduler;
- replacement lighting engine;
- custom modern renderer;
- deeper Forge/event execution changes.

A nuclear optimization may be valuable research even when it is not suitable for the default profile.
