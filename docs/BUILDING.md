# Building Reny Optimization

## Requirements

Reny targets Java 8 bytecode, but the current Gradle 9.x wrapper must run on a modern JDK. Use JDK 21+ to launch Gradle; the GTNH/RFG toolchain compiles the mod for Java 8.

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
- Java 8 source and bytecode target
- UniMixins enabled through `usesMixins=true`
- no coremod
- no direct ASM

## Mixins

`dev.reny.optimization.mixin` is reserved for future runtime patches. It is deliberately empty during the bootstrap milestone. Mixins must later be associated with stable Patch Registry IDs and must not bypass the registry/compatibility policy.
