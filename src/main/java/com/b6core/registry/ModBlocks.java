package com.b6core.registry;

import com.b6core.B6Mod;
import com.b6core.block.MissingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, B6Mod.MOD_ID);

    /** B6 stage 1: the Missing Texture block. Walking into it sends you to the Core. */
    public static final RegistryObject<Block> MISSING_BLOCK = BLOCKS.register("missing_block",
            () -> new MissingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.STONE)
                    .lightLevel(state -> 6)
                    .noCollission()));

    /** Core dimension ground (dark checker). */
    public static final RegistryObject<Block> CORE_STONE = BLOCKS.register("core_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(3.0F, 8.0F).sound(SoundType.STONE)));

    /** Core dimension surface (bright checker). */
    public static final RegistryObject<Block> CORE_SURFACE = BLOCKS.register("core_surface",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW).strength(3.0F, 8.0F).sound(SoundType.STONE)));

    private ModBlocks() {}
}
