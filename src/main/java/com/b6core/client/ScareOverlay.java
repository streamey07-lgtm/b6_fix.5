package com.b6core.client;

import com.b6core.config.B6Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Jumpscare: a fast black-and-white checker flash with a huge "B6" that fades out.
 * Strength follows the overlayIntensity setting (0 = off). Purely visual.
 */
public final class ScareOverlay {
    private static final long DURATION_MS = 900L;
    private static long startMs = -1L;

    /** Called when the server sends a scare. */
    public static void trigger(int strength) {
        startMs = System.currentTimeMillis();
    }

    public static void render(GuiGraphics g, int width, int height) {
        if (startMs < 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long dt = now - startMs;
        float intensity = B6Config.OVERLAY_INTENSITY.get().floatValue();
        if (dt > DURATION_MS || intensity <= 0.0F) {
            return;
        }
        float life = 1.0F - dt / (float) DURATION_MS;
        int alpha = (int) (255 * Math.min(1.0F, life * 1.4F) * Math.min(1.0F, intensity));
        if (alpha < 4) {
            return;
        }

        Random rng = new Random(now / 45L);
        int cell = 48;
        for (int x = 0; x < width; x += cell) {
            for (int y = 0; y < height; y += cell) {
                int rgb = rng.nextBoolean() ? 0xFFFFFF : 0x000000;
                g.fill(x, y, x + cell, y + cell, (alpha << 24) | rgb);
            }
        }

        if (rng.nextFloat() < 0.75F) {
            var font = Minecraft.getInstance().font;
            g.pose().pushPose();
            g.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
            g.pose().scale(12.0F, 12.0F, 1.0F);
            g.drawCenteredString(font, "B6", 1, -3, (alpha << 24) | 0x000000);
            g.drawCenteredString(font, "B6", 0, -4, (alpha << 24) | 0xFFFFFF);
            g.pose().popPose();
        }
    }

    private ScareOverlay() {}
}
