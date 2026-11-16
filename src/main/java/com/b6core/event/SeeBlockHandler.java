package com.b6core.event;

import com.b6core.B6Mod;
import com.b6core.config.B6Config;
import com.b6core.network.ModNetwork;
import com.b6core.registry.ModBlocks;
import com.b6core.registry.ModDimensions;
import com.b6core.world.CoreTravel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Looking at a Missing Texture block for about 2 seconds sends you to the Core dimension.
 * Turn off with lookTeleport=false. /b6core leave brings you back.
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID)
public final class SeeBlockHandler {
    private static final String KEY = "b6core_look";
    private static final double RANGE = 24.0D;
    private static final int TRIGGER_TICKS = 40;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 5 != 0 || !B6Config.LOOK_TELEPORT.get()) {
            return;
        }
        if (player.isSpectator() || player.level().dimension() == ModDimensions.CORE_LEVEL) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int count = data.getInt(KEY);
        if (isLookingAtMissingBlock(player)) {
            count += 5;
            if (count == 5) {
                // first moment of eye contact with the block
                player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.HOSTILE, 1.0F, 0.8F);
            }
        } else {
            count = Math.max(0, count - 10);
        }
        data.putInt(KEY, count);

        if (count >= TRIGGER_TICKS) {
            data.putInt(KEY, 0);
            ModNetwork.sendScare(player, 1);
            CoreTravel.enterCore(player);
        }
    }

    private static boolean isLookingAtMissingBlock(ServerPlayer player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = player.level().clip(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK
                && player.level().getBlockState(hit.getBlockPos()).is(ModBlocks.MISSING_BLOCK.get());
    }

    private SeeBlockHandler() {}
}
