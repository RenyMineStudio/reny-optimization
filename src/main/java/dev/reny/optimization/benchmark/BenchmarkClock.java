package dev.reny.optimization.benchmark;

/** Injectable clock used to keep benchmark phase transitions deterministic in tests. */
public interface BenchmarkClock {

    long nanoTime();

    long currentTimeMillis();
}
