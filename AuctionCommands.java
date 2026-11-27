package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.serverhub.Auction;
import dev.serverhub.Msg;
import dev.serverhub.gui.AuctionGui;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class AuctionCommands {
    private static final long MAX_PRICE = 1_000_000_000_000L;

    private AuctionCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        for (String name : new String[]{"ah", "auction"}) {
            d.register(Commands.literal(name)
                    .executes(c -> {
                        AuctionGui.open(c.getSource().getPlayerOrException());
                        return 1;
                    })
                    .then(Commands.literal("sell")
                            .then(Commands.argument("price", LongArgumentType.longArg(1, MAX_PRICE))
                                    .executes(c -> sell(c.getSource().getPlayerOrException(),
                                            LongArgumentType.getLong(c, "price"), -1))
                                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(c -> sell(c.getSource().getPlayerOrException(),
                                                    LongArgumentType.getLong(c, "price"),
                                                    IntegerArgumentType.getInteger(c, "amount"))))))
                    .then(Commands.literal("search").then(Commands.argument("text", StringArgumentType.greedyString()).executes(c -> {
                        AuctionGui.open(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "text"));
                        return 1;
                    })))
                    .then(Commands.literal("claim").executes(c -> {
                        ServerPlayer p = c.getSource().getPlayerOrException();
                        int n = Auction.claim(p);
                        p.sendSystemMessage(n > 0 ? Msg.ok("Claimed " + n + " item(s).") : Msg.err("Nothing to claim."));
                        return n > 0 ? 1 : 0;
                    })));
        }
    }

    private static int sell(ServerPlayer p, long price, int amount) {
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty()) {
            p.sendSystemMessage(Msg.err("Hold the item you want to sell in your main hand."));
            return 0;
        }
        int count = amount < 0 ? held.getCount() : Math.min(amount, held.getCount());
        ItemStack toSell = held.copy();
        toSell.setCount(count);
        String err = Auction.create(p, toSell, price);
        if (err != null) {
            p.sendSystemMessage(Msg.err(err));
            return 0;
        }
        if (count >= held.getCount()) p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        else held.shrink(count);
        return 1;
    }
}
