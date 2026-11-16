package com.b6core.client;

import com.b6core.B6Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * From day 2 to day 6 the sun shows a word from B6 instead of being plain.
 * Drawn right after the sky, on the same plane and at the same spot as the vanilla sun
 * (the rotation copies vanilla's sun rotation). Overworld only.
 * Words are placeholders: edit WORDS. Day 1 is the first day; day 2 uses WORDS[0].
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SunWords {
    private static final String[] WORDS = {
            "LEAVE",
            "I SEE YOU",
            "STILL HERE?",
            "ONE MORE DAY",
            "SAVE AND QUIT",
    };
    private static final int COLOR_RED = 0xFFFF2020;
    private static final float TEXT_SCALE = 0.6F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.font == null || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        long day = level.getDayTime() / 24000L; // 0 = day 1, 1 = day 2, ...
        if (day < 1 || day > WORDS.length) {
            return;
        }
        String word = WORDS[(int) day - 1];

        float timeOfDay = level.getTimeOfDay(event.getPartialTick());
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        stack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0F));
        stack.translate(0.0F, 100.0F, 0.0F);
        stack.mulPose(Axis.XP.rotationDegrees(90.0F));
        stack.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);

        Font font = mc.font;
        float x = -font.width(word) / 2.0F;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        font.drawInBatch(word, x, 0.0F, COLOR_RED, false, stack.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
        stack.popPose();
    }

    private SunWords() {}
}
