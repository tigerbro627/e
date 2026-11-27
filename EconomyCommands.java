package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.serverhub.Config;
import dev.serverhub.Economy;
import dev.serverhub.Msg;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public final class EconomyCommands {
    private static final long MAX = 1_000_000_000_000L;

    private EconomyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        for (String name : new String[]{"balance", "bal"}) {
            d.register(Commands.literal(name)
                    .executes(c -> {
                        ServerPlayer p = c.getSource().getPlayerOrException();
                        p.sendSystemMessage(Msg.info("Balance: " + Economy.format(Economy.get(p.getUUID()))));
                        return 1;
                    })
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(c -> {
                                ServerPlayer t = EntityArgument.getPlayer(c, "player");
                                c.getSource().sendSuccess(() -> Msg.info(t.getName().getString() + "'s balance: "
                                        + Economy.format(Economy.get(t.getUUID()))), false);
                                return 1;
                            })));
        }

        d.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, MAX))
                                .executes(c -> {
                                    ServerPlayer from = c.getSource().getPlayerOrException();
                                    ServerPlayer to = EntityArgument.getPlayer(c, "player");
                                    long amt = LongArgumentType.getLong(c, "amount");
                                    if (from.getUUID().equals(to.getUUID())) {
                                        from.sendSystemMessage(Msg.err("You can't pay yourself."));
                                        return 0;
                                    }
                                    if (!Economy.take(from.getUUID(), amt)) {
                                        from.sendSystemMessage(Msg.err("You don't have enough money."));
                                        return 0;
                                    }
                                    Economy.add(to.getUUID(), amt);
                                    from.sendSystemMessage(Msg.ok("Paid " + Economy.format(amt) + " to " + to.getName().getString() + "."));
                                    to.sendSystemMessage(Msg.ok("You received " + Economy.format(amt) + " from " + from.getName().getString() + "."));
                                    return 1;
                                }))));

        d.register(Commands.literal("baltop").executes(c -> {
            c.getSource().sendSuccess(() -> Msg.info("--- Richest players ---"), false);
            int i = 1;
            for (Map.Entry<String, Long> e : Economy.top(10)) {
                String line = i++ + ". " + Economy.nameOf(UUID.fromString(e.getKey())) + " - " + Economy.format(e.getValue());
                c.getSource().sendSuccess(() -> Msg.ok(line), false);
            }
            return 1;
        }));

        d.register(Commands.literal("eco")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, MAX)).executes(c -> {
                            ServerPlayer t = EntityArgument.getPlayer(c, "player");
                            long a = LongArgumentType.getLong(c, "amount");
                            Economy.add(t.getUUID(), a);
                            c.getSource().sendSuccess(() -> Msg.ok("Gave " + Economy.format(a) + " to " + t.getName().getString() + "."), true);
                            return 1;
                        }))))
                .then(Commands.literal("take").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, MAX)).executes(c -> {
                            ServerPlayer t = EntityArgument.getPlayer(c, "player");
                            long a = LongArgumentType.getLong(c, "amount");
                            Economy.set(t.getUUID(), Economy.get(t.getUUID()) - a);
                            c.getSource().sendSuccess(() -> Msg.ok("Took " + Economy.format(a) + " from " + t.getName().getString() + "."), true);
                            return 1;
                        }))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0, MAX)).executes(c -> {
                            ServerPlayer t = EntityArgument.getPlayer(c, "player");
                            long a = LongArgumentType.getLong(c, "amount");
                            Economy.set(t.getUUID(), a);
                            c.getSource().sendSuccess(() -> Msg.ok("Set " + t.getName().getString() + "'s balance to " + Economy.format(a) + "."), true);
                            return 1;
                        }))))
                .then(startNode())
                .then(mobHeartsNode()));
    }

    /** /eco start [amount] - the balance new players begin with. */
    private static LiteralArgumentBuilder<CommandSourceStack> startNode() {
        return Commands.literal("start")
                .executes(c -> {
                    c.getSource().sendSuccess(() -> Msg.info("New players start with " + Economy.format(Config.get().startingBalance)
                            + ". Change it with /eco start <amount>."), false);
                    return 1;
                })
                .then(Commands.argument("amount", LongArgumentType.longArg(0, MAX)).executes(c -> {
                    long a = LongArgumentType.getLong(c, "amount");
                    Config.get().startingBalance = a;
                    Config.save();
                    c.getSource().sendSuccess(() -> Msg.ok("Starting balance set to " + Economy.format(a)
                            + ". It applies to players who join for the first time from now on."), true);
                    return 1;
                }));
    }

    private static String hearts(double h) {
        return h == Math.floor(h) ? String.valueOf((long) h) : String.format("%.1f", h);
    }

    /** /eco mobhearts [hearts] - how many hearts of mob health are worth $1. */
    private static LiteralArgumentBuilder<CommandSourceStack> mobHeartsNode() {
        return Commands.literal("mobhearts")
                .executes(c -> {
                    double h = Config.get().healthPerDollar / 2.0;
                    c.getSource().sendSuccess(() -> Msg.info("Killing a mob pays $1 per " + hearts(h)
                            + " hearts of its health. Change it with /eco mobhearts <hearts>."), false);
                    return 1;
                })
                .then(Commands.argument("hearts", DoubleArgumentType.doubleArg(0.5, 1000)).executes(c -> {
                    double h = DoubleArgumentType.getDouble(c, "hearts");
                    Config.get().healthPerDollar = (int) Math.max(1, Math.round(h * 2));
                    Config.save();
                    double shown = Config.get().healthPerDollar / 2.0;
                    c.getSource().sendSuccess(() -> Msg.ok("Mobs now pay $1 per " + hearts(shown) + " hearts of health."), true);
                    return 1;
                }));
    }
}
