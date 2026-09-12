package dev.reny.optimization.benchmark;

/** Canonical reproducible benchmark scenarios defined by the profiling plan. */
public enum BenchmarkScenario {

    STATIONARY_RENDER("BENCH-01", "Stationary render", "Renderer baseline and static-scene frame-time distribution"),
    CHUNK_TRAVERSAL("BENCH-02", "Chunk traversal", "Chunk streaming, generation, lighting, meshing, upload, and I/O"),
    INDUSTRIAL_BASE("BENCH-03", "Industrial base",
        "TileEntity, Forge-event, machine-network, and complex-block pressure"),
    ENTITY_STRESS("BENCH-04", "Entity stress", "Entity AI, collision, allocation, ticking, and rendering"),
    LIGHTING_TORTURE("BENCH-05", "Lighting torture", "Large-scale block-light and sky-light propagation"),
    SHADER_TORTURE("BENCH-06", "Shader torture", "Expensive shader passes and CPU/GPU renderer interaction"),
    DIMENSION_TRANSITION("BENCH-07", "Dimension transition", "World initialization, I/O, memory spikes, and GC"),
    STARTUP_WORLD_LOAD("BENCH-08", "Startup/world load", "Process startup through the first usable world frame"),
    MONSTER("BENCH-99", "Monster", "Extreme combined scaling stress; never a sole normal-gameplay justification");

    private final String id;
    private final String displayName;
    private final String purpose;

    BenchmarkScenario(String id, String displayName, String purpose) {
        this.id = id;
        this.displayName = displayName;
        this.purpose = purpose;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPurpose() {
        return purpose;
    }

    public static BenchmarkScenario byId(String id) {
        for (BenchmarkScenario scenario : values()) {
            if (scenario.id.equalsIgnoreCase(id)) {
                return scenario;
            }
        }
        throw new IllegalArgumentException("Unknown benchmark ID: " + id);
    }
}
