package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.serverhub.Msg;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class HelpCommands {
    private HelpCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // Replaces plain "/help". "/help <vanilla command>" still works as normal.
        for (String name : new String[]{"help", "hubhelp"}) {
            d.register(Commands.literal(name).executes(HelpCommands::show));
        }
    }

    private static void line(CommandContext<CommandSourceStack> c, String cmd, String desc) {
        c.getSource().sendSuccess(() -> Msg.ok(cmd).append(Msg.info(" - " + desc)), false);
    }

    private static void header(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Msg.info("--- " + text + " ---"), false);
    }

    private static int show(CommandContext<CommandSourceStack> c) {
        header(c, "Money");
        line(c, "/balance [player]", "show a balance (/bal)");
        line(c, "/pay <player> <amount>", "send money");
        line(c, "/baltop", "richest players");
        c.getSource().sendSuccess(() -> Msg.info("Kill mobs to earn money: $1 per 4 hearts of mob health."), false);

        line(c, "/sidebar", "hide or show the sidebar");

        header(c, "Auction House");
        line(c, "/ah", "open the auction house (/auction)");
        line(c, "/ah sell <price> [amount]", "sell the item in your hand");
        line(c, "/ah search <text>", "search the auction house");
        line(c, "/ah claim", "collect items from expired listings");

        header(c, "Shop");
        line(c, "/shop", "open the admin shop (/adminshop)");
        line(c, "/shop search <text>", "search the admin shop");
        line(c, "/sell", "open the sell window - drop items in and click the green button");
        line(c, "/sellprices", "see what each item sells for");

        header(c, "Teleporting");
        line(c, "/tpa <player>", "ask to teleport to a player");
        line(c, "/tpahere <player>", "ask a player to teleport to you");
        line(c, "/tpaccept [player]", "accept a request");
        line(c, "/tpdeny [player]", "deny a request");
        line(c, "/tpcancel", "cancel your request");

        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(c.getSource())) {
            header(c, "Operators only");
            line(c, "/eco give|take|set <player> <amount>", "edit balances");
            line(c, "/eco mobhearts [hearts]", "see or set how many hearts of mob health = $1");
            line(c, "/eco start [amount]", "see or set the starting balance for new players");
            line(c, "/shopadmin add <item> <price>", "add an item (can include data like enchants) or change its price");
            line(c, "/shopadmin addheld <price>", "add the item in your hand with all its data");
            line(c, "/shopadmin sell <item> <price>", "let players /sell an item for a set price");
            line(c, "/shopadmin unsell <item>", "stop an item being sellable");
            line(c, "/shopadmin priceat|removeat <number>", "edit any item by its list number");
            line(c, "/shopadmin price <item> <price>", "change a price");
            line(c, "/shopadmin remove <item>", "remove an item");
            line(c, "/shopadmin list", "list shop items");
        }
        return 1;
    }
}
