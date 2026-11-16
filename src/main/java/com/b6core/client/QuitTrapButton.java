package com.b6core.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/**
 * Replacement for the main-menu "Quit Game" button.
 * Clicks 1 and 2 do nothing. Click 3 starts the corruption sequence (see QuitPrank).
 * It never quits the game itself and never touches any files.
 */
public class QuitTrapButton extends Button {
    private static final int CELL = 4;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    public QuitTrapButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message, QuitPrank::onPress, DEFAULT_NARRATION);
        this.active = QuitPrank.phase(System.currentTimeMillis()) == QuitPrank.PHASE_IDLE;
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (QuitPrank.startMs >= 0L) {
            return;
        }
        // click sound gets lower each time, like something is wrong
        float pitch = 0.7F - 0.15F * QuitPrank.clicks;
        handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), pitch, 1.0F));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        int phase = QuitPrank.phase(now);
        QuitPrank.tickSounds(now, phase);

        switch (phase) {
            case QuitPrank.PHASE_IDLE -> renderIdle(g, mouseX, mouseY, partialTick, now);
            case QuitPrank.PHASE_CORRUPT -> {
                float p = (now - QuitPrank.startMs) / (float) QuitPrank.CORRUPT_MS;
                renderVanilla(g, mouseX, mouseY, partialTick);
                drawCorruption(g, p, now);
            }
            case QuitPrank.PHASE_MELT -> {
                float p = (now - QuitPrank.startMs - QuitPrank.CORRUPT_MS) / (float) QuitPrank.MELT_MS;
                drawMelted(g, Math.min(1.0F, p));
            }
            case QuitPrank.PHASE_FALL -> {
                float s = (now - QuitPrank.startMs - QuitPrank.CORRUPT_MS - QuitPrank.MELT_MS) / 1000.0F;
                float fallY = 0.5F * 900.0F * s * s;
                g.pose().pushPose();
                g.pose().translate(getX() + width / 2.0F, getY() + height / 2.0F + fallY, 0.0F);
                g.pose().mulPose(Axis.ZP.rotationDegrees(40.0F * s));
                g.pose().translate(-(getX() + width / 2.0F), -(getY() + height / 2.0F), 0.0F);
                drawMelted(g, 1.0F);
                g.pose().popPose();
            }
            default -> {
                // PHASE_GONE: nothing is drawn; the message is drawn by QuitPrankHandler
            }
        }
    }

    /** Vanilla-looking button; a short shake after each of the first clicks. */
    private void renderIdle(GuiGraphics g, int mouseX, int mouseY, float partialTick, long now) {
        long dt = now - QuitPrank.lastClickMs;
        if (QuitPrank.lastClickMs >= 0L && dt < 250L) {
            float shake = (float) Math.sin(dt * 0.8D) * 4.0F * (1.0F - dt / 250.0F);
            g.pose().pushPose();
            g.pose().translate(shake, 0.0F, 0.0F);
            renderVanilla(g, mouseX, mouseY, partialTick);
            g.pose().popPose();
        } else {
            renderVanilla(g, mouseX, mouseY, partialTick);
        }
    }

    /** Draws the normal button even though it is already deactivated. */
    private void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean prev = this.active;
        this.active = true;
        super.renderWidget(g, mouseX, mouseY, partialTick);
        this.active = prev;
    }

    private static int checker(int cx, int cy) {
        return ((cx + cy) & 1) == 0 ? WHITE : BLACK;
    }

    private static float hash(int cx, int cy, int salt) {
        int h = cx * 73856093 ^ cy * 19349663 ^ salt * 83492791;
        h ^= (h >>> 13);
        h *= 0x5bd1e995;
        h ^= (h >>> 15);
        return (h & 0xFFFF) / 65535.0F;
    }

    /** Black-and-white missing texture spreads over the button; cracks grow. */
    private void drawCorruption(GuiGraphics g, float p, long now) {
        int cols = Math.max(1, width / CELL);
        int rows = Math.max(1, height / CELL);
        Random flicker = new Random(now / 70L);

        for (int cx = 0; cx < cols; cx++) {
            for (int cy = 0; cy < rows; cy++) {
                boolean stable = hash(cx, cy, 1) < p * 1.05F;
                boolean glitch = flicker.nextFloat() < p * 0.12F;
                if (stable || glitch) {
                    int x0 = getX() + cx * CELL;
                    int y0 = getY() + cy * CELL;
                    g.fill(x0, y0, x0 + CELL, y0 + CELL, checker(cx, cy));
                }
            }
        }

        // cracks: fixed random paths, revealed progressively
        Random crackRng = new Random(9L);
        int cracks = 3;
        int segments = 16;
        int shown = (int) (p * segments * 1.2F);
        for (int c = 0; c < cracks; c++) {
            int x = getX() + 10 + crackRng.nextInt(Math.max(1, width - 20));
            int y = getY();
            for (int s = 0; s < segments && s < shown; s++) {
                int nx = x + crackRng.nextInt(9) - 4;
                int ny = y + 2 + crackRng.nextInt(3);
                drawLine(g, x, y, nx, ny);
                x = nx;
                y = ny;
                if (y > getY() + height) {
                    break;
                }
            }
        }
    }

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (n == 0) {
            n = 1;
        }
        for (int i = 0; i <= n; i++) {
            int x = x0 + (x1 - x0) * i / n;
            int y = y0 + (y1 - y0) * i / n;
            if (y >= getY() && y < getY() + height) {
                g.fill(x, y, x + 1, y + 1, 0xFF000000);
                g.fill(x + 1, y, x + 2, y + 1, 0xFFFFFFFF);
            }
        }
    }

    /** Fully missing-texture button whose cells drip downward; top cells fade away as it melts. */
    private void drawMelted(GuiGraphics g, float p) {
        int cols = Math.max(1, width / CELL);
        int rows = Math.max(1, height / CELL);
        for (int cx = 0; cx < cols; cx++) {
            float drip = 15.0F + hash(cx, 0, 2) * 55.0F;
            for (int cy = 0; cy < rows; cy++) {
                // top cells vanish first, at slightly different times per column
                if (cy < rows * p * 0.7F * (0.5F + hash(cx, 0, 3))) {
                    continue;
                }
                float depth = (cy + 1) / (float) rows;
                float dy = drip * p * depth * depth;
                float stretch = drip * p * 0.12F;
                int x0 = getX() + cx * CELL;
                int y0 = Math.round(getY() + cy * CELL + dy);
                g.fill(x0, y0, x0 + CELL, Math.round(y0 + CELL + stretch), checker(cx, cy));
            }
        }
    }
}
