package dev.reny.optimization.profiler;

/**
 * Single-writer fixed-size ring buffer for duration samples.
 *
 * <p>Recording allocates nothing. Snapshots allocate and may retry if a writer publishes while data is copied.</p>
 */
final class DurationSeries {

    private final long[] ids;
    private final long[] correlationIds;
    private final long[] durationsNanos;
    private int cursor;
    private int size;
    private long totalSamples;
    private volatile long publishedVersion;

    DurationSeries(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        ids = new long[capacity];
        correlationIds = new long[capacity];
        durationsNanos = new long[capacity];
    }

    void record(long id, long correlationId, long durationNanos) {
        int index = cursor;
        ids[index] = id;
        correlationIds[index] = correlationId;
        durationsNanos[index] = durationNanos;
        cursor = (index + 1) % durationsNanos.length;
        if (size < durationsNanos.length) {
            size++;
        }
        totalSamples++;
        publishedVersion = totalSamples;
    }

    DurationSeriesSnapshot snapshot() {
        for (int attempt = 0; attempt < 3; attempt++) {
            long before = publishedVersion;
            int localCursor = cursor;
            int localSize = size;
            long localTotalSamples = totalSamples;
            DurationSeriesSnapshot snapshot = copy(localCursor, localSize, localTotalSamples);
            if (before == publishedVersion) {
                return snapshot;
            }
        }

        return copy(cursor, size, totalSamples);
    }

    private DurationSeriesSnapshot copy(int localCursor, int localSize, long localTotalSamples) {
        long[] copiedIds = new long[localSize];
        long[] copiedCorrelations = new long[localSize];
        long[] copiedDurations = new long[localSize];
        int start = localCursor - localSize;
        if (start < 0) {
            start += durationsNanos.length;
        }

        for (int i = 0; i < localSize; i++) {
            int source = (start + i) % durationsNanos.length;
            copiedIds[i] = ids[source];
            copiedCorrelations[i] = correlationIds[source];
            copiedDurations[i] = durationsNanos[source];
        }
        return new DurationSeriesSnapshot(localTotalSamples, copiedIds, copiedCorrelations, copiedDurations);
    }
}
