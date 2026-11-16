package com.b6core.client;

import com.b6core.B6Mod;
import com.b6core.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only mod-bus events. */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.B6.get(), B6Renderer::new);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("b6_scare", (gui, graphics, partialTick, width, height) ->
                ScareOverlay.render(graphics, width, height));
        event.registerAboveAll("b6_fx", (gui, graphics, partialTick, width, height) ->
                ScreenFx.render(graphics, width, height));
    }

    private ClientEvents() {}
}
