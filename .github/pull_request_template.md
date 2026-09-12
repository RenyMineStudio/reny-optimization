## Summary

Describe the change and the specific problem it solves.

## Scope

- Subsystem(s):
- Patch ID(s), if applicable:
- Profile: `COMPATIBLE` / `AGGRESSIVE` / `NUCLEAR`
- Transformation mechanism: Forge hook / AT / Mixin / ASM / subsystem replacement / N/A

## Profiling evidence

What trace/profile identified the bottleneck or justified this work?

## Benchmark

- Scenario:
- Hardware/OS:
- Java/JVM flags:
- Mods/shaders:
- Warmup:
- Measurement duration:
- Independent runs:

### Before

| Metric | Result |
|---|---:|
| Average FPS | |
| Frame P95 | |
| Frame P99 | |
| Frame P99.9 | |
| MSPT | |
| Allocation MB/s | |
| Memory | |

### After

| Metric | Result |
|---|---:|
| Average FPS | |
| Frame P95 | |
| Frame P99 | |
| Frame P99.9 | |
| MSPT | |
| Allocation MB/s | |
| Memory | |

## Compatibility

List affected classes/mods, known conflicts, and fallback behavior.

## Correctness / safety

Explain how behavior was validated and whether the patch can be independently disabled.

## Checklist

- [ ] The target bottleneck is documented.
- [ ] The workload is reproducible.
- [ ] Tail-latency metrics were checked, not only average FPS.
- [ ] Compatibility impact is documented.
- [ ] Risky behavior is feature-gated.
- [ ] The least invasive reasonable transformation mechanism was used.
- [ ] No unrelated optimization/cleanup is mixed into this PR.
