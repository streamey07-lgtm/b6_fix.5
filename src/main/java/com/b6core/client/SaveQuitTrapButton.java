package com.b6core.client;

import com.b6core.config.B6Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/**
 * Replaces "Save and Quit to Title" on day 6 and later.
 * Click -> cracks appear -> black-and-white missing texture spreads -> the button melts.
 * When it finishes, the button is dead: clicks do nothing and it shows "Use Alt + F4".
 * The mod never saves or quits for the player. Alt+F4 and the window close button still work.
 */
public class SaveQuitTrapButton extends Button {
    private static final int CELL = 4;
    private static final long CRACK_MS = 2500L;
    private static final long CORRUPT_MS = 2500L;
    private static final long MELT_MS = 3000L;
    private static final long TOTAL_MS = CRACK_MS + CORRUPT_MS + MELT_MS;

    private final Button original;
    private long startMs = -1L;
    private boolean finished = false;

    public SaveQuitTrapButton(int x, int y, int width, int height, Component message, Button original) {
        super(x, y, width, height, message, b -> { }, DEFAULT_NARRATION);
        this.original = original;
    }

    @Override
    public void onPress() {
        if (startMs < 0L && !finished) {
            startMs = System.currentTimeMillis();
            playSound(SoundEvents.WARDEN_HEARTBEAT, 0.7F);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (startMs < 0L) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            return;
        }
        long e = System.currentTimeMillis() - startMs;
        if (e >= TOTAL_MS) {
            // Dead button: it never saves or quits. Clicks do nothing, and the player is told to use Alt + F4.
            finished = true;
            g.drawCenteredString(Minecraft.getInstance().font,
                    "You thought you could save and quit? Use Alt + F4.",
                    getX() + width / 2, getY() + height + 4, 0xFFFFFFFF);
            return;
        }

        if (e < CRACK_MS) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            drawCracks(g, e / (float) CRACK_MS);
        } else if (e < CRACK_MS + CORRUPT_MS) {
            float p = (e - CRACK_MS) / (float) CORRUPT_MS;
            drawCorruption(g, p);
            drawCracks(g, 1.0F);
        } else {
            float p = (e - CRACK_MS - CORRUPT_MS) / (float) MELT_MS;
            if (p < 0.02F) {
                playSound(SoundEvents.ENDERMAN_SCREAM, 0.5F);
            }
            drawMelt(g, p);
        }
    }

    /** Cracks grow from the top edge; each crack is a fixed random path. */
    private void drawCracks(GuiGraphics g, float p) {
        Random rng = new Random(9L);
        int cracks = 3;
        int segments = 14;
        int shown = (int) (p * segments);
        for (int c = 0; c < cracks; c++) {
            int x = getX() + 10 + rng.nextInt(Math.max(1, width - 20));
            int y = getY();
            for (int s = 0; s < segments && s < shown; s++) {
                int nx = x + rng.nextInt(9) - 4;
                int ny = y + 1 + rng.nextInt(3);
                for (int i = 0; i <= 3; i++) {
                    int px = x + (nx - x) * i / 3;
                    int py = y + (ny - y) * i / 3;
                    if (py >= getY() && py < getY() + height) {
                        g.fill(px, py, px + 1, py + 1, 0xFF000000);
                        g.fill(px + 1, py, px + 2, py + 1, 0xFFFFFFFF);
                    }
                }
                x = nx;
                y = ny;
            }
        }
    }

    /** Black-and-white missing texture spreads over the button cell by cell. */
    private void drawCorruption(GuiGraphics g, float p) {
        int cols = Math.max(1, width / CELL);
        int rows = Math.max(1, height / CELL);
        for (int cx = 0; cx < cols; cx++) {
            for (int cy = 0; cy < rows; cy++) {
                float roll = new Random(cx * 73L + cy * 191L).nextFloat();
                if (roll < p) {
                    int color = ((cx + cy) & 1) == 0 ? 0xFFFFFFFF : 0xFF000000;
                    int x0 = getX() + cx * CELL;
                    int y0 = getY() + cy * CELL;
                    g.fill(x0, y0, x0 + CELL, y0 + CELL, color);
                }
            }
        }
    }

    /** The corrupted button drips downward, top cells fading first. */
    private void drawMelt(GuiGraphics g, float p) {
        int cols = Math.max(1, width / CELL);
        int rows = Math.max(1, height / CELL);
        for (int cx = 0; cx < cols; cx++) {
            float drip = 10.0F + (cx * 37 % 40);
            for (int cy = 0; cy < rows; cy++) {
                if (cy < rows * p * 0.8F) {
                    continue;
                }
                float depth = (cy + 1) / (float) rows;
                int x0 = getX() + cx * CELL;
                int y0 = Math.round(getY() + cy * CELL + drip * p * depth * depth);
                int color = ((cx + cy) & 1) == 0 ? 0xFFFFFFFF : 0xFF000000;
                g.fill(x0, y0, x0 + CELL, y0 + CELL, color);
            }
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        float volume = B6Config.SOUND_VOLUME.get().floatValue();
        if (volume > 0.0F) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }
}
