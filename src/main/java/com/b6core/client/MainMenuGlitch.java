package com.b6core.client;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Draws B6's animated main-menu title.
 *
 * The vanilla logo texture is replaced by a transparent one (see resources/assets/minecraft),
 * and the panorama is replaced by a black-and-white checker world. This class draws the
 * vibrating, glitching black-and-white "MINECRAFT" logo on top of the title screen.
 * Purely visual: it never touches saves or menu buttons.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MainMenuGlitch {
    private static final ResourceLocation LOGO = new ResourceLocation(B6Mod.MOD_ID, "textures/gui/logo.png");
    private static final int LOGO_W = 256;
    private static final int LOGO_H = 44;
    private static final int LOGO_TOP = 30; // same place as the vanilla logo

    @SubscribeEvent
    public static void onTitleRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        float intensity = B6Config.OVERLAY_INTENSITY.get().floatValue();

        int baseX = screen.width / 2 - LOGO_W / 2;
        long now = System.currentTimeMillis();

        // New random "glitch frame" about every 50 ms so the shake is jittery, not smooth.
        Random rng = new Random(now / 50L);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (intensity <= 0.0F) {
            g.blit(LOGO, baseX, LOGO_TOP, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
            RenderSystem.disableBlend();
            return;
        }

        // 1) constant vibration of the whole logo
        int shake = Math.max(1, Math.round(2 * intensity));
        int dx = rng.nextInt(shake * 2 + 1) - shake;
        int dy = rng.nextInt(shake * 2 + 1) - shake;

        // 2) faint ghost copies (double-vision)
        g.setColor(1.0F, 1.0F, 1.0F, 0.25F);
        g.blit(LOGO, baseX + dx * 3, LOGO_TOP + dy, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
        g.blit(LOGO, baseX - dx * 2, LOGO_TOP - dy * 2, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 3) main logo, drawn in horizontal slices; some slices tear sideways in glitch bursts
        boolean burst = rng.nextFloat() < 0.18F * intensity;
        int y = 0;
        while (y < LOGO_H) {
            int h = 3 + rng.nextInt(7);
            if (y + h > LOGO_H) {
                h = LOGO_H - y;
            }
            int tear = 0;
            if (burst && rng.nextFloat() < 0.45F) {
                tear = (rng.nextInt(25) - 12) * Math.max(1, Math.round(intensity));
            }
            g.blit(LOGO, baseX + dx + tear, LOGO_TOP + dy + y, 0, y, LOGO_W, h, LOGO_W, LOGO_H);
            y += h;
        }

        // 4) rare flash: a bright block flickers over part of the logo
        if (rng.nextFloat() < 0.04F * intensity) {
            int fx = baseX + rng.nextInt(LOGO_W - 40);
            int fw = 20 + rng.nextInt(60);
            g.fill(fx, LOGO_TOP + rng.nextInt(LOGO_H - 6), Math.min(fx + fw, baseX + LOGO_W),
                    LOGO_TOP + LOGO_H, 0xCCFFFFFF);
        }

        RenderSystem.disableBlend();
    }

    private MainMenuGlitch() {}
}
