package com.b6core.client;

import com.b6core.config.B6Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/**
 * Shared state for the main-menu "Quit Game" horror prank (session only, never saved).
 *
 * Timeline after the 3rd click:
 *   corrupt (cracks + missing texture spreads) -> melt (drips) -> fall -> gone + message.
 *
 * SAFETY: this only changes how a menu button is drawn and clicked. The game can always be
 * closed with Alt+F4 or the window's close button, and the prank resets on the next launch.
 */
final class QuitPrank {
    static final int CLICKS_NEEDED = 3;
    static final long CORRUPT_MS = 3000L;
    static final long MELT_MS = 6000L;
    static final long FALL_MS = 1400L;

    static final int PHASE_IDLE = 0;
    static final int PHASE_CORRUPT = 1;
    static final int PHASE_MELT = 2;
    static final int PHASE_FALL = 3;
    static final int PHASE_GONE = 4;

    static int clicks = 0;
    static long startMs = -1L;
    static long lastClickMs = -1L;

    // where the real button was (used to place the message)
    static int boundsX, boundsY, boundsW, boundsH;
    static boolean hasBounds = false;

    private static boolean startSoundPlayed = false;
    private static boolean fallSoundPlayed = false;
    private static boolean revealSoundPlayed = false;
    private static long lastMeltSoundMs = 0L;

    static int phase(long now) {
        if (startMs < 0L) {
            return PHASE_IDLE;
        }
        long t = now - startMs;
        if (t < CORRUPT_MS) return PHASE_CORRUPT;
        if (t < CORRUPT_MS + MELT_MS) return PHASE_MELT;
        if (t < CORRUPT_MS + MELT_MS + FALL_MS) return PHASE_FALL;
        return PHASE_GONE;
    }

    static long revealStartMs() {
        return startMs + CORRUPT_MS + MELT_MS + FALL_MS;
    }

    static void onPress(net.minecraft.client.gui.components.Button button) {
        if (startMs >= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        lastClickMs = now;
        clicks++;
        if (clicks >= CLICKS_NEEDED) {
            startMs = now;
            button.active = false;
        }
    }

    static void tickSounds(long now, int phase) {
        if (phase >= PHASE_CORRUPT && !startSoundPlayed) {
            startSoundPlayed = true;
            play(net.minecraft.sounds.SoundEvents.ENDERMAN_SCREAM, 0.4F);
        }
        if (phase == PHASE_MELT && now - lastMeltSoundMs > 450L) {
            lastMeltSoundMs = now;
            play(net.minecraft.sounds.SoundEvents.SLIME_BLOCK_HIT, 0.4F + (now % 7) * 0.04F);
        }
        if (phase >= PHASE_FALL && !fallSoundPlayed) {
            fallSoundPlayed = true;
            play(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.5F);
        }
        if (phase >= PHASE_GONE && !revealSoundPlayed) {
            revealSoundPlayed = true;
            play(net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, 0.8F);
        }
    }

    /** Temporary placeholder sounds (vanilla), scaled by the soundVolume setting. */
    static void play(SoundEvent sound, float pitch) {
        float volume = B6Config.SOUND_VOLUME.get().floatValue();
        if (volume <= 0.0F) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private QuitPrank() {}
}
