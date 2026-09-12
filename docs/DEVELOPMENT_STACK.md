# Development Stack

## Baseline

Reny targets Minecraft 1.7.10 while using a modern-enough development toolchain to keep the project maintainable.

### Game/runtime target

- Minecraft: `1.7.10`
- Forge: `10.13.4.1614`
- MCP mappings: `stable_12`
- Runtime baseline: Java 8
- Bytecode target: Java 8

Java 8 remains the compatibility floor even when Gradle or developer tooling runs on a newer JVM.

## Build system

### RetroFuturaGradle ecosystem

The project should use the maintained GTNH/RetroFuturaGradle toolchain rather than historical ForgeGradle 1.2.

Reasons:

- maintained support for Minecraft 1.7.10;
- modern Gradle compatibility;
- Forge/FML run configurations;
- MCP/deobfuscation support;
- reobfuscation and packaging;
- built-in support for Mixins/coremods;
- easier CI on current systems.

The initial codebase should avoid language-level modernization such as Jabel/JVM Downgrader. Java 8 source syntax reduces variables during early bootstrap/debugging. This can be revisited after the Instrumented Core is stable.

## Runtime transformation stack

### 1. Forge/FML hooks

Use public Forge/FML extension points where they are sufficient.

### 2. AccessTransformers

Use AccessTransformers when the problem is only visibility/finality and no behavioral bytecode rewrite is needed.

### 3. UniMixins + Mixin

Mixin is the preferred behavioral patch mechanism.

Planned usage:

- early mixins for Minecraft/Forge/FML classes;
- late/conditional mixins for mod-specific compatibility;
- MixinExtras where it makes an injection substantially safer or clearer.

Mixin configuration must remain modular by subsystem so incompatible groups can be disabled independently.

### 4. ASM

ASM is reserved for transformations that cannot reasonably be represented with Mixin/AccessTransformers.

Rules:

- direct ASM code belongs under `bytecode/`;
- every transformer must have a stable patch ID;
- every transformer must fail closed when expected bytecode does not match;
- transformed-class dumps should be available in debug mode;
- bytecode verification should be possible in development builds;
- do not expose a newer `org.objectweb.asm` globally if it can conflict with the 1.7.10 classpath.

If a newer ASM is ever required, it should be isolated/relocated.

## Coremod policy

Reny may use an FML core plugin, but it must remain intentionally small.

Core plugin responsibilities:

- initialize bootstrap state;
- establish transformation infrastructure;
- register required transformers/configuration;
- collect early environment information;
- expose diagnostics for bootstrap failures.

Actual performance algorithms do not belong in the core plugin class.

## Profiling toolchain

### async-profiler

Primary CPU/allocation profiler when supported by the selected JVM/platform.

Use for:

- CPU flamegraphs;
- allocation hot spots;
- lock/contention investigation;
- wall-clock profiling;
- native/JVM frames where available.

### Java Flight Recorder / JDK Mission Control

Use to investigate:

- garbage collection;
- allocation pressure;
- long JVM pauses;
- locks;
- thread scheduling;
- JIT/runtime behavior.

JFR availability/capabilities vary by Java 8 distribution, so benchmark records must include the exact JVM distribution/version.

### VisualVM

Useful for:

- heap inspection;
- thread dumps;
- leak investigations;
- class-loading observations.

Not used as the canonical microbenchmark source.

### Linux `perf`

Use for low-level CPU investigations:

- cycles;
- instructions;
- IPC;
- cache misses;
- branch misses;
- context switches;
- page faults.

Hardware-counter results are machine-specific and must include hardware metadata.

### apitrace

Preferred initial OpenGL tracing tool for the legacy renderer path.

Use to inspect:

- draw calls;
- state churn;
- texture/buffer binds;
- frame traces;
- OpenGL call distribution.

### RenderDoc

Reserved primarily for a modern/core-profile renderer path where capture support is appropriate. Do not make it a required tool for the vanilla 1.7.10 renderer.

## Internal instrumentation

External profilers are not enough. Reny will also expose low-overhead internal timers/counters for game-specific subsystems.

Target categories:

- frame;
- tick;
- entities;
- TileEntities;
- chunks;
- lighting;
- meshing;
- GPU upload queue;
- I/O;
- Forge events;
- allocations/GC observations where safely available.

Internal metrics must be switchable so profiling overhead can be measured and minimized.

## CI expectations

Early CI should validate at minimum:

- project configuration;
- Java compilation;
- formatting/static checks;
- unit tests for pure Java components;
- successful packaging/reobfuscation.

Later CI stages may add deterministic benchmark-result comparison, but performance gates should not rely on noisy shared cloud runners for absolute timing.
