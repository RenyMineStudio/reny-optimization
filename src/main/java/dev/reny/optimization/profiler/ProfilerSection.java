package dev.reny.optimization.profiler;

/** Built-in sections that later Minecraft/Forge hooks can time without dynamic lookup. */
public enum ProfilerSection {

    WORLD(ProfilerGroup.WORLD),
    ENTITY(ProfilerGroup.ENTITY),
    TILE_ENTITY(ProfilerGroup.TILE_ENTITY),
    CHUNK(ProfilerGroup.CHUNK),
    LIGHTING(ProfilerGroup.LIGHTING);

    private final ProfilerGroup group;

    ProfilerSection(ProfilerGroup group) {
        this.group = group;
    }

    public ProfilerGroup getGroup() {
        return group;
    }
}
