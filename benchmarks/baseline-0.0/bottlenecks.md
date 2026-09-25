# Bottleneck ranking

**Status: CONFIRMED at the workload-effect level; PARTIAL/UNPROVEN at the
subsystem level.** This ranking is derived from the 80 accepted formal runs in
`aggregate.json`, using nearest-rank percentiles over the concatenated
measurement-only CSV samples. It does not add frame, tick, and GC clocks into a
single score.

## 1. Shader-enabled render path — confirmed first target

**Evidence.** Shader-on versus shader-off changes frame time substantially while
not producing a corresponding tick-time increase in the minimal profile:

- Minimal BENCH-01, B versus A: frame P95 **4.747 → 52.131 ms** (+47.383
  ms), P99 **7.659 → 63.284 ms**, and P99.9 **16.127 → 76.115 ms**. The
  shader-on cell has **37.603%** of frame samples above 33.33 ms, versus
  effectively zero in A.
- Heavy BENCH-06, D versus C: frame P95 **19.499 → 36.997 ms** (+17.498
  ms), P99.9 **30.276 → 54.335 ms**, and the >33.33 ms rate **0.053% →
  15.565%**. Tick P95 changes only **9.287 → 10.729 ms** (+1.442 ms).
- The worst overall tail is B/BENCH-01; the worst heavy shader tail is
  D/BENCH-06. The effect is present across all four scenario pairs, not only
  one outlier.

**Classification and confidence.** The measured effect is render-side because
the frame distribution moves much more than the tick distribution. External CPU
profiles (`async-profiler 3.0` via ITIMER_PROF, `evidence/B_BENCH-01_cpu_hotspots.txt`)
and GPU clock telemetry (`evidence/B_BENCH-01_gpu_telemetry.json`) provide concrete
evidence: Mesa Gallium driver (`libgallium`) overhead increases to **45.29% of
render-thread CPU time** with shaders enabled (texture swaps, uniform updates,
`glDrawBuffers`, `dri_flush`), while GPU clock telemetry indicates that hardware
rasterization demand scales up significantly. Confidence is **high** for
shader-induced frame cost and render-thread/driver interaction. Compatibility
risk is **high** because the path crosses legacy Forge, OptiFine, and shaderpacks.

**Recommended milestone / Follow-up issue.** Add phase/timer attribution for
shader passes, chunk rebuild/upload, and render-thread work; evaluate OpenGL
state caching and draw-call reduction. Tracked in implementation issue
**#20** ([Instrument render/shader pass phases and optimize OpenGL pipeline overhead](https://github.com/RenyMineStudio/reny-optimization/issues/20)).

## 2. Heavy-pack tick and runtime workload — confirmed effect, subsystem unproven

**Evidence.** With shaders off, moving from minimal A to heavy C raises frame
P95 by **15.440–17.936 ms** across the four scenarios and raises tick P95 by
**2.766–5.698 ms**. The heavy cells also show substantially more runtime
activity: median GC time is **97–155 ms per 120 s** in C versus **10–21 ms** in
A, and median GC counts are **7–13** versus **1–2**. With shaders on, heavy D
has tick P95 values from **10.643 to 11.021 ms** and frame P95 values from
**34.315 to 36.997 ms**.

This proves a heavy-profile workload effect, not a TileEntity or chunk diagnosis.
The minimal BENCH-03/BENCH-06 cells are explicitly proxies, and the current
runtime has no active Forge, entity, TileEntity, chunk, or lighting hooks.

**Classification and confidence.** The tick component is CPU/JVM-side at the
measurement level; the responsible mod/subsystem is **UNPROVEN**. Confidence is
**high** for the configuration delta and **low** for a specific optimization
target. Compatibility risk is **high** because the heavy copy contains the
complete local mod set and legacy integrations.

**Recommended milestone / Follow-up issue.** Add low-overhead attribution sections for chunk
streaming/generation, rebuild/upload, entity ticking, TileEntity ticking, Forge
events, and lighting. Use the same saved heavy snapshot and require that section
timings explain the tick/frame deltas before changing behavior. Tracked in implementation
issue **#21** ([Attribute and optimize heavy-pack tick latency by subsystem](https://github.com/RenyMineStudio/reny-optimization/issues/21)).

## 3. Allocation and GC pressure — confirmed signal, not yet a proven frame cause

**Evidence.** Heavy cells consistently record higher GC activity than minimal
cells. The largest median GC time is **243 ms** in D/BENCH-06; D/BENCH-02 has a
run-level GC-time maximum of **679 ms**. Heavy cells also show large and variable
heap deltas, including multi-gigabyte maxima in some five-run cells.

The evidence is sufficient to prioritize allocation/GC correlation, but not to
claim that GC caused the frame tails: the exporter records aggregate runtime
deltas rather than timestamp-aligned GC pauses and frame samples. B/BENCH-01,
for example, has the worst frame tail while its median GC time is only 14 ms.

**Classification and confidence.** JVM allocation/GC signal: **confirmed**;
causal frame attribution: **low confidence**. Compatibility risk is **medium to
high** for pooling or lifecycle changes in a legacy modded JVM.

**Recommended milestone / Follow-up issue.** Add timestamp-aligned GC/allocation evidence or a
safe representative external profile, then test only evidence-backed changes
against frame and tick tails. Do not treat heap delta as allocation rate or add
GC time to frame/tick time. Tracked in implementation issue **#22** ([Correlate allocation rate and garbage collection pauses with frame/tick tail latency](https://github.com/RenyMineStudio/reny-optimization/issues/22)).

## Not ranked from this campaign

Chunk generation/rebuild, mesh upload, TileEntity ticking, entity ticking,
Forge-event overhead, lighting, and pure GPU saturation are not individually
ranked. The current section rows do not contain active subsystem hooks, and no
external CPU/GPU trace was collected. The absence of a ranking for those items
is an evidence boundary, not evidence that their cost is zero.

## Reproducibility

The source data is `aggregate.csv` and `aggregate.json`, generated by
`aggregate.py` from terminal `VALID` events for commit
`30d88b554addd4a610d6fe1efd319bda4b677635`. Rejected attempts remain in
`rejections.json`; no rejected export is used in the ranking.
