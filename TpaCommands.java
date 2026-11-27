package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.serverhub.TpaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public final class TpaCommands {
    private TpaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // /tpa <player>      -> ask to teleport TO them
        d.register(Commands.literal("tpa").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
            TpaManager.send(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), false);
            return 1;
        })));

        // /tpahere <player>  -> ask them to teleport to YOU
        d.register(Commands.literal("tpahere").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
            TpaManager.send(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), true);
            return 1;
        })));

        d.register(Commands.literal("tpaccept")
                .executes(c -> {
                    TpaManager.accept(c.getSource().getPlayerOrException(), null);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    TpaManager.accept(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"));
                    return 1;
                })));

        d.register(Commands.literal("tpdeny")
                .executes(c -> {
                    TpaManager.deny(c.getSource().getPlayerOrException(), null);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    TpaManager.deny(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"));
                    return 1;
                })));

        d.register(Commands.literal("tpcancel").executes(c -> {
            TpaManager.cancel(c.getSource().getPlayerOrException());
            return 1;
        }));
    }
}
