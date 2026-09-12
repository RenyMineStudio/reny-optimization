# Internal Profiler

Reny's internal profiler provides Minecraft-aware timing and correlation without requiring an external profiler for every investigation.

## Design constraints

Hot-path collection follows four rules:

1. recording uses fixed-size primitive ring buffers;
2. frame/tick recording does not allocate per event;
3. section timers are indexed by enums rather than dynamic maps;
4. sorting, percentiles, MXBean reads, and file I/O happen only when a snapshot/export is requested.

Instrumentation can be disabled globally or by group. The disabled path checks volatile primitive state and returns `InternalProfiler.DISABLED_TOKEN` without calling `System.nanoTime()`.

Frame/tick ring buffers use a single-writer publication sequence. Snapshots retry if they observe a write in progress or if the buffer changes while being copied, so exported samples are never assembled from a partially published record.

## IDs and correlation

Every measured frame receives a monotonically increasing `frame_id`. Every measured simulation/server tick receives a monotonically increasing `tick_id`.

`frames.csv` stores the most recently visible `tick_id` when the frame completed. `ticks.csv` stores the most recently visible `frame_id` when the server tick completed. These IDs let later chunk, lighting, GC, and section data be related to the same time interval without forcing the render and integrated-server threads through a synchronized correlation map.

The initial Forge hooks measure:

- `RenderTickEvent` for client frametime;
- `ServerTickEvent` for simulation MSPT on both dedicated and integrated servers.

Client ticks are deliberately not reported as MSPT. This keeps `tick_id` tied to the game simulation rather than mixing client-loop and server-loop durations in one series. Deeper client-specific timing can be added later as a separate metric series.

## Rolling windows and percentiles

The default in-memory windows retain the newest:

- 8192 frame samples;
- 2048 tick samples.

Snapshots calculate nearest-rank P50, P95, P99, and P99.9, plus min/mean/max. `totalSamples` remains monotonic even after the ring overwrites old samples.

Frame threshold counters are lifetime counters for frames above:

- 16.67 ms;
- 33.33 ms;
- 50 ms;
- 100 ms;
- 250 ms.

## Runtime metrics

Snapshot/export reads runtime state outside hot paths:

- heap used and committed;
- cumulative GC count and GC time;
- queued/active task gauges;
- submitted/completed task counters.

Future schedulers should update the task values through `InternalProfiler` rather than introducing separate metric registries.

## Section timing API

Future patch modules can instrument known subsystems without allocation:

```java
long token = InternalProfiler.get().startSection(ProfilerSection.CHUNK);
try {
    // work
} finally {
    InternalProfiler.get().endSection(ProfilerSection.CHUNK, token);
}
```

If the profiler or the section's group is disabled, `startSection` returns a sentinel and `endSection` becomes a cheap no-op.

Initial sections are WORLD, ENTITY, TILE_ENTITY, CHUNK, and LIGHTING.

## Export

`InternalProfiler.exportNow(directory)` creates a timestamped snapshot directory containing:

- `summary.json` — aggregate frame/tick percentiles, threshold counts, heap/GC/task state;
- `frames.csv` — retained frame IDs, correlated tick IDs, duration ns/ms;
- `ticks.csv` — retained tick IDs, correlated frame IDs, duration ns/ms;
- `sections.csv` — section calls, total time, and max duration.

The mod installs a best-effort JVM shutdown export under `config/reny-profiler/` while profiling is enabled. Export failures during JVM shutdown are intentionally ignored so they cannot block game termination.

## Configuration

The initial control surface uses JVM system properties and the runtime API:

```text
-Dreny.profiler.enabled=false
-Dreny.profiler.group.chunk=false
-Dreny.profiler.group.lighting=false
```

Runtime code may also call `ProfilerConfig#setEnabled` and `setGroupEnabled`.

A user-facing Forge config/command layer can be added later without changing the collection core.

## Validation

Run the dependency-free profiler self-test:

```bash
./gradlew profilerSelfTest
```

`check` runs this automatically.

### Overhead benchmark

Run:

```bash
./gradlew profilerOverheadBenchmark
```

The benchmark performs warmup followed by 5,000,000 operations in two modes:

1. profiler globally disabled;
2. profiler enabled with a representative CHUNK section timer.

It prints nanoseconds per operation. Results are intentionally informational rather than a CI threshold because VM scheduling, CPU power states, and GitHub-hosted runners make strict microbenchmark thresholds unreliable.

For before/after profiler-cost studies, keep the JVM, hardware, power profile, and iteration count identical and record at least five runs. The performance baseline campaign should additionally compare full Minecraft runs with profiling disabled, enabled without extra section hooks, and enabled with representative instrumentation.
