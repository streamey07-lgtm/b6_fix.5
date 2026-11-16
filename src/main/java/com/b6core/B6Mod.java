package com.b6core;

import com.b6core.config.B6Config;
import com.b6core.network.ModNetwork;
import com.b6core.registry.ModBlocks;
import com.b6core.registry.ModEntities;
import com.b6core.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * B6: THE CORE - main mod entry point.
 *
 * Safety rule for the whole mod: this mod never deletes, edits or corrupts
 * world saves, and never blocks the real Save and Quit button. All horror
 * is simulated visually.
 */
@Mod(B6Mod.MOD_ID)
public class B6Mod {
    public static final String MOD_ID = "b6core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public B6Mod() {
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, B6Config.COMMON_SPEC, "b6core-common.toml");
        ctx.registerConfig(ModConfig.Type.CLIENT, B6Config.CLIENT_SPEC, "b6core-client.toml");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModNetwork.init();

        LOGGER.info("B6: THE CORE loaded (Phase 3)");
    }
}
