package com.b6core.client;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * On day 6 and later, swaps the pause menu's "Save and Quit to Title" button for SaveQuitTrapButton.
 * Only the appearance and the delay change. The real button is pressed at the end.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SaveQuitHandler {
    private static final long DAY_LENGTH_TICKS = 24000L;
    private static final long HORROR_FROM_DAY = 6L; // day 6 counted from day 1

    @SubscribeEvent
    public static void onPauseInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen) || !B6Config.SAVE_QUIT_HORROR.get()) {
            return;
        }
        if (!isHorrorDayOrLater()) {
            return;
        }
        Button target = null;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button b && isReturnToTitleButton(b)) {
                target = b;
                break;
            }
        }
        if (target == null) {
            B6Mod.LOGGER.warn("Save and Quit to Title button not found; day-6 horror skipped");
            return;
        }
        target.visible = false;
        target.active = false;
        event.addListener(new SaveQuitTrapButton(target.getX(), target.getY(), target.getWidth(),
                target.getHeight(), target.getMessage(), target));
    }

    private static boolean isHorrorDayOrLater() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        long day = level.getDayTime() / DAY_LENGTH_TICKS + 1L; // day 1 = first 24000 ticks
        return day >= HORROR_FROM_DAY;
    }

    private static boolean isReturnToTitleButton(Button b) {
        return b.getMessage().getContents() instanceof TranslatableContents tc
                && "menu.returnToMenu".equals(tc.getKey());
    }

    private SaveQuitHandler() {}
}
