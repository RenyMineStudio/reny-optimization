# Building Reny Optimization

## Requirements

Reny targets **Java 8 source semantics and Java 8 bytecode**, but the current GTNH Gradle tooling itself requires **JDK 25** to run. This is only the build JVM; it does not raise the runtime requirement of the produced Minecraft mod.

Use JDK 25 to launch Gradle. The RFG/GTNH toolchain then compiles Reny for Java 8 (`classfile major version 52`).

The project intentionally uses real Java 8 source semantics (`enableModernJavaSyntax=false`) rather than Jabel or JVM Downgrader.

## First setup

```bash
./gradlew setupDecompWorkspace
```

## Build

```bash
./gradlew build
```

The production/reobfuscated artifact is produced under `build/libs/`.

## Development client

```bash
./gradlew runClient
```

The Forge development client should list **Reny Optimization** as an installed mod. The bootstrap contains no active optimization patches yet.

## Development server

```bash
./gradlew runServer
```

## Stack

- Minecraft `1.7.10`
- Forge `10.13.4.1614`
- MCP `stable_12`
- GTNH convention / RetroFuturaGradle toolchain
- Gradle wrapper `9.3.1`
- Gradle build JVM: JDK `25`
- mod source semantics: Java `8`
- mod bytecode target: Java `8`
- UniMixins enabled through `usesMixins=true`
- no coremod
- no direct ASM

## Mixins

`dev.reny.optimization.mixin` is reserved for future runtime patches. It is deliberately empty during the bootstrap milestone. Mixins must later be associated with stable Patch Registry IDs and must not bypass the registry/compatibility policy.
