# Benchmark Harness

Reny's benchmark harness turns the internal profiler into reproducible, machine-readable performance runs. It is deliberately local-only: no cloud service, telemetry endpoint, or network dependency is required.

## Schema

The initial benchmark schema is **version 1**. Every run writes exactly these baseline files:

```text
benchmarks/results/
  <BENCH-ID>/
    <reny-commit-sha>/
      <run-id>/
        environment.json
        summary.json
        frames.csv
        ticks.csv
```

`run-id` combines a local timestamp with a random suffix. The exporter refuses to reuse an existing final directory, so repeated runs cannot overwrite earlier data.

`benchmarks/results/` is ignored by Git. Small curated summaries may be committed deliberately elsewhere; large traces should remain build/CI artifacts.

## Phase model

A session has four explicit states:

1. `CREATED`
2. `WARMUP`
3. `MEASURING`
4. `COMPLETE`

Warmup is never removed by resetting the global profiler. Instead, `beginMeasurement()` captures the latest monotonic `frame_id` and `tick_id`. Export includes only samples with later IDs. This lets the profiler remain useful to other diagnostics while guaranteeing that retained warmup frames/ticks do not enter benchmark statistics.

The configured durations are guidance for the runner. `shouldBeginMeasurement()` and `shouldFinishMeasurement()` use them without silently changing phases; the caller performs the transition explicitly.

Typical use:

```java
BenchmarkContext context = BenchmarkContext.builder()
    .display(1920, 1080, 12, false, 0)
    .shader("Complementary", "5.x", "High")
    .world("seed", "BENCH-06 world", "fixed camera", "rain-night")
    .configHash("sha256:...")
    .build();

BenchmarkSession session = new BenchmarkSession(
    InternalProfiler.get(),
    BenchmarkScenario.SHADER_TORTURE,
    context,
    60_000L,
    120_000L,
    new File("benchmarks/results"));

session.startWarmup();
// When session.shouldBeginMeasurement() becomes true:
session.beginMeasurement();
// When session.shouldFinishMeasurement() becomes true:
File runDirectory = session.finish();
```

No scenario automation is implied yet. BENCH-01 through BENCH-99 describe the reproducible workload; later integrations can drive camera paths, world actions, or commands around this state machine.

## environment.json

The environment file records:

- schema/run/benchmark identity;
- Reny version and commit SHA;
- Minecraft and Forge versions;
- active FML mod IDs/names/versions when FML is initialized;
- Java vendor/version/VM and JVM arguments;
- OS name/version/architecture and kernel where discoverable;
- CPU, processor count, physical RAM where discoverable;
- OpenGL vendor/renderer/version and driver hint where an OpenGL context is active;
- display resolution, render distance, VSync and FPS cap supplied by the benchmark context;
- shader name/version/preset;
- world seed/descriptor/route/time-weather/config hash;
- arbitrary explicit `extras`;
- relative paths to detailed sample files.

Hardware discovery is best-effort and never performs a network lookup. Unknown values stay explicit rather than being guessed.

### Commit SHA discovery

The harness resolves the Reny commit in this order:

1. `-Dreny.commit.sha=<sha>`;
2. `RENY_COMMIT_SHA`;
3. `GITHUB_SHA`;
4. local `.git/HEAD` when running from a checkout;
5. `unknown`.

For packaged benchmark releases, set `reny.commit.sha` or `RENY_COMMIT_SHA` when launching if the build process does not already provide Git metadata.

### Hardware overrides

When automatic discovery is unavailable, the following local JVM properties can make a run explicit:

```text
-Dreny.benchmark.cpu=...
-Dreny.benchmark.gpu=...
-Dreny.benchmark.gpu.vendor=...
-Dreny.benchmark.driver=...
-Dreny.benchmark.opengl=...
-Dreny.benchmark.ram.bytes=...
```

Minecraft and Forge target metadata can similarly be overridden with `reny.benchmark.minecraft` and `reny.benchmark.forge` for diagnostic builds.

## summary.json

Summary data is computed only from the measured window. Frame statistics include:

- sample count;
- min/mean/max;
- standard deviation;
- P50/P90/P95/P99/P99.9;
- average FPS;
- FPS-equivalent 1% low and 0.1% low;
- measured counts/rates above 16.67, 33.33, 50, 100, and 250 ms.

Tick statistics include min/mean/max, standard deviation, P50/P90/P95/P99/P99.9, and effective TPS capped at 20 TPS.

Runtime deltas include measured-window heap start/end/delta, GC count/time, and submitted/completed task deltas.

The summary also records configured and actual warmup/measurement durations and wall-clock timestamps.

## CSV files

`frames.csv`:

```text
frame_id,tick_id,duration_ns,duration_ms
```

`ticks.csv`:

```text
tick_id,frame_id,duration_ns,duration_ms
```

IDs preserve correlation with future chunk/lighting/entity subsystem exports.

## Canonical scenarios

The authoritative code descriptors live in `BenchmarkScenario`; a machine-readable catalog is also stored at `benchmarks/scenarios/catalog.json`.

- BENCH-01 Stationary render
- BENCH-02 Chunk traversal
- BENCH-03 Industrial base
- BENCH-04 Entity stress
- BENCH-05 Lighting torture
- BENCH-06 Shader torture
- BENCH-07 Dimension transition
- BENCH-08 Startup/world load
- BENCH-99 Monster

BENCH-99 is intentionally pathological and must not be the sole evidence for changes that hurt normal gameplay.

## Validation

Run:

```bash
./gradlew benchmarkHarnessSelfTest
```

The self-test verifies canonical descriptors, measured statistics, warmup exclusion, schema fields, required files, unique run directories, and phase-state validation. `check` includes this task automatically.

For formal comparisons, continue following `docs/PROFILING_PLAN.md`: restart the JVM for important A/B runs, use at least five independent runs, preserve exact settings/config hashes, and do not report only the best run.
