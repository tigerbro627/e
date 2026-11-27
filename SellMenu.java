package dev.serverhub.gui;

import dev.serverhub.Economy;
import dev.serverhub.ItemUtil;
import dev.serverhub.Msg;
import dev.serverhub.SellPrices;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Top 45 slots are free for the player's items; the bottom row has the sell/close buttons. */
public final class SellMenu extends ChestMenu {
    private static final int FREE = 45, SELL = 48, CLOSE = 50;
    private final SimpleContainer box;

    private SellMenu(int id, Inventory inv, SimpleContainer box) {
        super(MenuType.GENERIC_9x6, id, inv, box, 6);
        this.box = box;
        refresh();
    }

    public static void open(ServerPlayer p) {
        SimpleContainer box = new SimpleContainer(54);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SellMenu(id, inv, box), Component.literal("Sell Items")));
    }

    private void refresh() {
        long total = 0;
        int unsellable = 0;
        for (int i = 0; i < FREE; i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty()) continue;
            Long pr = SellPrices.price(s);
            if (pr == null) unsellable += s.getCount();
            else total += pr * s.getCount();
        }
        for (int i = FREE; i < 54; i++) box.setItem(i, Gui.filler());
        box.setItem(SELL, Gui.icon(Gui.itemOr("minecraft:lime_stained_glass_pane", Items.EMERALD_BLOCK), "SELL", ChatFormatting.GREEN,
                Gui.text("Total: " + Economy.format(total), ChatFormatting.GOLD),
                Gui.text(unsellable > 0 ? unsellable + " item(s) can't be sold and stay here" : "Click to sell everything above", ChatFormatting.GRAY)));
        box.setItem(CLOSE, Gui.icon(Gui.itemOr("minecraft:red_stained_glass_pane", Items.BARRIER), "Close", ChatFormatting.RED,
                Gui.text("Unsold items are returned to you", ChatFormatting.GRAY)));
        box.setItem(49, Gui.icon(Items.PAPER, "How it works", ChatFormatting.AQUA,
                Gui.text("Put items in the slots above.", ChatFormatting.GRAY),
                Gui.text("See prices with /sellprices", ChatFormatting.GRAY)));
    }

    private void sell(ServerPlayer p) {
        long total = 0;
        int count = 0;
        for (int i = 0; i < FREE; i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty()) continue;
            Long pr = SellPrices.price(s);
            if (pr == null) continue;
            total += pr * s.getCount();
            count += s.getCount();
            box.setItem(i, ItemStack.EMPTY);
        }
        if (total <= 0) {
            p.sendSystemMessage(Msg.err("Nothing in the window can be sold."));
            return;
        }
        Economy.add(p.getUUID(), total);
        p.sendSystemMessage(Msg.ok("Sold " + count + " item(s) for " + Economy.format(total) + "."));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput type, Player player) {
        if (slotId >= FREE && slotId < 54) { // button row: never moves items
            if (player instanceof ServerPlayer sp && type == ContainerInput.PICKUP) {
                if (slotId == CLOSE) {
                    sp.closeContainer();
                    return;
                }
                if (slotId == SELL) sell(sp);
            }
            refresh();
            this.broadcastFullState();
            return;
        }
        if (type == ContainerInput.PICKUP || type == ContainerInput.QUICK_MOVE) {
            super.clicked(slotId, button, type, player);
        } else {
            this.broadcastFullState();
        }
        refresh();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || index >= FREE && index < 54) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < FREE) {
            if (!this.moveItemStackTo(stack, 54, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, 0, FREE, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer sp) {
            for (int i = 0; i < FREE; i++) {
                ItemStack s = box.getItem(i);
                if (!s.isEmpty()) {
                    ItemUtil.give(sp, s);
                    box.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        super.removed(player);
    }
}
