package dev.reny.optimization;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dev.reny.optimization.profiler.ForgeProfilerHooks;
import dev.reny.optimization.profiler.InternalProfiler;

@Mod(
    modid = RenyOptimization.MOD_ID,
    name = RenyOptimization.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]")
public final class RenyOptimization {

    public static final String MOD_ID = "renyoptimization";
    public static final String MOD_NAME = "Reny Optimization";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        InternalProfiler profiler = InternalProfiler.get();
        profiler.initialize(new File(event.getModConfigurationDirectory(), "reny-profiler"));
        FMLCommonHandler.instance().bus().register(ForgeProfilerHooks.INSTANCE);
        LOG.info(
            "Reny Optimization {} initialized; internal profiler enabled={}, no optimization patches active",
            Tags.VERSION,
            profiler.getConfig().isEnabled());
    }
}
