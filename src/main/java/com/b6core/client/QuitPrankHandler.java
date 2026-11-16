package com.b6core.client;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Hooks the title screen: swaps the Quit Game button for QuitTrapButton and,
 * once the button has fallen, shows B6's line where it used to be.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuitPrankHandler {
    private static final Component MESSAGE =
            Component.literal("You thought you could leave that easily? Just use your Alt + F4.");

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !B6Config.MENU_QUIT_PRANK.get()) {
            return;
        }
        Button quit = null;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button b && !(listener instanceof QuitTrapButton) && isQuitButton(b)) {
                quit = b;
                break;
            }
        }
        if (quit == null) {
            B6Mod.LOGGER.warn("Quit Game button not found on title screen; quit prank skipped");
            return;
        }

        QuitPrank.boundsX = quit.getX();
        QuitPrank.boundsY = quit.getY();
        QuitPrank.boundsW = quit.getWidth();
        QuitPrank.boundsH = quit.getHeight();
        QuitPrank.hasBounds = true;

        // hide the real button; our button takes its place
        quit.visible = false;
        quit.active = false;
        event.addListener(new QuitTrapButton(quit.getX(), quit.getY(), quit.getWidth(), quit.getHeight(),
                quit.getMessage()));
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen) || !B6Config.MENU_QUIT_PRANK.get()
                || !QuitPrank.hasBounds) {
            return;
        }
        long now = System.currentTimeMillis();
        if (QuitPrank.phase(now) != QuitPrank.PHASE_GONE) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        float fade = Math.min(1.0F, (now - QuitPrank.revealStartMs()) / 1200.0F);
        int alpha = Math.max(8, (int) (255 * fade));
        int color = (alpha << 24) | 0xFFFFFF;

        Random rng = new Random(now / 60L);
        int jx = rng.nextFloat() < 0.15F ? rng.nextInt(5) - 2 : 0;
        int centerX = screen.width / 2;
        int y = QuitPrank.boundsY + QuitPrank.boundsH + 8;
        var font = Minecraft.getInstance().font;

        if (rng.nextFloat() < 0.08F) {
            g.drawCenteredString(font, MESSAGE, centerX + 3, y - 1, (alpha / 2 << 24) | 0x888888);
        }
        g.drawCenteredString(font, MESSAGE, centerX + jx, y, color);
    }

    private static boolean isQuitButton(Button b) {
        return b.getMessage().getContents() instanceof TranslatableContents tc
                && "menu.quit".equals(tc.getKey());
    }

    private QuitPrankHandler() {}
}
