package com.b6core.command;

import com.b6core.B6Mod;
import com.b6core.entity.B6Entity;
import com.b6core.event.EncounterHandler;
import com.b6core.network.ModNetwork;
import com.b6core.world.CoreTravel;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /b6core spawn <glitch|stone|main>   (op)  spawn B6 in front of you
 * /b6core core                        (op)  go to the Core dimension
 * /b6core scare                       (op)  play the jumpscare
 * /b6core leave                       (anyone) return from the Core dimension
 */
@Mod.EventBusSubscriber(modid = B6Mod.MOD_ID)
public final class B6Commands {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("b6core")
                .then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("glitch").executes(c -> spawn(c.getSource(), B6Entity.FORM_GLITCH)))
                        .then(Commands.literal("stone").executes(c -> spawn(c.getSource(), B6Entity.FORM_STONE)))
                        .then(Commands.literal("main").executes(c -> spawn(c.getSource(), B6Entity.FORM_MAIN))))
                .then(Commands.literal("core").requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return CoreTravel.enterCore(p) ? 1 : fail(c.getSource(), "Could not enter the Core.");
                        }))
                .then(Commands.literal("sign").requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            BlockPos spot = EncounterHandler.findSpot(p.serverLevel(), p, 3, 6);
                            if (spot == null) {
                                return fail(c.getSource(), "No free spot for a sign nearby.");
                            }
                            return com.b6core.event.B6Signs.placeSign(p.serverLevel(), spot, p.getRandom()) ? 1
                                    : fail(c.getSource(), "Could not place the sign.");
                        }))
                .then(Commands.literal("scare").requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            ModNetwork.sendScare(c.getSource().getPlayerOrException(), 1);
                            return 1;
                        }))
                .then(Commands.literal("leave")
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return CoreTravel.leaveCore(p) ? 1 : fail(c.getSource(), "You are not in the Core.");
                        })));
    }

    private static int spawn(CommandSourceStack source, int form) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 look = player.getLookAngle();
        BlockPos spot = BlockPos.containing(player.getX() + look.x * 6.0D, player.getY(), player.getZ() + look.z * 6.0D);
        EncounterHandler.spawn(player.serverLevel(), player, spot, form);
        source.sendSuccess(() -> Component.literal("Spawned B6."), true);
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private B6Commands() {}
}
