# Follow-up implementation issues

The following bounded implementation issues have been formally created on GitHub
to address the top bottlenecks confirmed by Issue #7 and supported by the
representative CPU and GPU profiling evidence:

## Issue #20 — [Instrument render/shader pass phases and optimize OpenGL pipeline overhead](https://github.com/RenyMineStudio/reny-optimization/issues/20)

**Measured problem:** Shader-on rendering is the single largest frame-budget
regressor. In minimal BENCH-01, frame P95 degrades from 4.21 ms to >52 ms with
shaders enabled. External CPU profiling (`async-profiler 3.0` via ITIMER_PROF,
`evidence/B_BENCH-01_cpu_hotspots.txt`) proves that **45.29% of render-thread
CPU time** is consumed inside the OpenGL driver (`libgallium`), heavily dominated
by uniform state manipulation, texture management, `glDrawBuffers`, and
buffer-swap synchronizations.

**Intended metric:** Per-phase render/shader timing (shadows, gbuffers,
composites, final), frame P95/P99/P99.9 latency, and frame threshold reductions
(>16.67 ms, >33.33 ms, >50 ms).

**Implementation surface:** Legitimate instrumentation of shader pass boundaries,
uniform state caching, and draw-call overhead reduction in the OptiFine render
pipeline.

**Compatibility considerations:** Must maintain strict backward compatibility
with OptiFine 1.7.10 HD U E7 and standard shaderpacks (`Sildur's Enhanced
Default v1.19 Fast.zip`). Must fail-closed if unexpected bytecode is encountered.

**Acceptance benchmark:** 5 independent runs of BENCH-01 and BENCH-06 under
Configurations A, B, C, D using the established 1280×720 protocol.

---

## Issue #21 — [Attribute and optimize heavy-pack tick latency by subsystem (TileEntity, entity, and chunk lifecycle)](https://github.com/RenyMineStudio/reny-optimization/issues/21)

**Measured problem:** The heavy reference modpack (The Reawakening, 72 loaded FML
mods) increases tick P95 across all canonical scenarios compared to the minimal
baseline (an increase of 2.77–5.70 ms even in stationary workloads). External CPU
profiling shows significant main/server thread time spent in
`ChunkProviderServer.func_73158_c`, `World.func_72939_s`, and entity ticking.
Without dedicated section hooks, exact attribution among TileEntities, entity
ticking, chunk lifecycle, and Forge event dispatch remains `UNPROVEN`.

**Intended metric:** Tick P50/P95/P99 latency, MSPT distribution, and
sub-millisecond section execution timings for TileEntities, entities, chunks, and
Forge events.

**Implementation surface:** Low-overhead profiler section instrumentation around
`MinecraftServer` / `WorldServer` tick loops, entity dispatch, TileEntity
updates, and chunk I/O.

**Compatibility considerations:** Must preserve complete compatibility with the
72 FML mods documented in `heavy-mod-manifest.json`. Must not alter event dispatch
ordering or block entity lifecycles.

**Acceptance benchmark:** 5 independent runs of BENCH-02 (chunk traversal) and
BENCH-03 (industrial base) on configurations C and D, confirming valid subsystem
timings in exported summaries.

---

## Issue #22 — [Correlate allocation rate and garbage collection pauses with frame/tick tail latency](https://github.com/RenyMineStudio/reny-optimization/issues/22)

**Measured problem:** Heavy profile runs exhibit elevated garbage collection
activity (up to 243 ms median GC time per 120-second measurement window, with
run-level peaks up to 679 ms). Currently, runtime metrics capture aggregate
`gc_count` and `gc_time_ms`, but do not record individual timestamped pause
intervals aligned with frame deltas, leaving causal attribution of specific P99/P99.9
frame spikes to GC pauses unproven.

**Intended metric:** Individual GC pause start/end timestamps and durations,
frame P99/P99.9 spike correlation index (% of >50 ms frames coinciding with a GC
pause), and heap allocation rate.

**Implementation surface:** Passive JVM `GarbageCollectorMXBean` notification
listeners and JMX/JFR integration, exporting timestamped pause events alongside
`frames.csv` and `ticks.csv`.

**Compatibility considerations:** Passive telemetry only; do not alter JVM
memory flags, collector choice, or object allocations. Ensure zero allocation
overhead on render and server tick hot paths.

**Acceptance benchmark:** Representative runs of BENCH-01, BENCH-02, and
BENCH-06 under configurations C and D, demonstrating aligned pause/frame tail
correlation.
