package dev.reny.optimization.profiler;

import java.util.Arrays;

/** Immutable aggregate statistics for a duration series. */
public final class DurationStatistics {

    private static final DurationStatistics EMPTY = new DurationStatistics(0L, 0L, 0L, 0.0D, 0L, 0L, 0L, 0L);

    private final long sampleCount;
    private final long minNanos;
    private final long maxNanos;
    private final double meanNanos;
    private final long p50Nanos;
    private final long p95Nanos;
    private final long p99Nanos;
    private final long p999Nanos;

    private DurationStatistics(long sampleCount, long minNanos, long maxNanos, double meanNanos, long p50Nanos,
        long p95Nanos, long p99Nanos, long p999Nanos) {
        this.sampleCount = sampleCount;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
        this.meanNanos = meanNanos;
        this.p50Nanos = p50Nanos;
        this.p95Nanos = p95Nanos;
        this.p99Nanos = p99Nanos;
        this.p999Nanos = p999Nanos;
    }

    public static DurationStatistics from(long[] samples) {
        if (samples.length == 0) {
            return EMPTY;
        }

        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        double sum = 0.0D;
        for (long sample : sorted) {
            sum += sample;
        }

        return new DurationStatistics(
            sorted.length,
            sorted[0],
            sorted[sorted.length - 1],
            sum / sorted.length,
            percentile(sorted, 0.50D),
            percentile(sorted, 0.95D),
            percentile(sorted, 0.99D),
            percentile(sorted, 0.999D));
    }

    private static long percentile(long[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        if (index < 0) {
            index = 0;
        } else if (index >= sorted.length) {
            index = sorted.length - 1;
        }
        return sorted[index];
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public long getMinNanos() {
        return minNanos;
    }

    public long getMaxNanos() {
        return maxNanos;
    }

    public double getMeanNanos() {
        return meanNanos;
    }

    public long getP50Nanos() {
        return p50Nanos;
    }

    public long getP95Nanos() {
        return p95Nanos;
    }

    public long getP99Nanos() {
        return p99Nanos;
    }

    public long getP999Nanos() {
        return p999Nanos;
    }
}
