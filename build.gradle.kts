plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The Patch Registry deliberately uses a dependency-free executable self-test instead
// of a JUnit engine. Gradle 9 otherwise fails when it sees test sources but discovers
// no framework-managed tests.
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

tasks.named("check") {
    dependsOn("patchRegistrySelfTest")
}
