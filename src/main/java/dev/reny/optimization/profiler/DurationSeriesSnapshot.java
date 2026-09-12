package dev.reny.optimization.profiler;

/** Immutable chronological copy of a fixed-size duration window. */
public final class DurationSeriesSnapshot {

    private final long totalSamples;
    private final long[] ids;
    private final long[] correlationIds;
    private final long[] durationsNanos;
    private final DurationStatistics statistics;

    DurationSeriesSnapshot(long totalSamples, long[] ids, long[] correlationIds, long[] durationsNanos) {
        this.totalSamples = totalSamples;
        this.ids = ids;
        this.correlationIds = correlationIds;
        this.durationsNanos = durationsNanos;
        this.statistics = DurationStatistics.from(durationsNanos);
    }

    public long getTotalSamples() {
        return totalSamples;
    }

    public int size() {
        return durationsNanos.length;
    }

    public long getId(int index) {
        return ids[index];
    }

    public long getCorrelationId(int index) {
        return correlationIds[index];
    }

    public long getDurationNanos(int index) {
        return durationsNanos[index];
    }

    public DurationStatistics getStatistics() {
        return statistics;
    }
}
