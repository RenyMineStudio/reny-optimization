# Contributing to Reny Optimization

Reny is performance software. Correctness, reproducibility, and compatibility are part of performance work, not separate concerns.

## General rules

- Keep changes scoped to a clear subsystem or measurable problem.
- Prefer the least invasive patching mechanism that solves the problem.
- Do not add speculative optimizations to hot paths without evidence.
- Do not trade severe P99/P99.9 regressions for higher average FPS.
- Do not silently narrow compatibility; document it and feature-gate risky behavior.
- Avoid unrelated cleanup in performance PRs because it makes profiling comparisons harder.

## Performance change checklist

A performance PR should answer:

1. What bottleneck does this change target?
2. Which profile/trace identified it?
3. Which canonical benchmark reproduces it?
4. What are the before/after results?
5. What is the effect on frame P95/P99/P99.9?
6. What is the effect on MSPT when relevant?
7. What is the effect on allocation/GC and memory when relevant?
8. Which classes/mods/subsystems are transformed?
9. What compatibility risk exists?
10. Can the optimization be disabled independently?

## Benchmark evidence

For meaningful performance claims, include enough information to reproduce the result:

- commit SHA;
- JVM/version and JVM arguments;
- hardware and OS;
- mod list and shader setup;
- benchmark scenario;
- warmup/measurement duration;
- number of runs;
- median and tail-latency metrics.

Five independent runs are the initial default for important before/after comparisons.

## Transformation policy

Preferred order:

1. Forge/FML hook;
2. AccessTransformer;
3. Mixin/MixinExtras;
4. targeted ASM;
5. subsystem replacement.

Direct ASM must live under the dedicated bytecode layer and fail closed when expected bytecode does not match.

## Patch identity

Every non-trivial optimization should have a stable patch ID and document:

- module;
- side;
- risk level;
- dependencies/conflicts;
- compatibility notes;
- benchmark coverage.

## Profiles

- `COMPATIBLE`: default; broad compatibility first.
- `AGGRESSIVE`: deeper internal changes with expected mainstream compatibility.
- `NUCLEAR`: opt-in research mode; narrower compatibility is acceptable when documented.

Experimental/nuclear behavior must never silently become part of the compatible profile.

## Commit hygiene

Prefer small commits with a clear purpose. Documentation and benchmark fixtures may be committed separately from the implementation when useful.
