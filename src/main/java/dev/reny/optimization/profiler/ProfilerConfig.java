package dev.reny.optimization.profiler;

/** Mutable low-cost profiler configuration backed by volatile primitives. */
public final class ProfilerConfig {

    private volatile boolean enabled;
    private volatile long enabledGroupsMask;

    public ProfilerConfig() {
        enabled = Boolean.parseBoolean(System.getProperty("reny.profiler.enabled", "true"));
        long mask = 0L;
        for (ProfilerGroup group : ProfilerGroup.values()) {
            String property = "reny.profiler.group." + group.name().toLowerCase();
            if (Boolean.parseBoolean(System.getProperty(property, "true"))) {
                mask |= bit(group);
            }
        }
        enabledGroupsMask = mask;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGroupEnabled(ProfilerGroup group) {
        return enabled && (enabledGroupsMask & bit(group)) != 0L;
    }

    public void setGroupEnabled(ProfilerGroup group, boolean enabled) {
        long bit = bit(group);
        long current = enabledGroupsMask;
        enabledGroupsMask = enabled ? current | bit : current & ~bit;
    }

    private static long bit(ProfilerGroup group) {
        return 1L << group.ordinal();
    }
}
