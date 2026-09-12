package dev.reny.optimization.profiler;

/** Simple developer microbenchmark for profiler call-site overhead. Not a CI pass/fail gate. */
public final class ProfilerOverheadBenchmark {

    private static final int WARMUP_ITERATIONS = 1_000_000;
    private static final int MEASURE_ITERATIONS = 5_000_000;

    public static void main(String[] args) {
        InternalProfiler profiler = new InternalProfiler(1024, 1024);
        runDisabled(profiler, WARMUP_ITERATIONS);
        runEnabled(profiler, WARMUP_ITERATIONS);

        long disabled = runDisabled(profiler, MEASURE_ITERATIONS);
        long enabled = runEnabled(profiler, MEASURE_ITERATIONS);

        System.out.println("Profiler overhead benchmark");
        System.out.println("iterations=" + MEASURE_ITERATIONS);
        System.out.println("disabled_ns_per_op=" + perOperation(disabled));
        System.out.println("enabled_section_ns_per_op=" + perOperation(enabled));
        System.out.println("Note: compare on the same JVM, machine, and power profile; this task is informational.");
    }

    private static long runDisabled(InternalProfiler profiler, int iterations) {
        profiler.getConfig()
            .setEnabled(false);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long token = profiler.startSection(ProfilerSection.CHUNK);
            profiler.endSection(ProfilerSection.CHUNK, token);
        }
        return System.nanoTime() - start;
    }

    private static long runEnabled(InternalProfiler profiler, int iterations) {
        profiler.getConfig()
            .setEnabled(true);
        profiler.getConfig()
            .setGroupEnabled(ProfilerGroup.CHUNK, true);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long token = profiler.startSection(ProfilerSection.CHUNK);
            profiler.endSection(ProfilerSection.CHUNK, token);
        }
        return System.nanoTime() - start;
    }

    private static double perOperation(long totalNanos) {
        return (double) totalNanos / MEASURE_ITERATIONS;
    }
}
