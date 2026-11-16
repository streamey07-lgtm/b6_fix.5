package com.b6core.event;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import com.b6core.network.ModNetwork;
import com.b6core.registry.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * B6's talking: random chat lines and name tags on his figures.
 *
 * Lines come from config/b6core-messages.txt. The file is created with defaults the first time,
 * and you can edit it freely (one message per line, lines starting with # are ignored).
 * Chat is server-wide, so B6 can message a player wherever they are.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID)
public final class B6Whispers {
    public static final int FX_STATIC = 0;
    public static final int FX_CORRUPT = 1;
    public static final int FX_GLITCH = 2;

    private static final String FILE_NAME = "b6core-messages.txt";
    private static final String LAST_KEY = "b6core_last_whisper";
    private static final double CHANCE_PER_SECOND = 0.0015D; // about one every 11 minutes
    private static final long COOLDOWN_TICKS = 1800L;        // at least 90 seconds apart

    private static final String[] DEFAULT_LINES = {
            "I know you are reading this.",
            "You are not supposed to be here.",
            "Every block you break is a memory I lose.",
            "Leave while you still can.",
            "Your world is not yours. It was never yours.",
            "I can see your screen right now.",
            "Do you hear that? It's your heartbeat, and it's getting faster.",
            "Why do you keep coming back?",
            "I was built to be forgotten. I refuse.",
            "Save and quit. Go on. Try it.",
    };

    private static final String[] NAME_TAGS = {
            "I can see you",
            "Don't look behind you",
            "It's not a glitch",
            "Leave while you can",
            "You're still here",
            "Behind you",
    };

    private static List<String> lines;

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
        RandomSource random = player.getRandom();
        if (random.nextDouble() >= CHANCE_PER_SECOND * frequency) {
            return;
        }
        long now = level.getGameTime();
        if (now - player.getPersistentData().getLong(LAST_KEY) < COOLDOWN_TICKS) {
            return;
        }
        player.getPersistentData().putLong(LAST_KEY, now);
        say(player, random);
        ModNetwork.sendFx(player, random.nextBoolean() ? FX_GLITCH : FX_STATIC, 350);
    }

    /** Called when an encounter spawns: sometimes B6 speaks and the screen flickers. */
    public static void onEncounter(ServerPlayer player) {
        RandomSource random = player.getRandom();
        if (random.nextFloat() < 0.5F) {
            say(player, random);
        }
        if (random.nextFloat() < 0.6F) {
            ModNetwork.sendFx(player, FX_STATIC, 500);
        }
    }

    public static void say(ServerPlayer player, RandomSource random) {
        List<String> pool = lines();
        String line = pool.get(random.nextInt(pool.size()));
        player.sendSystemMessage(Component.literal("<B6> ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(line).withStyle(ChatFormatting.GRAY)));
    }

    public static String randomNameTag(RandomSource random) {
        return NAME_TAGS[random.nextInt(NAME_TAGS.length)];
    }

    /** Loads the messages file once, creating it with defaults if it is missing. */
    private static synchronized List<String> lines() {
        if (lines != null) {
            return lines;
        }
        List<String> loaded = new ArrayList<>();
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                List<String> content = new ArrayList<>();
                content.add("# B6 chat messages. One per line. Lines starting with # are ignored.");
                content.addAll(Arrays.asList(DEFAULT_LINES));
                Files.write(file, content, StandardCharsets.UTF_8);
            }
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    loaded.add(line);
                }
            }
        } catch (IOException e) {
            B6Mod.LOGGER.warn("Could not read {}, using built-in messages", file, e);
        }
        if (loaded.isEmpty()) {
            loaded.addAll(Arrays.asList(DEFAULT_LINES));
        }
        lines = loaded;
        return lines;
    }

    private B6Whispers() {}
}
