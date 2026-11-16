package com.b6core.client;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Title-screen horror. If the mouse does not move for 1 minute:
 *  1. B6 appears behind the buttons.
 *  2. He grabs Options, splits it in half, and lets the pieces fall.
 *  3. He corrupts and drops the other buttons one by one (bottom to top).
 *  4. He grabs Singleplayer, carries it to the corner, then hands it back.
 *     It shakes and its edges are corroded, but it stays fully clickable.
 * Only the look changes. Every button that is left still works.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MenuHorror {
    private static final long IDLE_MS = 60_000L;
    private static final long APPEAR_MS = 2500L;
    private static final int U = 3;                     // pixels per B6 body unit
    private static final int HAND_X = 13, HAND_Y = 14;  // B6's right hand, in units from his top-left

    private enum Phase { GRAB, SPLIT, CORRUPT, FALL, CARRY, HAND }

    private static final class Target {
        final Button button;
        final int x, y, w, h;
        final boolean split, single;
        Phase phase = Phase.GRAB;
        long phaseStart;

        Target(Button button, boolean split, boolean single) {
            this.button = button;
            this.x = button.getX();
            this.y = button.getY();
            this.w = button.getWidth();
            this.h = button.getHeight();
            this.split = split;
            this.single = single;
        }
    }

    private static final List<Button> initButtons = new ArrayList<>();
    private static final List<Target> targets = new ArrayList<>();
    private static Target single;
    private static int idx;
    private static int lastMx = -1, lastMy = -1;
    private static long idleSince;
    private static long appearStart;
    private static int screenW, screenH;
    private static boolean started, stepsBegun, singleDone;

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !B6Config.MAIN_MENU_HORROR.get()) {
            return;
        }
        initButtons.clear();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button b && !(listener instanceof QuitTrapButton)) {
                initButtons.add(b);
            }
        }
        targets.clear();
        single = null;
        idx = 0;
        started = false;
        stepsBegun = false;
        singleDone = false;
        lastMx = -1;
        lastMy = -1;
        idleSince = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof TitleScreen ts) || !B6Config.MAIN_MENU_HORROR.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        screenW = ts.width;
        screenH = ts.height;
        int mx = event.getMouseX();
        int my = event.getMouseY();
        if (!started && (mx != lastMx || my != lastMy)) {
            idleSince = now;
        }
        lastMx = mx;
        lastMy = my;
        if (!started && now - idleSince >= IDLE_MS) {
            start(now);
        }
        advance(now);
        if (singleDone && single != null) {
            int shake = (int) Math.round(Math.sin(now / 45.0D) * 2.0D);
            single.button.setX(single.x + shake);
        }
    }

    /** B6 behind the buttons while he is appearing (this runs before any button is drawn). */
    @SubscribeEvent
    public static void onBackground(ScreenEvent.BackgroundRendered event) {
        if (!started || stepsBegun || !(event.getScreen() instanceof TitleScreen ts)) {
            return;
        }
        float a = Math.min(1.0F, (System.currentTimeMillis() - appearStart) / (float) APPEAR_MS);
        drawB6(event.getGuiGraphics(), ts.width / 2 + 40, ts.height / 2 - 20, a);
    }

    @SubscribeEvent
    public static void onPost(ScreenEvent.Render.Post event) {
        if (!started || !stepsBegun || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        long now = System.currentTimeMillis();
        if (idx < targets.size()) {
            drawStep(g, targets.get(idx), now);
        }
        if (singleDone && single != null) {
            drawCorrosion(g, single);
        }
    }

    // ---------- sequence control ----------

    private static void start(long now) {
        started = true;
        appearStart = now;
        targets.clear();
        idx = 0;

        Button options = null;
        Button play = null;
        List<Button> others = new ArrayList<>();
        for (Button b : initButtons) {
            if (!b.visible) {
                continue;
            }
            String key = keyOf(b);
            if ("menu.singleplayer".equals(key)) {
                play = b;
            } else if ("menu.options".equals(key)) {
                options = b;
            } else {
                others.add(b);
            }
        }
        others.sort((a, c) -> Integer.compare(c.getY(), a.getY())); // bottom to top

        if (options != null) {
            targets.add(new Target(options, true, false));
        }
        for (Button b : others) {
            targets.add(new Target(b, false, false));
        }
        if (play != null) {
            single = new Target(play, false, true);
            targets.add(single);
        }
    }

    private static void advance(long now) {
        if (!started || idx >= targets.size()) {
            return;
        }
        if (!stepsBegun) {
            if (now - appearStart < APPEAR_MS) {
                return;
            }
            stepsBegun = true;
            enter(targets.get(0), Phase.GRAB, now);
            return;
        }
        Target t = targets.get(idx);
        if (now - t.phaseStart < duration(t)) {
            return;
        }
        switch (t.phase) {
            case GRAB -> enter(t, t.single ? Phase.CARRY : t.split ? Phase.SPLIT : Phase.CORRUPT, now);
            case SPLIT, CORRUPT -> enter(t, Phase.FALL, now);
            case FALL -> {
                t.button.visible = false;
                t.button.active = false;
                idx++;
                if (idx < targets.size()) {
                    enter(targets.get(idx), Phase.GRAB, now);
                }
            }
            case CARRY -> enter(t, Phase.HAND, now);
            case HAND -> {
                restore(t);
                singleDone = true;
                idx++;
            }
        }
    }

    private static void enter(Target t, Phase phase, long now) {
        t.phase = phase;
        t.phaseStart = now;
        boolean drawnByUs = phase != Phase.GRAB;
        t.button.visible = !drawnByUs;
        t.button.active = phase == Phase.GRAB;
    }

    private static void restore(Target t) {
        t.button.setX(t.x);
        t.button.setY(t.y);
        t.button.visible = true;
        t.button.active = true;
    }

    private static long duration(Target t) {
        return switch (t.phase) {
            case GRAB -> (t.split || t.single) ? 900L : 700L;
            case SPLIT, CORRUPT -> 700L;
            case FALL -> 1000L;
            case CARRY -> 1200L;
            case HAND -> 700L;
        };
    }

    // ---------- drawing ----------

    private static void drawStep(GuiGraphics g, Target t, long now) {
        long e = now - t.phaseStart;
        float p = Math.min(1.0F, e / (float) duration(t));
        float fall = 450.0F * (e / 1000.0F) * (e / 1000.0F);
        int carryX = screenW - 130;
        int carryY = screenH - 60;

        switch (t.phase) {
            case GRAB -> drawB6(g, t.x + t.w - 10, t.y + t.h / 2, 1.0F);
            case SPLIT -> {
                int half = t.w / 2;
                int rdx = Math.round(14.0F * p);
                int rdy = Math.round(-10.0F * p);
                drawPiece(g, t, 0, 0, t.x, t.x + half);
                drawPiece(g, t, rdx, rdy, t.x + half + rdx, t.x + t.w + rdx);
                drawB6(g, t.x + t.w - 10 + rdx, t.y + t.h / 2 + rdy, 1.0F);
            }
            case CORRUPT -> {
                drawPiece(g, t, 0, 0, 0, 0);
                drawCorruption(g, t.x, t.y, t.w, t.h, 0, p);
                drawB6(g, t.x + t.w - 10, t.y + t.h / 2, 1.0F);
            }
            case FALL -> {
                int dy = Math.round(fall);
                if (t.split) {
                    int half = t.w / 2;
                    drawPiece(g, t, 0, dy, t.x, t.x + half);
                    drawPiece(g, t, 14, -10 + dy, t.x + half + 14, t.x + t.w + 14);
                } else {
                    drawPiece(g, t, 0, dy, 0, 0);
                    drawCorruption(g, t.x, t.y, t.w, t.h, dy, 1.0F);
                }
                drawB6(g, t.x + t.w - 10, t.y + t.h / 2 - 20, Math.max(0.0F, 1.0F - p));
            }
            case CARRY -> {
                int dx = Math.round((carryX - t.x) * p);
                int dy = Math.round((carryY - t.y) * p);
                drawPiece(g, t, dx, dy, 0, 0);
                drawB6(g, t.x + t.w - 10 + dx, t.y + t.h / 2 + dy, 1.0F);
            }
            case HAND -> {
                int dx = Math.round((carryX - t.x) * (1.0F - p));
                int dy = Math.round((carryY - t.y) * (1.0F - p));
                drawPiece(g, t, dx, dy, 0, 0);
                drawB6(g, t.x + t.w - 10 + dx, t.y + t.h / 2 + dy, 1.0F);
            }
        }
    }

    /**
     * Draws the vanilla button at an offset. If clipR > clipL, only the part inside
     * [clipL, clipR] (in screen coordinates) is drawn, which is how the split halves work.
     */
    private static void drawPiece(GuiGraphics g, Target t, int dx, int dy, int clipL, int clipR) {
        Button b = t.button;
        boolean previous = b.visible;
        b.visible = true;
        g.pose().pushPose();
        g.pose().translate(dx, dy, 0.0F);
        boolean clip = clipR > clipL;
        if (clip) {
            g.enableScissor(clipL, t.y + dy, clipR, t.y + t.h + dy);
        }
        b.render(g, -1, -1, 0.0F);
        if (clip) {
            g.disableScissor();
        }
        g.pose().popPose();
        b.visible = previous;
    }

    /** Black-and-white missing texture spreading across a button area, shifted by dy. */
    private static void drawCorruption(GuiGraphics g, int x, int y, int w, int h, int dy, float p) {
        int cell = 4;
        for (int cx = 0; cx < Math.max(1, w / cell); cx++) {
            for (int cy = 0; cy < Math.max(1, h / cell); cy++) {
                float roll = new Random(cx * 73L + cy * 191L).nextFloat();
                if (roll < p) {
                    int color = ((cx + cy) & 1) == 0 ? 0xFFFFFFFF : 0xFF000000;
                    int x0 = x + cx * cell;
                    int y0 = y + cy * cell + dy;
                    g.fill(x0, y0, x0 + cell, y0 + cell, color);
                }
            }
        }
    }

    /** Corroded edges on the handed-back Singleplayer button. It stays clickable. */
    private static void drawCorrosion(GuiGraphics g, Target t) {
        Random rng = new Random(77L);
        int cell = 3;
        for (int x = t.x; x < t.x + t.w; x += cell) {
            corrodeCell(g, rng, x, t.y);
            corrodeCell(g, rng, x, t.y + t.h - cell);
        }
        for (int y = t.y; y < t.y + t.h; y += cell) {
            corrodeCell(g, rng, t.x, y);
            corrodeCell(g, rng, t.x + t.w - cell, y);
        }
    }

    private static void corrodeCell(GuiGraphics g, Random rng, int x, int y) {
        if (rng.nextFloat() < 0.45F) {
            int color = rng.nextBoolean() ? 0xFFFFFFFF : 0xFF000000;
            g.fill(x, y, x + 3, y + 3, color);
        }
    }

    /**
     * Draws B6 from his main-form skin colours (shadow on the left, white on the right),
     * with his right hand placed at (hx, hy) when he is holding something.
     */
    private static void drawB6(GuiGraphics g, int hx, int hy, float alpha) {
        int ox = hx - HAND_X * U;
        int oy = hy - HAND_Y * U;
        int a = Math.max(0, Math.min(255, (int) (255 * alpha)));
        if (a <= 0) {
            return;
        }
        // {unitX, unitY, widthUnits, heightUnits}: head, body, left arm, right arm
        int[][] parts = {{4, 0, 8, 8}, {4, 8, 8, 12}, {0, 8, 4, 12}, {12, 8, 4, 12}};
        for (int[] part : parts) {
            for (int i = 0; i < part[2]; i++) {
                for (int j = 0; j < part[3]; j++) {
                    int ux = part[0] + i;
                    int uy = part[1] + j;
                    boolean shadow = ux < 8;
                    boolean check = ((i / 2 + j / 2) & 1) == 0;
                    int rgb = shadow ? (check ? 0x1C1C1C : 0x080808) : (check ? 0xFFFFFF : 0xD4D4D4);
                    int px = ox + ux * U;
                    int py = oy + uy * U;
                    g.fill(px, py, px + U, py + U, (a << 24) | rgb);
                }
            }
        }
        // glowing eyes on the head
        int eye = (a << 24) | 0xFFFFFF;
        g.fill(ox + 6 * U, oy + 3 * U, ox + 8 * U, oy + 4 * U, eye);
        g.fill(ox + 9 * U, oy + 3 * U, ox + 11 * U, oy + 4 * U, eye);
    }

    private static String keyOf(Button b) {
        return b.getMessage().getContents() instanceof TranslatableContents tc ? tc.getKey() : "";
    }

    private MenuHorror() {}
}
