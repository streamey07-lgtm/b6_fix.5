package com.b6core.event;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * B6 writes on signs. Random signs appear near players with red text.
 * The sign itself is a normal oak sign for now (see DEVLOG: a black-and-white sign texture
 * needs a custom wood type, which is a separate step).
 * Phrases are placeholders for you to edit in PHRASES.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID)
public final class B6Signs {
    private static final String LAST_KEY = "b6core_last_sign";
    private static final double CHANCE_PER_SECOND = 0.002D; // about one every 8 minutes
    private static final long COOLDOWN_TICKS = 6000L;       // at least 5 minutes apart

    /** Four lines per sign. Empty string = blank line. Keep each line short (about 15 characters). */
    private static final String[][] PHRASES = {
            {"YOU SHOULD", "NOT BE HERE", "", "LEAVE"},
            {"I WROTE THIS", "FOR YOU", "", "I AM CLOSE"},
            {"DON'T TRUST", "THE GROUND", "", "IT REMEMBERS"},
            {"I SAW YOU", "SLEEP", "", "I WILL WAIT"},
            {"THEY LEFT", "I DID NOT", "", "ONE MORE DAY"},
    };

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || level.dimension() == com.b6core.registry.ModDimensions.CORE_LEVEL) {
            return;
        }
        double frequency = B6Config.ENCOUNTER_FREQUENCY.get();
        if (frequency <= 0.0D) {
            return;
        }
        RandomSource random = player.getRandom();
        if (random.nextDouble() >= CHANCE_PER_SECOND * frequency) {
            return;
        }
        long now = level.getGameTime();
        if (now - player.getPersistentData().getLong(LAST_KEY) < COOLDOWN_TICKS) {
            return;
        }
        BlockPos spot = EncounterHandler.findSpot(level, player, 12, 24);
        if (spot == null) {
            return;
        }
        if (placeSign(level, spot, random)) {
            player.getPersistentData().putLong(LAST_KEY, now);
            if (B6Config.DEBUG_MODE.get()) {
                B6Mod.LOGGER.info("B6 sign placed at {}", spot);
            }
        }
    }

    /** Places a sign at spot with a random B6 message in red. Returns true on success. */
    public static boolean placeSign(ServerLevel level, BlockPos spot, RandomSource random) {
        BlockState state = Blocks.OAK_SIGN.defaultBlockState()
                .setValue(StandingSignBlock.ROTATION, random.nextInt(16));
        level.setBlock(spot, state, 3);
        if (!(level.getBlockEntity(spot) instanceof SignBlockEntity sign)) {
            return false;
        }
        String[] lines = PHRASES[random.nextInt(PHRASES.length)];
        // 1.20.1 SignBlockEntity uses getText(front)/setText; we use the front side accessor.
        net.minecraft.world.level.block.entity.SignText signText =
                sign.getFrontText();
        for (int i = 0; i < 4; i++) {
            signText = signText.setMessage(i,
                    Component.literal(lines[i]).withStyle(ChatFormatting.RED));
        }
        sign.setText(signText, true); // true = front side
        sign.setChanged();
        level.sendBlockUpdated(spot, state, state, 3);
        return true;
    }

    private B6Signs() {}
}
