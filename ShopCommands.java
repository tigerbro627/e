package dev.serverhub.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.serverhub.AdminShop;
import dev.serverhub.Economy;
import dev.serverhub.ItemUtil;
import dev.serverhub.Msg;
import dev.serverhub.SellPrices;
import dev.serverhub.gui.ShopGui;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ShopCommands {
    private static final long MAX_PRICE = 1_000_000_000L;

    private ShopCommands() {}

    private static String idOf(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Item item = ResourceArgument.getResource(c, "item", Registries.ITEM).value();
        return item == Items.AIR ? null : BuiltInRegistries.ITEM.getKey(item).toString();
    }


    /** Builds a 1-count stack (with any [components]) from a parsed item argument. */
    private static ItemStack stackOf(Object input) throws CommandSyntaxException {
        try {
            for (java.lang.reflect.Method m : input.getClass().getMethods()) {
                String n = m.getName();
                if (!n.equals("createItemStack") && !n.equals("createStack")) continue;
                Object r = null;
                if (m.getParameterCount() == 1) r = m.invoke(input, 1);
                else if (m.getParameterCount() == 2) r = m.invoke(input, 1, false);
                if (r instanceof ItemStack st) return st;
            }
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof CommandSyntaxException cse) throw cse;
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return ItemStack.EMPTY;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        for (String name : new String[]{"shop", "adminshop"}) {
            d.register(Commands.literal(name).executes(c -> {
                ShopGui.open(c.getSource().getPlayerOrException());
                return 1;
            }).then(Commands.literal("search").then(Commands.argument("text", StringArgumentType.greedyString()).executes(c -> {
                ShopGui.open(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "text"));
                return 1;
            }))));
        }

        d.register(Commands.literal("shopadmin")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                // /shopadmin add <item[data]> <price>  - e.g. diamond_sword[enchantments={sharpness:5},custom_name="Excalibur"]
                .then(Commands.literal("add").then(Commands.argument("item", ItemArgument.item(ctx))
                        .then(Commands.argument("price", LongArgumentType.longArg(0, MAX_PRICE)).executes(c -> {
                            ItemStack st = stackOf(ItemArgument.getItem(c, "item"));
                            if (st.isEmpty()) {
                                c.getSource().sendFailure(Msg.err("That item can't be sold."));
                                return 0;
                            }
                            long price = LongArgumentType.getLong(c, "price");
                            String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
                            boolean plain = ItemUtil.encode(st).equals(ItemUtil.encode(new ItemStack(st.getItem())));
                            String label;
                            if (plain) {
                                boolean existed = AdminShop.set(id, price);
                                label = (existed ? "Updated " : "Added ") + id;
                            } else {
                                int n = AdminShop.addStack(st, price);
                                label = "Added #" + n + " " + st.getHoverName().getString() + " (with item data)";
                            }
                            String msg = label + " at " + Economy.format(price) + ".";
                            c.getSource().sendSuccess(() -> Msg.ok(msg), true);
                            return 1;
                        }))))
                // /shopadmin addheld <price>  - the item in your hand WITH all its data
                .then(Commands.literal("addheld").then(Commands.argument("price", LongArgumentType.longArg(0, MAX_PRICE)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ItemStack held = p.getMainHandItem();
                    if (held.isEmpty()) {
                        c.getSource().sendFailure(Msg.err("Hold the item you want to add in your main hand."));
                        return 0;
                    }
                    long price = LongArgumentType.getLong(c, "price");
                    int n = AdminShop.addStack(held, price);
                    String nm = held.getHoverName().getString();
                    c.getSource().sendSuccess(() -> Msg.ok("Added #" + n + " " + nm + " (with all its data) at " + Economy.format(price) + "."), true);
                    return 1;
                })))
                .then(Commands.literal("price").then(Commands.argument("item", ResourceArgument.resource(ctx, Registries.ITEM))
                        .then(Commands.argument("price", LongArgumentType.longArg(0, MAX_PRICE)).executes(c -> {
                            String id = idOf(c);
                            long price = LongArgumentType.getLong(c, "price");
                            if (id == null || AdminShop.find(id) == null) {
                                c.getSource().sendFailure(Msg.err("That plain item isn't in the shop. Use /shopadmin add, or /shopadmin priceat <number>."));
                                return 0;
                            }
                            AdminShop.set(id, price);
                            c.getSource().sendSuccess(() -> Msg.ok("Set " + id + " to " + Economy.format(price) + "."), true);
                            return 1;
                        }))))
                .then(Commands.literal("remove").then(Commands.argument("item", ResourceArgument.resource(ctx, Registries.ITEM)).executes(c -> {
                    String id = idOf(c);
                    if (id == null || !AdminShop.remove(id)) {
                        c.getSource().sendFailure(Msg.err("That plain item isn't in the shop. Use /shopadmin removeat <number>."));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Msg.ok("Removed " + id + " from the shop."), true);
                    return 1;
                })))
                // by list number (works for items with custom data too)
                .then(Commands.literal("removeat").then(Commands.argument("number", IntegerArgumentType.integer(1)).executes(c -> {
                    int n = IntegerArgumentType.getInteger(c, "number");
                    if (!AdminShop.removeAt(n)) {
                        c.getSource().sendFailure(Msg.err("No shop item with that number. Use /shopadmin list."));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Msg.ok("Removed #" + n + " from the shop."), true);
                    return 1;
                })))
                .then(Commands.literal("priceat").then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .then(Commands.argument("price", LongArgumentType.longArg(0, MAX_PRICE)).executes(c -> {
                            int n = IntegerArgumentType.getInteger(c, "number");
                            long price = LongArgumentType.getLong(c, "price");
                            if (!AdminShop.setPriceAt(n, price)) {
                                c.getSource().sendFailure(Msg.err("No shop item with that number. Use /shopadmin list."));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> Msg.ok("Set #" + n + " to " + Economy.format(price) + "."), true);
                            return 1;
                        }))))
                .then(Commands.literal("sell").then(Commands.argument("item", ResourceArgument.resource(ctx, Registries.ITEM))
                        .then(Commands.argument("price", LongArgumentType.longArg(1, MAX_PRICE)).executes(c -> {
                            String id = idOf(c);
                            if (id == null) {
                                c.getSource().sendFailure(Msg.err("That item can't be sold."));
                                return 0;
                            }
                            long price = LongArgumentType.getLong(c, "price");
                            SellPrices.set(id, price);
                            c.getSource().sendSuccess(() -> Msg.ok("Players can now /sell " + id + " for " + Economy.format(price) + " each."), true);
                            return 1;
                        }))))
                .then(Commands.literal("unsell").then(Commands.argument("item", ResourceArgument.resource(ctx, Registries.ITEM)).executes(c -> {
                    String id = idOf(c);
                    if (id == null || !SellPrices.remove(id)) {
                        c.getSource().sendFailure(Msg.err("That item has no sell price."));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Msg.ok(id + " can no longer be sold."), true);
                    return 1;
                })))
                .then(Commands.literal("list").executes(c -> {
                    c.getSource().sendSuccess(() -> Msg.info("--- Admin shop (" + AdminShop.entries().size() + " items) ---"), false);
                    int i = 1;
                    for (AdminShop.Entry e : AdminShop.entries()) {
                        String line = "#" + i++ + " " + e.item + (e.data != null ? " (custom data)" : "") + " - " + Economy.format(e.price);
                        c.getSource().sendSuccess(() -> Msg.ok(line), false);
                    }
                    return 1;
                })));
    }
}
