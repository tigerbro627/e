package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.serverhub.Msg;
import dev.serverhub.SidebarManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class SidebarCommands {
    private SidebarCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sidebar").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean on = SidebarManager.toggle(p);
            p.sendSystemMessage(Msg.ok("Sidebar " + (on ? "shown." : "hidden.") + " (It shows again next time you join.)"));
            return 1;
        }));
    }
}
