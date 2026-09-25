# Known limitations

The execution gate is complete, but the following limitations remain explicit
and must not be converted into stronger claims in the final report:

- The formal matrix is complete at 80/80 accepted runs and 16/16 cells. The
  separate pilot is not part of the formal count.
- BENCH-03 and BENCH-06 in the minimal profile are labeled proxies. The
  minimal world does not contain a constructed machine network or TileEntity
  farm, and it is not equivalent to the heavy industrial scene.
- The current runtime exports frame/tick series and JVM/GC deltas. Section rows
  exist, but active Forge/entity/TileEntity/chunk/lighting hooks are not yet
  enabled; attribution to any of those subsystems is therefore `UNPROVEN`.
- Representative external CPU profiles (via async-profiler 3.0) and GPU
  clock/driver telemetry are now collected under `evidence/`. They show that in
  minimal shader-on rendering, Mesa Gallium driver (`libgallium`) consumes ~45.3%
  of render-thread CPU time (uniform updates, texture bindings, buffer swaps),
  while in shader-off it consumes ~28.1% with the GPU staying below max clock
  (mean ~506 MHz vs 1,300 MHz ceiling). Full GPU hardware shader-stage pipeline
  attribution remains bounded by what unprivileged Mesa sysfs exposes.
- The exact Sildur pack loaded in the isolated heavy shader boot and entered a
  world, but legacy shader block mappings and missing texture warnings remain
  recorded compatibility noise.
- A delayed exporter write was observed once after the COMPLETE log. The runner
  now waits for all four export files before validation; the original rejected
  event is retained and the late-created export is revalidated separately.
- The campaign log retains 29 rejected attempts/exports, including startup,
  world-entry, recovery-stop, one pause-menu-contaminated run, and the original
  delayed-export validation event. None is included in the 80 accepted formal
  runs; see `rejections.json` for the exact IDs and reasons.
- Forge/OptiFine update-check socket failures and the known nonfatal UniMixins
  crash-enhancer warning are environment noise, not proof of a performance
  bottleneck.
