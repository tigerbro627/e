package dev.serverhub.gui;

import dev.serverhub.AdminShop;
import dev.serverhub.Economy;
import dev.serverhub.ItemUtil;
import dev.serverhub.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShopGui {
    private final SimpleContainer container = new SimpleContainer(54);
    private final String filter;
    private int page;
    private List<AdminShop.Entry> view = List.of();

    private ShopGui(String filter) { this.filter = filter; }

    public static void open(ServerPlayer p) { open(p, null); }

    public static void open(ServerPlayer p, String filter) {
        if (filter != null && filter.isBlank()) filter = null;
        ShopGui g = new ShopGui(filter);
        g.render(p);
        String title = filter == null ? "Admin Shop" : "Shop: " + (filter.length() > 20 ? filter.substring(0, 20) : filter);
        GuiMenu.open(p, Component.literal(title), g.container, g::click);
    }

    private void render(ServerPlayer p) {
        container.clearContent();
        List<AdminShop.Entry> all = new ArrayList<>();
        for (AdminShop.Entry e : AdminShop.entries()) {
            ItemStack s = e.stack();
            if (!s.isEmpty() && ItemUtil.matches(s, filter)) all.add(e);
        }
        all.sort(Comparator.comparingLong((AdminShop.Entry e) -> e.price)); // cheapest first
        int pages = Math.max(1, (all.size() + 44) / 45);
        page = Math.max(0, Math.min(page, pages - 1));
        view = all.subList(page * 45, Math.min(all.size(), page * 45 + 45));

        for (int i = 0; i < view.size(); i++) {
            AdminShop.Entry e = view.get(i);
            ItemStack base = e.stack();
            int max = base.getMaxStackSize();
            container.setItem(i, ItemUtil.withLore(base, List.of(
                    Gui.text("", ChatFormatting.GRAY),
                    Gui.text("Price: " + Economy.format(e.price) + " each", ChatFormatting.GOLD),
                    Gui.text("Click: buy 1", ChatFormatting.GREEN),
                    Gui.text("Shift-click: buy " + max + " (" + Economy.format(e.price * max) + ")", ChatFormatting.GREEN))));
        }

        for (int i = 45; i < 54; i++) container.setItem(i, Gui.filler());
        if (page > 0) container.setItem(45, Gui.icon(Items.ARROW, "Previous page", ChatFormatting.YELLOW));
        container.setItem(47, Gui.icon(Items.COMPASS, "Search", ChatFormatting.AQUA,
                Gui.text("Click, then click the chat message", ChatFormatting.GRAY),
                Gui.text("or type /shop search <text>", ChatFormatting.GRAY)));
        container.setItem(49, Gui.icon(Items.GOLD_INGOT, "Balance: " + Economy.format(Economy.get(p.getUUID())), ChatFormatting.GOLD,
                Gui.text("Page " + (page + 1) + "/" + pages + " - cheapest first", ChatFormatting.GRAY)));
        if (filter != null) container.setItem(51, Gui.icon(Items.BARRIER, "Clear search", ChatFormatting.RED,
                Gui.text("Searching: " + filter, ChatFormatting.GRAY)));
        if (page < pages - 1) container.setItem(53, Gui.icon(Items.ARROW, "Next page", ChatFormatting.YELLOW));
    }

    private void click(ServerPlayer p, int slot, ContainerInput type, int button) {
        if (slot < 45) {
            if (slot >= view.size()) return;
            AdminShop.Entry e = view.get(slot);
            ItemStack base = e.stack();
            if (base.isEmpty()) return;
            int max = base.getMaxStackSize();
            int qty = type == ContainerInput.QUICK_MOVE ? max : 1;
            long total = e.price * qty;
            if (!Economy.take(p.getUUID(), total)) {
                p.sendSystemMessage(Msg.err("You need " + Economy.format(total) + "."));
            } else {
                int left = qty;
                while (left > 0) {
                    int n = Math.min(left, max);
                    ItemStack give = base.copy();
                    give.setCount(n);
                    ItemUtil.give(p, give);
                    left -= n;
                }
                p.sendSystemMessage(Msg.ok("Bought " + qty + "x " + base.getHoverName().getString()
                        + " for " + Economy.format(total) + "."));
            }
        } else if (slot == 45) {
            page--;
        } else if (slot == 53) {
            page++;
        } else if (slot == 47) {
            p.closeContainer();
            p.sendSystemMessage(Msg.info("Search the shop: ").append(Msg.suggest("[Click here, then type what to find]", "/shop search ", ChatFormatting.GREEN)));
            return;
        } else if (slot == 51 && filter != null) {
            open(p);
            return;
        }
        render(p);
    }
}
