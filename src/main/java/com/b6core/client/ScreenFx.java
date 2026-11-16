package com.b6core.client;

import com.b6core.config.B6Config;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Short screen effects driven by the server (see FxMessage):
 *   static  - grainy grey noise over the screen
 *   corrupt - black-and-white missing-texture cells spreading in from the fade-out side
 *   glitch  - horizontal bars shifting across the screen
 * All effects scale with overlayIntensity (0 = off). Purely visual.
 */
public final class ScreenFx {
    public static final int STATIC = 0;
    public static final int CORRUPT = 1;
    public static final int GLITCH = 2;

    private static int kind = -1;
    private static long startMs;
    private static long durationMs;

    public static void trigger(int newKind, int lengthMs) {
        kind = newKind;
        startMs = System.currentTimeMillis();
        durationMs = Math.max(50, lengthMs);
    }

    public static void render(GuiGraphics g, int width, int height) {
        if (kind < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long dt = now - startMs;
        if (dt > durationMs) {
            kind = -1;
            return;
        }
        float intensity = (float) B6Config.OVERLAY_INTENSITY.get().doubleValue();
        if (intensity <= 0.0F) {
            return;
        }
        float life = 1.0F - dt / (float) durationMs;
        Random rng = new Random(now / 40L);

        if (kind == STATIC) {
            int cell = 6;
            int a = alpha(170.0F * life * intensity);
            for (int x = 0; x < width; x += cell) {
                for (int y = 0; y < height; y += cell) {
                    if (rng.nextFloat() < 0.5F) {
                        int v = rng.nextInt(256);
                        g.fill(x, y, x + cell, y + cell, (a << 24) | (v << 16) | (v << 8) | v);
                    }
                }
            }
        } else if (kind == CORRUPT) {
            int cell = 16;
            float spread = (1.0F - life) * Math.min(1.0F, intensity);
            int a = alpha(200.0F * Math.min(1.0F, intensity));
            for (int x = 0; x < width; x += cell) {
                for (int y = 0; y < height; y += cell) {
                    if (rng.nextFloat() < spread * 0.9F) {
                        int c = ((x / cell + y / cell) & 1) == 0 ? 0xFFFFFF : 0x000000;
                        g.fill(x, y, x + cell, y + cell, (a << 24) | c);
                    }
                }
            }
        } else if (kind == GLITCH) {
            int a = alpha(220.0F * life * intensity);
            for (int i = 0; i < 6; i++) {
                int y = rng.nextInt(Math.max(1, height));
                int barH = 2 + rng.nextInt(10);
                int shift = rng.nextInt(61) - 30;
                int c = rng.nextBoolean() ? 0xFFFFFF : 0x000000;
                g.fill(Math.max(0, shift), y, Math.min(width, width + shift), y + barH, (a << 24) | c);
            }
        }
    }

    private static int alpha(float value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    private ScreenFx() {}
}
