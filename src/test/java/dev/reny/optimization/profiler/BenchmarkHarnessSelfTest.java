package dev.reny.optimization.profiler;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import dev.reny.optimization.benchmark.BenchmarkContext;
import dev.reny.optimization.benchmark.BenchmarkScenario;
import dev.reny.optimization.benchmark.BenchmarkSession;
import dev.reny.optimization.benchmark.BenchmarkStatistics;

/** Dependency-free verification for benchmark phase isolation, schema, and local export. */
public final class BenchmarkHarnessSelfTest {

    private int passed;

    public static void main(String[] args) throws Exception {
        new BenchmarkHarnessSelfTest().run();
    }

    private void run() throws Exception {
        System.setProperty("reny.commit.sha", "test-commit");
        testScenarioCatalog();
        testMeasuredStatistics();
        testWarmupIsolationAndExport();
        testRepeatedRunsDoNotOverwrite();
        testStateValidation();
        System.out.println("BenchmarkHarnessSelfTest: " + passed + " tests passed");
    }

    private void testScenarioCatalog() {
        equal(9L, BenchmarkScenario.values().length, "scenario count");
        check(BenchmarkScenario.byId("BENCH-01") == BenchmarkScenario.STATIONARY_RENDER, "BENCH-01 lookup");
        check(BenchmarkScenario.byId("bench-99") == BenchmarkScenario.MONSTER, "case-insensitive BENCH-99 lookup");
        pass();
    }

    private void testMeasuredStatistics() {
        DurationSeries series = new DurationSeries(8);
        series.record(1L, 0L, 10_000_000L);
        series.record(2L, 0L, 20_000_000L);
        series.record(3L, 0L, 40_000_000L);
        series.record(4L, 0L, 120_000_000L);
        BenchmarkStatistics statistics = BenchmarkStatistics.from(series.snapshot(), true);
        equal(4L, statistics.getSampleCount(), "statistics sample count");
        equal(20_000_000L, statistics.getP50Nanos(), "statistics p50");
        equal(120_000_000L, statistics.getP99Nanos(), "statistics p99");
        equal(3L, statistics.getFrameThresholdCount(0), ">16.67 ms measured count");
        equal(1L, statistics.getFrameThresholdCount(3), ">100 ms measured count");
        pass();
    }

    private void testWarmupIsolationAndExport() throws Exception {
        InternalProfiler profiler = new InternalProfiler(32, 32);
        File root = Files.createTempDirectory("reny-benchmark-warmup")
            .toFile();
        BenchmarkSession session = new BenchmarkSession(
            profiler,
            BenchmarkScenario.STATIONARY_RENDER,
            context(),
            0L,
            1L,
            root);

        session.startWarmup();
        long warmupFrame = profiler.beginFrame();
        profiler.endFrame(warmupFrame);
        long warmupTick = profiler.beginTick();
        profiler.endTick(warmupTick);
        check(session.shouldBeginMeasurement(), "zero-duration warmup should be ready");
        session.beginMeasurement();

        long measuredFrameId = profiler.getCurrentFrameId() + 1L;
        long measuredTickId = profiler.getCurrentTickId() + 1L;
        profiler.recordFrameDurationNanos(measuredFrameId, measuredTickId, 20_000_000L);
        profiler.recordTickDurationNanos(measuredTickId, measuredFrameId, 30_000_000L);

        File output = session.finish();
        File environment = new File(output, "environment.json");
        File summary = new File(output, "summary.json");
        File frames = new File(output, "frames.csv");
        File ticks = new File(output, "ticks.csv");
        check(environment.isFile() && summary.isFile() && frames.isFile() && ticks.isFile(), "required export files");

        String frameText = read(frames);
        String tickText = read(ticks);
        String summaryText = read(summary);
        String environmentText = read(environment);
        check(frameText.contains(measuredFrameId + "," + measuredTickId + ",20000000"), "measured frame exported");
        check(!frameText.contains("1,1,"), "warmup frame excluded");
        check(tickText.contains(measuredTickId + "," + measuredFrameId + ",30000000"), "measured tick exported");
        check(summaryText.contains("\"sample_count\": 1"), "summary uses measured window");
        check(summaryText.contains("\"schema_version\": 1"), "summary schema version");
        check(environmentText.contains("\"commit_sha\": \"test-commit\""), "commit SHA persisted");
        check(environmentText.contains("\"vsync\": false"), "VSync persisted");
        pass();
    }

    private void testRepeatedRunsDoNotOverwrite() throws Exception {
        InternalProfiler profiler = new InternalProfiler(8, 8);
        File root = Files.createTempDirectory("reny-benchmark-unique")
            .toFile();
        BenchmarkSession first = new BenchmarkSession(
            profiler,
            BenchmarkScenario.ENTITY_STRESS,
            context(),
            0L,
            1L,
            root);
        first.startWarmup();
        first.beginMeasurement();
        File firstOutput = first.finish();

        BenchmarkSession second = new BenchmarkSession(
            profiler,
            BenchmarkScenario.ENTITY_STRESS,
            context(),
            0L,
            1L,
            root);
        second.startWarmup();
        second.beginMeasurement();
        File secondOutput = second.finish();

        check(!first.getRunId().equals(second.getRunId()), "run IDs must be unique");
        check(!firstOutput.equals(secondOutput), "repeated runs must use different directories");
        check(firstOutput.isDirectory() && secondOutput.isDirectory(), "both repeated runs preserved");
        pass();
    }

    private void testStateValidation() throws Exception {
        BenchmarkSession session = new BenchmarkSession(
            new InternalProfiler(8, 8),
            BenchmarkScenario.LIGHTING_TORTURE,
            context(),
            0L,
            1L,
            Files.createTempDirectory("reny-benchmark-state")
                .toFile());
        boolean failed = false;
        try {
            session.beginMeasurement();
        } catch (IllegalStateException expected) {
            failed = true;
        }
        check(failed, "measurement cannot begin before warmup");
        pass();
    }

    private static BenchmarkContext context() {
        return BenchmarkContext.builder()
            .display(1280, 720, 8, false, 0)
            .shader("none", "none", "none")
            .world("12345", "self-test", "stationary", "day-clear")
            .configHash("self-test-config")
            .build();
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private void pass() {
        passed++;
    }

    private static void equal(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
