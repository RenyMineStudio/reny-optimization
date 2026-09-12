package dev.reny.optimization.profiler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Initial Forge/FML hooks for client frame timing and server simulation MSPT. */
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tickStart = profiler.beginTick();
        } else {
            profiler.endTick(tickStart);
            tickStart = InternalProfiler.DISABLED_TOKEN;
        }
    }
}
