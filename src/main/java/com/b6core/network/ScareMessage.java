package com.b6core.network;

import com.b6core.client.ScareOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: play the jumpscare overlay. */
public class ScareMessage {
    private final int strength;

    public ScareMessage(int strength) {
        this.strength = strength;
    }

    public static void encode(ScareMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.strength);
    }

    public static ScareMessage decode(FriendlyByteBuf buf) {
        return new ScareMessage(buf.readVarInt());
    }

    public static void handle(ScareMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ScareOverlay.trigger(msg.strength);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
