package dev.reny.optimization.profiler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Initial Forge/FML hooks for frame and physical-side tick timing. */
public final class ForgeProfilerHooks {

    public static final ForgeProfilerHooks INSTANCE = new ForgeProfilerHooks();

    private final InternalProfiler profiler = InternalProfiler.get();
    private long frameStart = InternalProfiler.DISABLED_TOKEN;
    private long tickStart = InternalProfiler.DISABLED_TOKEN;

    private ForgeProfilerHooks() {}

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            frameStart = profiler.beginFrame();
        } else {
            profiler.endFrame(frameStart);
            frameStart = InternalProfiler.DISABLED_TOKEN;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            return;
        }
        handleTickPhase(event.phase);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!FMLCommonHandler.instance()
            .getSide()
            .isServer()) {
            return;
        }
        handleTickPhase(event.phase);
    }

    private void handleTickPhase(TickEvent.Phase phase) {
        if (phase == TickEvent.Phase.START) {
            tickStart = profiler.beginTick();
        } else {
            profiler.endTick(tickStart);
            tickStart = InternalProfiler.DISABLED_TOKEN;
        }
    }
}
