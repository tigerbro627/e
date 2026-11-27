package dev.serverhub.gui;

import dev.serverhub.Economy;
import dev.serverhub.ItemUtil;
import dev.serverhub.SellPrices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Read-only list of what /sell pays, cheapest first. */
public final class SellPricesGui {
    private final SimpleContainer container = new SimpleContainer(54);
    private int page;

    private SellPricesGui() {}

    public static void open(ServerPlayer p) {
        SellPricesGui g = new SellPricesGui();
        g.render();
        GuiMenu.open(p, Component.literal("Sell Prices"), g.container, g::click);
    }

    private void render() {
        container.clearContent();
        List<Map.Entry<String, Long>> all = new ArrayList<>(SellPrices.all().entrySet());
        all.sort((a, b) -> Long.compare(a.getValue(), b.getValue()));
        int pages = Math.max(1, (all.size() + 44) / 45);
        page = Math.max(0, Math.min(page, pages - 1));
        List<Map.Entry<String, Long>> view = all.subList(page * 45, Math.min(all.size(), page * 45 + 45));
        int slot = 0;
        for (Map.Entry<String, Long> e : view) {
            Identifier id = Identifier.tryParse(e.getKey());
            Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) continue;
            container.setItem(slot++, ItemUtil.withLore(new ItemStack(item), List.of(
                    Gui.text("Sells for " + Economy.format(e.getValue()) + " each", ChatFormatting.GOLD))));
        }
        for (int i = 45; i < 54; i++) container.setItem(i, Gui.filler());
        if (page > 0) container.setItem(45, Gui.icon(Items.ARROW, "Previous page", ChatFormatting.YELLOW));
        container.setItem(49, Gui.icon(Items.PAPER, "Page " + (page + 1) + "/" + pages, ChatFormatting.GRAY));
        if (page < pages - 1) container.setItem(53, Gui.icon(Items.ARROW, "Next page", ChatFormatting.YELLOW));
    }

    private void click(ServerPlayer p, int slot, ContainerInput type, int button) {
        if (slot == 45) page--;
        else if (slot == 53) page++;
        render();
    }
}
