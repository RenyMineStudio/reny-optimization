plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The project deliberately uses dependency-free executable self-tests instead of a JUnit engine.
// Gradle 9 otherwise fails when it sees test sources but discovers no framework-managed tests.
tasks.withType<org.gradle.api.tasks.testing.AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.register<JavaExec>("patchRegistrySelfTest") {
    group = "verification"
    description = "Runs the dependency-free Patch Registry self-test suite."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.reny.optimization.patch.PatchRegistrySelfTest")
}

tasks.register<JavaExec>("compatibilitySelfTest") {
    group = "verification"
    description = "Runs the dependency-free environment/compatibility self-test suite."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.reny.optimization.compat.CompatibilityManagerSelfTest")
}

tasks.register<JavaExec>("profilerSelfTest") {
    group = "verification"
    description = "Runs the dependency-free internal profiler self-test suite."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.reny.optimization.profiler.InternalProfilerSelfTest")
}

tasks.register<JavaExec>("benchmarkHarnessSelfTest") {
    group = "verification"
    description = "Runs the dependency-free benchmark harness self-test suite."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.reny.optimization.profiler.BenchmarkHarnessSelfTest")
}

tasks.register<JavaExec>("profilerOverheadBenchmark") {
    group = "verification"
    description = "Runs the informational internal-profiler overhead microbenchmark."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.reny.optimization.profiler.ProfilerOverheadBenchmark")
}

tasks.named("check") {
    dependsOn("patchRegistrySelfTest", "compatibilitySelfTest", "profilerSelfTest", "benchmarkHarnessSelfTest")
}
