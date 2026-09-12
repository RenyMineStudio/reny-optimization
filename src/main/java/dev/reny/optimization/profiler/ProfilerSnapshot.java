package dev.reny.optimization.profiler;

/** Immutable export-ready snapshot of profiler state. */
public final class ProfilerSnapshot {

    private final long createdAtMillis;
    private final boolean enabled;
    private final long currentFrameId;
    private final long currentTickId;
    private final DurationSeriesSnapshot frames;
    private final DurationSeriesSnapshot ticks;
    private final long[] frameThresholdCounts;
    private final SectionSnapshot[] sections;
    private final RuntimeSnapshot runtime;

    ProfilerSnapshot(long createdAtMillis, boolean enabled, long currentFrameId, long currentTickId,
        DurationSeriesSnapshot frames, DurationSeriesSnapshot ticks, long[] frameThresholdCounts,
        SectionSnapshot[] sections, RuntimeSnapshot runtime) {
        this.createdAtMillis = createdAtMillis;
        this.enabled = enabled;
        this.currentFrameId = currentFrameId;
        this.currentTickId = currentTickId;
        this.frames = frames;
        this.ticks = ticks;
        this.frameThresholdCounts = frameThresholdCounts;
        this.sections = sections;
        this.runtime = runtime;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getCurrentFrameId() {
        return currentFrameId;
    }

    public long getCurrentTickId() {
        return currentTickId;
    }

    public DurationSeriesSnapshot getFrames() {
        return frames;
    }

    public DurationSeriesSnapshot getTicks() {
        return ticks;
    }

    public long getFrameThresholdCount(int index) {
        return frameThresholdCounts[index];
    }

    public SectionSnapshot[] getSections() {
        return sections.clone();
    }

    public RuntimeSnapshot getRuntime() {
        return runtime;
    }

    public static final class SectionSnapshot {

        private final ProfilerSection section;
        private final long callCount;
        private final long totalNanos;
        private final long maxNanos;

        SectionSnapshot(ProfilerSection section, long callCount, long totalNanos, long maxNanos) {
            this.section = section;
            this.callCount = callCount;
            this.totalNanos = totalNanos;
            this.maxNanos = maxNanos;
        }

        public ProfilerSection getSection() {
            return section;
        }

        public long getCallCount() {
            return callCount;
        }

        public long getTotalNanos() {
            return totalNanos;
        }

        public long getMaxNanos() {
            return maxNanos;
        }
    }

    public static final class RuntimeSnapshot {

        private final long heapUsedBytes;
        private final long heapCommittedBytes;
        private final long gcCount;
        private final long gcTimeMillis;
        private final long queuedTasks;
        private final long activeTasks;
        private final long submittedTasks;
        private final long completedTasks;

        RuntimeSnapshot(long heapUsedBytes, long heapCommittedBytes, long gcCount, long gcTimeMillis, long queuedTasks,
            long activeTasks, long submittedTasks, long completedTasks) {
            this.heapUsedBytes = heapUsedBytes;
            this.heapCommittedBytes = heapCommittedBytes;
            this.gcCount = gcCount;
            this.gcTimeMillis = gcTimeMillis;
            this.queuedTasks = queuedTasks;
            this.activeTasks = activeTasks;
            this.submittedTasks = submittedTasks;
            this.completedTasks = completedTasks;
        }

        public long getHeapUsedBytes() {
            return heapUsedBytes;
        }

        public long getHeapCommittedBytes() {
            return heapCommittedBytes;
        }

        public long getGcCount() {
            return gcCount;
        }

        public long getGcTimeMillis() {
            return gcTimeMillis;
        }

        public long getQueuedTasks() {
            return queuedTasks;
        }

        public long getActiveTasks() {
            return activeTasks;
        }

        public long getSubmittedTasks() {
            return submittedTasks;
        }

        public long getCompletedTasks() {
            return completedTasks;
        }
    }
}
