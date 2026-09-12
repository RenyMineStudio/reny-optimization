package dev.reny.optimization.benchmark;

import java.util.Arrays;

import dev.reny.optimization.profiler.DurationSeriesSnapshot;

/** Export-time statistics for one measured benchmark window. */
public final class BenchmarkStatistics {

    private static final long[] FRAME_THRESHOLDS_NANOS = { 16_670_000L, 33_330_000L, 50_000_000L, 100_000_000L,
        250_000_000L };

    private final long sampleCount;
    private final long minNanos;
    private final long maxNanos;
    private final double meanNanos;
    private final double standardDeviationNanos;
    private final long p50Nanos;
    private final long p90Nanos;
    private final long p95Nanos;
    private final long p99Nanos;
    private final long p999Nanos;
    private final long[] frameThresholdCounts;

    private BenchmarkStatistics(long sampleCount, long minNanos, long maxNanos, double meanNanos,
        double standardDeviationNanos, long p50Nanos, long p90Nanos, long p95Nanos, long p99Nanos, long p999Nanos,
        long[] frameThresholdCounts) {
        this.sampleCount = sampleCount;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
        this.meanNanos = meanNanos;
        this.standardDeviationNanos = standardDeviationNanos;
        this.p50Nanos = p50Nanos;
        this.p90Nanos = p90Nanos;
        this.p95Nanos = p95Nanos;
        this.p99Nanos = p99Nanos;
        this.p999Nanos = p999Nanos;
        this.frameThresholdCounts = frameThresholdCounts;
    }

    public static BenchmarkStatistics from(DurationSeriesSnapshot series, boolean frameSeries) {
        int count = series.size();
        if (count == 0) {
            return new BenchmarkStatistics(0L, 0L, 0L, 0.0D, 0.0D, 0L, 0L, 0L, 0L, 0L,
                new long[FRAME_THRESHOLDS_NANOS.length]);
        }

        long[] sorted = new long[count];
        double sum = 0.0D;
        long[] thresholds = new long[FRAME_THRESHOLDS_NANOS.length];
        for (int i = 0; i < count; i++) {
            long value = series.getDurationNanos(i);
            sorted[i] = value;
            sum += value;
            if (frameSeries) {
                for (int threshold = 0; threshold < FRAME_THRESHOLDS_NANOS.length; threshold++) {
                    if (value > FRAME_THRESHOLDS_NANOS[threshold]) {
                        thresholds[threshold]++;
                    }
                }
            }
        }
        Arrays.sort(sorted);
        double mean = sum / count;
        double squaredDifferenceSum = 0.0D;
        for (long value : sorted) {
            double difference = value - mean;
            squaredDifferenceSum += difference * difference;
        }
        double standardDeviation = Math.sqrt(squaredDifferenceSum / count);

        return new BenchmarkStatistics(
            count,
            sorted[0],
            sorted[count - 1],
            mean,
            standardDeviation,
            percentile(sorted, 0.50D),
            percentile(sorted, 0.90D),
            percentile(sorted, 0.95D),
            percentile(sorted, 0.99D),
            percentile(sorted, 0.999D),
            thresholds);
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

    public double getStandardDeviationNanos() {
        return standardDeviationNanos;
    }

    public long getP50Nanos() {
        return p50Nanos;
    }

    public long getP90Nanos() {
        return p90Nanos;
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

    public long getFrameThresholdCount(int index) {
        return frameThresholdCounts[index];
    }

    public double getFrameThresholdRate(int index) {
        return sampleCount == 0L ? 0.0D : (double) frameThresholdCounts[index] / sampleCount;
    }

    public double getAverageFps() {
        return fpsEquivalent(meanNanos);
    }

    public double getOnePercentLowFps() {
        return fpsEquivalent(p99Nanos);
    }

    public double getPointOnePercentLowFps() {
        return fpsEquivalent(p999Nanos);
    }

    public double getEffectiveTps() {
        if (meanNanos <= 0.0D) {
            return 0.0D;
        }
        return Math.min(20.0D, 1_000_000_000.0D / meanNanos);
    }

    private static double fpsEquivalent(double nanos) {
        return nanos <= 0.0D ? 0.0D : 1_000_000_000.0D / nanos;
    }
}
