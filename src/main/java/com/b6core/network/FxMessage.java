package com.b6core.network;

import com.b6core.client.ScreenFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: play a screen effect (static, corruption or glitch bars) for a set time. */
public class FxMessage {
    private final int kind;
    private final int durationMs;

    public FxMessage(int kind, int durationMs) {
        this.kind = kind;
        this.durationMs = durationMs;
    }

    public static void encode(FxMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.kind);
        buf.writeVarInt(msg.durationMs);
    }

    public static FxMessage decode(FriendlyByteBuf buf) {
        return new FxMessage(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FxMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ScreenFx.trigger(msg.kind, msg.durationMs);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
