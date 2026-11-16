package com.b6core.block;

import com.b6core.network.ModNetwork;
import com.b6core.registry.ModDimensions;
import com.b6core.world.CoreTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Black-and-white Missing Texture block. It has no collision, so walking into it
 * touches it, and touching it sends the player to the Core with a scare.
 * A short cooldown stops it from firing again straight away when the player comes back.
 */
public class MissingBlock extends Block {
    public static final String COOLDOWN_KEY = "b6core_portal_cd";
    private static final long COOLDOWN_TICKS = 200L; // 10 seconds

    public MissingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (player.level().dimension() == ModDimensions.CORE_LEVEL) {
            return;
        }
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLong(COOLDOWN_KEY)) {
            return;
        }
        player.getPersistentData().putLong(COOLDOWN_KEY, now + COOLDOWN_TICKS);
        ModNetwork.sendScare(player, 1);
        CoreTravel.enterCore(player);
    }
}
