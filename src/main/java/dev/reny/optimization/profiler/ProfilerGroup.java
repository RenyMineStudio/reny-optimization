package dev.reny.optimization.profiler;

/** Logical groups that can be toggled independently without changing call sites. */
public enum ProfilerGroup {
    FRAME,
    TICK,
    WORLD,
    ENTITY,
    TILE_ENTITY,
    CHUNK,
    LIGHTING,
    RUNTIME,
    TASK
}
