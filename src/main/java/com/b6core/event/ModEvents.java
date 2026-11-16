package com.b6core.event;

import com.b6core.B6Mod;
import com.b6core.entity.B6Entity;
import com.b6core.registry.ModEntities;
import com.b6core.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus events that run on both client and server. */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.B6.get(), B6Entity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.B6_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.MISSING_BLOCK.get());
            event.accept(ModItems.CORE_STONE.get());
            event.accept(ModItems.CORE_SURFACE.get());
        }
    }

    private ModEvents() {}
}
