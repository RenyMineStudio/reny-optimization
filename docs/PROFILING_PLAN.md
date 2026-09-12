# Profiling and Benchmark Plan

## 1. Objective

The profiling program exists to answer a concrete question for every performance problem:

> Which subsystem consumed the frame/tick budget, by how much, under which reproducible workload?

Average FPS is not sufficient. Reny prioritizes frame-time distribution, tick latency, allocation pressure, and workload-specific subsystem timing.

## 2. Measurement layers

Performance investigations are split into five layers:

1. **User-visible latency** — frames, stutter, world-load latency.
2. **Minecraft/Forge** — ticks, entities, TileEntities, chunks, lighting, events, rendering phases.
3. **JVM** — allocation, GC, JIT, locks, threads.
4. **CPU/OS** — cycles, IPC, cache behavior, scheduling, page faults.
5. **GPU/OpenGL** — draw calls, state changes, uploads, passes, GPU time where measurable.

A regression investigation should descend through these layers only as far as necessary.

## 3. Primary metrics

### Frame metrics

Record at minimum:

- frame time in milliseconds;
- FPS (derived/secondary);
- P50 frame time;
- P90 frame time;
- P95 frame time;
- P99 frame time;
- P99.9 frame time when sample size permits;
- 1% low FPS;
- 0.1% low FPS when sample size permits;
- maximum frame time;
- frame-time standard deviation;
- count/rate of frames above 16.67 ms;
- count/rate above 33.33 ms;
- count/rate above 50 ms;
- count/rate above 100 ms;
- count/rate above 250 ms.

### Tick metrics

- average MSPT;
- P50/P95/P99 MSPT;
- maximum MSPT;
- effective TPS;
- server/world tick time;
- entity time;
- TileEntity time;
- scheduled block update time;
- random tick time;
- lighting time;
- chunk-management time;
- Forge event time when instrumentable.

### Entity metrics

- loaded entity count;
- active entity count;
- ticked entity count;
- total entity tick time;
- time grouped by entity class/type;
- P95/P99 expensive entity tick where useful.

### TileEntity metrics

- loaded TileEntities;
- ticked TileEntities;
- skipped/dormant TileEntities if Reny introduces this concept;
- total TileEntity tick time;
- time grouped by class/mod;
- highest-cost classes/instances.

### Chunk metrics

- chunk loads/s;
- unloads/s;
- generated chunks/s;
- generation time;
- serialization/deserialization time;
- mesh build average/P95/P99;
- mesh upload average/P95/P99;
- pending rebuild queue length;
- maximum queue length.

### Lighting metrics

- light updates per tick/frame;
- sky-light updates;
- block-light updates;
- propagation work/nodes where measurable;
- queue size;
- duplicate/coalesced updates when supported;
- total lighting time and P95/P99.

### JVM/memory metrics

- heap used/committed;
- allocation rate (MB/s);
- minor/full GC counts;
- GC pause distribution;
- total GC time;
- largest allocation sources from external profiling;
- thread/lock contention when relevant.

### Rendering/GPU metrics

Where tooling permits:

- CPU frame time;
- GPU frame time;
- draw-call count;
- vertices/triangles submitted;
- texture binds;
- buffer binds/uploads;
- shader/program changes;
- framebuffer changes;
- significant OpenGL state changes;
- per-pass timing for shader pipelines.

## 4. Frame and tick correlation

Every internal sample should carry correlation identifiers where practical:

- `frame_id`
- `tick_id`

A long frame should therefore be attributable to simultaneous events such as:

```text
frame 19381 = 46.2 ms
  chunk rebuild = 10.8 ms
  lighting      = 6.4 ms
  TileEntities  = 8.1 ms
  GC pause      = 7.0 ms
```

Correlation is more useful than isolated averages when diagnosing stutter.

## 5. Canonical benchmark scenarios

### BENCH-01 — Stationary render

Fixed player position, camera, time, weather, and world state.

Purpose:

- renderer baseline;
- shader cost;
- static scene frame-time distribution.

### BENCH-02 — Chunk traversal

Move/fly through a fixed route at fixed speed.

Purpose:

- chunk loading;
- generation;
- mesh rebuild;
- lighting;
- GPU upload;
- I/O-induced stutter.

### BENCH-03 — Industrial base

A reproducible base with a controlled number of machines, pipes, cables, storage systems, and TileEntities.

Purpose:

- TileEntity ticking;
- Forge event pressure;
- network simulation from tech mods;
- render cost of complex blocks.

### BENCH-04 — Entity stress

Controlled arena containing large, reproducible entity populations.

Purpose:

- AI/tick cost;
- collision;
- entity rendering;
- item/entity allocation behavior.

### BENCH-05 — Lighting torture

Controlled large-scale block-light and sky-light updates.

Purpose:

- propagation algorithm;
- update queues;
- duplicate work;
- latency spikes.

### BENCH-06 — Shader torture

Scene intentionally combining expensive visual features:

- water;
- foliage;
- transparency;
- entities;
- particles;
- shadows;
- rain/weather where appropriate.

Purpose:

- shader-pass cost;
- CPU/GPU bottleneck separation;
- renderer/shader interaction.

### BENCH-07 — Dimension transition

Repeatable Overworld/dimension transitions.

Purpose:

- chunk/world initialization;
- I/O;
- memory spikes;
- GC;
- resource behavior.

### BENCH-08 — Startup/world load

Cold process start through first usable world frame.

Purpose:

- mod initialization;
- resources;
- class loading;
- world load;
- first-frame readiness.

### BENCH-99 — Monster

Intentionally unrealistic stress world combining very high TileEntity/entity counts, lighting changes, chunk streaming, fluids/particles, and shaders.

Purpose:

- reveal scaling limits;
- expose catastrophic algorithms;
- compare extreme-mode work.

BENCH-99 must not be used as the sole justification for behavior that harms normal gameplay.

## 6. Baseline matrix

At minimum, run core scenarios under:

| Configuration | Mods | Shaders | Reny |
|---|---:|---:|---:|
| A | minimal/vanilla-like | off | off |
| B | minimal/vanilla-like | on | off |
| C | heavy modpack | off | off |
| D | heavy modpack | on | off |
| A' | minimal/vanilla-like | off | on |
| B' | minimal/vanilla-like | on | on |
| C' | heavy modpack | off | on |
| D' | heavy modpack | on | on |

This prevents a patch that helps vanilla but hurts real modpacks from being mislabeled as universally beneficial.

## 7. CPU-bound versus GPU-bound tests

Use controlled resolution scaling such as:

- 854x480;
- 1280x720;
- 1920x1080;
- higher resolutions when hardware permits.

If frame time changes little with resolution, suspect CPU-side limits. If frame time scales strongly with pixel count, investigate GPU/shader cost.

For shaders, also test controlled shadow resolutions such as 512/1024/2048/4096 where supported.

## 8. Reproducibility protocol

Every benchmark result must record:

- Reny commit SHA;
- Minecraft version;
- Forge version;
- exact mod list/versions;
- configuration hashes or archived configs;
- shader/version/preset;
- Java distribution/version;
- JVM arguments;
- heap size/GC;
- OS/kernel;
- CPU;
- GPU;
- driver version;
- RAM;
- display resolution;
- render distance;
- FPS cap/VSync state;
- world/seed;
- player position/route;
- time/weather;
- benchmark duration;
- warmup duration.

## 9. Benchmark execution rules

### Uncapped performance runs

For throughput/headroom measurements:

- VSync off;
- FPS cap off.

Separate capped tests may be used to study frame pacing.

### Warmup

Default starting protocol:

- 60 seconds warmup;
- 120 seconds measured duration.

For long scenarios:

- 120 seconds warmup;
- 300 seconds measurement.

Adjust only when documented.

### Repetition

Initial standard: at least 5 independent runs for important comparisons.

Report:

- median;
- minimum;
- maximum;
- standard deviation or robust dispersion metric.

Do not publish only the best run.

### JVM isolation

For important before/after comparisons, restart Minecraft/JVM between runs to reduce contamination from heap state, JIT state, and caches. Randomize or alternate A/B order when thermal effects could bias results.

## 10. Result storage

Suggested layout:

```text
benchmarks/results/
  BENCH-02/
    <commit-sha>/
      environment.json
      summary.json
      frames.csv
      ticks.csv
      chunks.csv
      memory.csv
      gpu.csv
      profile-cpu.html
      profile-alloc.html
```

Large raw captures/traces should normally be stored as CI artifacts/releases/external benchmark artifacts rather than committed permanently to Git history.

## 11. Optimization acceptance criteria

Every performance patch should state its target metric(s).

Reject or revise a patch when it gains headline FPS by causing a material regression in:

- P99/P99.9 frame time;
- MSPT;
- allocation/GC;
- memory footprint;
- world correctness;
- compatibility.

Typical PR evidence:

```text
Before
  average FPS      109
  P99 frame        41 ms
  allocation       310 MB/s
  MSPT              24 ms

After
  average FPS      116
  P99 frame         18 ms
  allocation       141 MB/s
  MSPT              22 ms
```

The project should favor reductions in tail latency and wasted work over changes that only raise uncapped peak FPS.

## 12. First baseline campaign

The first formal dataset should run:

- BENCH-01 Stationary;
- BENCH-02 Chunk traversal;
- BENCH-03 Industrial base;
- BENCH-06 Shader torture;

across:

- minimal/vanilla-like;
- minimal + shaders;
- heavy modpack;
- heavy modpack + shaders.

With five runs per cell, this produces 80 runs and establishes the baseline ranking that should drive the first real optimization work.
