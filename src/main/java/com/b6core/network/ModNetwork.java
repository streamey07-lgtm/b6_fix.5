package com.b6core.network;

import com.b6core.B6Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(B6Mod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        CHANNEL.registerMessage(0, ScareMessage.class, ScareMessage::encode, ScareMessage::decode, ScareMessage::handle);
        CHANNEL.registerMessage(1, FxMessage.class, FxMessage::encode, FxMessage::decode, FxMessage::handle);
    }

    /** Tells one player's client to play the jumpscare flash. */
    public static void sendScare(ServerPlayer player, int strength) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScareMessage(strength));
    }

    /** Tells one player's client to play a screen effect (see B6Whispers.FX_*). */
    public static void sendFx(ServerPlayer player, int kind, int durationMs) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FxMessage(kind, durationMs));
    }

    private ModNetwork() {}
}
