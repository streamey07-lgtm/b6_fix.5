package com.b6core.world;

import com.b6core.B6Mod;
import com.b6core.block.MissingBlock;
import com.b6core.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Moves players into and out of the Core dimension.
 * The return point is remembered on the player, so /b6core leave always brings them back.
 * Only the player's position is changed; no world files are touched.
 */
public final class CoreTravel {
    private static final String RET_DIM = "b6core_ret_dim";
    private static final String RET_X = "b6core_ret_x";
    private static final String RET_Y = "b6core_ret_y";
    private static final String RET_Z = "b6core_ret_z";

    /** Standing height on the flat Core ground (layers fill y=0..59). */
    private static final double CORE_SPAWN_Y = 60.0D;

    public static boolean enterCore(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.level().dimension() == ModDimensions.CORE_LEVEL) {
            return false;
        }
        ServerLevel core = server.getLevel(ModDimensions.CORE_LEVEL);
        if (core == null) {
            B6Mod.LOGGER.warn("Core dimension 'b6core:k9' was not found. Is the data folder inside the jar?");
            return false;
        }
        CompoundTag data = player.getPersistentData();
        data.putString(RET_DIM, player.level().dimension().location().toString());
        data.putDouble(RET_X, player.getX());
        data.putDouble(RET_Y, player.getY());
        data.putDouble(RET_Z, player.getZ());

        player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 0.5F);
        player.teleportTo(core, 0.5D, CORE_SPAWN_Y, 0.5D, player.getYRot(), 0.0F);
        return true;
    }

    public static boolean leaveCore(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.level().dimension() != ModDimensions.CORE_LEVEL) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        ServerLevel dest = null;
        double x = 0, y = 0, z = 0;
        if (data.contains(RET_DIM)) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.getString(RET_DIM)));
            dest = server.getLevel(key);
            x = data.getDouble(RET_X);
            y = data.getDouble(RET_Y);
            z = data.getDouble(RET_Z);
        }
        if (dest == null) {
            dest = server.getLevel(player.getRespawnDimension());
            if (dest == null) {
                dest = server.overworld();
            }
            BlockPos pos = player.getRespawnPosition();
            if (pos == null) {
                pos = dest.getSharedSpawnPos();
            }
            x = pos.getX() + 0.5D;
            y = pos.getY();
            z = pos.getZ() + 0.5D;
        }
        // so the Missing Texture block does not send the player straight back in
        data.putLong(MissingBlock.COOLDOWN_KEY, player.level().getGameTime() + 200L);
        player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());
        return true;
    }

    private CoreTravel() {}
}
