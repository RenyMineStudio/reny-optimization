package dev.reny.optimization.compat;

import java.util.SortedMap;
import java.util.TreeMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;

/** Best-effort, fail-soft Forge 1.7.10 environment detector. */
public final class EnvironmentDetector {

    private EnvironmentDetector() {}

    public static EnvironmentSnapshot capture() {
        EnvironmentSnapshot.Builder builder = EnvironmentSnapshot.builder()
            .javaInfo(property("java.vendor"), property("java.version"))
            .osInfo(property("os.name"), property("os.version"), property("os.arch"));

        detectSide(builder);
        SortedMap<String, DetectedMod> mods = detectMods(builder);
        detectSyntheticMods(mods);
        for (DetectedMod mod : mods.values()) {
            builder.addMod(mod);
        }
        detectCapabilities(builder, mods);
        detectRenderPaths(builder, mods);
        return builder.build();
    }

    private static void detectSide(EnvironmentSnapshot.Builder builder) {
        try {
            Side side = FMLCommonHandler.instance()
                .getSide();
            if (side != null && side.isClient()) {
                builder.side(EnvironmentSide.CLIENT);
            } else if (side != null && side.isServer()) {
                builder.side(EnvironmentSide.DEDICATED_SERVER);
            }
        } catch (Throwable ignored) {
            // Bootstrap/tooling processes may not have an initialized FML side yet.
        }
    }

    private static SortedMap<String, DetectedMod> detectMods(EnvironmentSnapshot.Builder builder) {
        TreeMap<String, DetectedMod> mods = new TreeMap<String, DetectedMod>();
        try {
            for (ModContainer container : Loader.instance()
                .getActiveModList()) {
                DetectedMod mod = new DetectedMod(
                    container.getModId(),
                    container.getName(),
                    container.getVersion(),
                    false);
                mods.put(mod.getNormalizedId(), mod);
            }
            builder.capability(EnvironmentCapability.FORGE_FML);
        } catch (Throwable ignored) {
            // Missing/uninitialized Loader is represented by the absent FORGE_FML capability.
        }
        return mods;
    }

    private static void detectSyntheticMods(SortedMap<String, DetectedMod> mods) {
        if (!contains(mods, KnownMod.OPTIFINE)
            && (classPresent("Config") || classPresent("optifine.OptiFineClassTransformer"))) {
            putSynthetic(mods, "optifine", "OptiFine");
        }
        if (!contains(mods, KnownMod.LWJGL3IFY) && classPresent("org.lwjgl.system.Platform")) {
            putSynthetic(mods, "lwjgl3ify", "LWJGL3ify");
        }
    }

    private static void detectCapabilities(EnvironmentSnapshot.Builder builder, SortedMap<String, DetectedMod> mods) {
        if (classPresent("org.spongepowered.asm.mixin.Mixin")) {
            builder.capability(EnvironmentCapability.MIXIN);
        }
        if (contains(mods, KnownMod.UNIMIXINS)) {
            builder.capability(EnvironmentCapability.UNIMIXINS)
                .capability(EnvironmentCapability.MIXIN);
        }
        boolean lwjgl3 = contains(mods, KnownMod.LWJGL3IFY) || classPresent("org.lwjgl.system.Platform");
        if (lwjgl3) {
            builder.capability(EnvironmentCapability.LWJGL3);
        }
        boolean angelica = contains(mods, KnownMod.ANGELICA);
        if (angelica || lwjgl3) {
            builder.capability(EnvironmentCapability.MODERN_OPENGL);
        }
        if (angelica || contains(mods, KnownMod.OPTIFINE)) {
            builder.capability(EnvironmentCapability.SHADERS);
        }
    }

    private static void detectRenderPaths(EnvironmentSnapshot.Builder builder, SortedMap<String, DetectedMod> mods) {
        boolean angelica = contains(mods, KnownMod.ANGELICA);
        boolean optifine = contains(mods, KnownMod.OPTIFINE);
        if (angelica && optifine) {
            builder.renderer(RendererPath.UNKNOWN, ShaderPath.UNKNOWN);
        } else if (angelica) {
            builder.renderer(RendererPath.ANGELICA, ShaderPath.ANGELICA);
        } else if (optifine) {
            builder.renderer(RendererPath.OPTIFINE, ShaderPath.OPTIFINE);
        } else {
            builder.renderer(RendererPath.VANILLA, ShaderPath.NONE);
        }
    }

    private static boolean contains(SortedMap<String, DetectedMod> mods, KnownMod knownMod) {
        for (String alias : knownMod.getAliases()) {
            if (mods.containsKey(DetectedMod.normalizeId(alias))) {
                return true;
            }
        }
        return false;
    }

    private static void putSynthetic(SortedMap<String, DetectedMod> mods, String id, String name) {
        DetectedMod mod = new DetectedMod(id, name, "unknown", true);
        mods.put(mod.getNormalizedId(), mod);
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, EnvironmentDetector.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String property(String name) {
        try {
            String value = System.getProperty(name);
            return value == null || value.trim().isEmpty() ? "unknown" : value;
        } catch (SecurityException ignored) {
            return "unknown";
        }
    }
}
