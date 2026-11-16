package com.b6core.registry;

import com.b6core.B6Mod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, B6Mod.MOD_ID);

    public static final RegistryObject<Item> B6_SPAWN_EGG = ITEMS.register("b6_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.B6, 0x000000, 0xFFFFFF, new Item.Properties()));

    public static final RegistryObject<Item> MISSING_BLOCK = ITEMS.register("missing_block",
            () -> new BlockItem(ModBlocks.MISSING_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CORE_STONE = ITEMS.register("core_stone",
            () -> new BlockItem(ModBlocks.CORE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CORE_SURFACE = ITEMS.register("core_surface",
            () -> new BlockItem(ModBlocks.CORE_SURFACE.get(), new Item.Properties()));

    private ModItems() {}
}
