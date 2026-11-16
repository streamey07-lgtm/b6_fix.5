package com.b6core.event;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import com.b6core.entity.B6Entity;
import com.b6core.registry.ModBlocks;
import com.b6core.registry.ModDimensions;
import com.b6core.registry.ModEntities;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Random B6 encounters. Checked once per second per player.
 * Frequency comes from encounterFrequency (x3 in the Nether); forms unlock with storyStage.
 * Never runs on Peaceful or inside the Core dimension.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID)
public final class EncounterHandler {
    private static final String LAST_KEY = "b6core_last_encounter";
    private static final double BASE_CHANCE_PER_SECOND = 0.004D; // roughly one every ~4 minutes
    private static final long COOLDOWN_TICKS = 2400L;           // at least 2 minutes apart

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() == ModDimensions.CORE_LEVEL || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        double frequency = B6Config.ENCOUNTER_FREQUENCY.get();
        if (frequency <= 0.0D) {
            return;
        }
        double chance = BASE_CHANCE_PER_SECOND * frequency * (level.dimension() == Level.NETHER ? 3.0D : 1.0D);
        RandomSource random = player.getRandom();
        if (random.nextDouble() >= chance) {
            return;
        }
        long now = level.getGameTime();
        if (now - player.getPersistentData().getLong(LAST_KEY) < COOLDOWN_TICKS) {
            return;
        }

        BlockPos spot = findSpot(level, player, 18, 28);
        if (spot == null) {
            return;
        }
        player.getPersistentData().putLong(LAST_KEY, now);
        int story = B6Config.STORY_STAGE.get();

        // sometimes a missing-texture block appears nearby instead of (or as well as) a figure
        if (random.nextFloat() < 0.15F) {
            BlockPos blockSpot = findSpot(level, player, 10, 18);
            if (blockSpot != null) {
                level.setBlock(blockSpot, ModBlocks.MISSING_BLOCK.get().defaultBlockState(), 3);
            }
        }

        int form = B6Entity.FORM_GLITCH;
        float roll = random.nextFloat();
        if (story >= 6 && roll < 0.10F) {
            form = B6Entity.FORM_MAIN;
        } else if (story >= 3 && roll < 0.35F) {
            form = B6Entity.FORM_STONE;
        }
        spawn(level, player, spot, form);
        if (B6Config.DEBUG_MODE.get()) {
            B6Mod.LOGGER.info("B6 encounter: form {} at {}", form, spot);
        }
    }

    public static void spawn(ServerLevel level, ServerPlayer player, BlockPos spot, int form) {
        B6Entity b6 = ModEntities.B6.get().create(level);
        if (b6 == null) {
            return;
        }
        b6.moveTo(spot.getX() + 0.5D, spot.getY() + (form == B6Entity.FORM_MAIN ? 1.0D : 0.0D), spot.getZ() + 0.5D, 0.0F, 0.0F);
        b6.setForm(form);
        b6.lookAt(EntityAnchorArgument.Anchor.EYES, player.position());
        if (level.getRandom().nextFloat() < 0.5F) {
            b6.setCustomName(Component.literal(B6Whispers.randomNameTag(level.getRandom())));
            b6.setCustomNameVisible(true);
        }
        level.addFreshEntity(b6);
        B6Whispers.onEncounter(player);
    }

    /** Finds an air spot with solid ground below, at a random angle and distance from the player. */
    public static BlockPos findSpot(ServerLevel level, ServerPlayer player, int minDist, int maxDist) {
        RandomSource random = player.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int dist = minDist + random.nextInt(maxDist - minDist + 1);
            int x = player.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = player.getBlockZ() + (int) (Math.sin(angle) * dist);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, player.getBlockY() + 8, z);
            for (int dy = 0; dy < 20; dy++) {
                if (level.isLoaded(pos) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                    BlockPos below = pos.below();
                    if (level.isLoaded(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                        return pos.immutable();
                    }
                }
                pos.move(0, -1, 0);
            }
        }
        return null;
    }

    private EncounterHandler() {}
}
