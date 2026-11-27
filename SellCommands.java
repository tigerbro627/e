package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.serverhub.gui.SellMenu;
import dev.serverhub.gui.SellPricesGui;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class SellCommands {
    private SellCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sell").executes(c -> {
            SellMenu.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        d.register(Commands.literal("sellprices").executes(c -> {
            SellPricesGui.open(c.getSource().getPlayerOrException());
            return 1;
        }));
    }
}
