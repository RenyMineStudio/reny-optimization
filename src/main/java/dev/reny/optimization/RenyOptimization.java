package dev.reny.optimization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

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
        LOG.info("Reny Optimization {} initialized with no optimization patches active", Tags.VERSION);
    }
}
