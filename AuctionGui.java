package dev.serverhub.gui;

import dev.serverhub.Auction;
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
import java.util.List;

public final class AuctionGui {
    private final SimpleContainer container = new SimpleContainer(54);
    private final String filter;
    private int page;
    private List<Auction.Listing> view = List.of();

    private AuctionGui(String filter) { this.filter = filter; }

    public static void open(ServerPlayer p) { open(p, null); }

    public static void open(ServerPlayer p, String filter) {
        if (filter != null && filter.isBlank()) filter = null;
        AuctionGui g = new AuctionGui(filter);
        g.render(p);
        String title = filter == null ? "Auction House" : "Auction: " + (filter.length() > 20 ? filter.substring(0, 20) : filter);
        GuiMenu.open(p, Component.literal(title), g.container, g::click);
    }

    private void render(ServerPlayer p) {
        container.clearContent();
        List<Auction.Listing> all = new ArrayList<>();
        for (Auction.Listing l : Auction.sorted()) { // already cheapest first
            if (filter == null || ItemUtil.matches(l.stack(), filter)) all.add(l);
        }
        int pages = Math.max(1, (all.size() + 44) / 45);
        page = Math.max(0, Math.min(page, pages - 1));
        view = all.subList(page * 45, Math.min(all.size(), page * 45 + 45));

        long now = System.currentTimeMillis();
        String me = p.getUUID().toString();
        for (int i = 0; i < view.size(); i++) {
            Auction.Listing l = view.get(i);
            ItemStack s = l.stack();
            if (s.isEmpty()) {
                container.setItem(i, Gui.icon(Items.BARRIER, "Corrupted listing", ChatFormatting.RED));
                continue;
            }
            boolean mine = l.seller.equals(me);
            container.setItem(i, ItemUtil.withLore(s, List.of(
                    Gui.text("", ChatFormatting.GRAY),
                    Gui.text("Price: " + Economy.format(l.price), ChatFormatting.GOLD),
                    Gui.text("Seller: " + l.sellerName, ChatFormatting.GRAY),
                    Gui.text("Expires in: " + Msg.duration(l.expiresAt - now), ChatFormatting.GRAY),
                    Gui.text(mine ? "Click to cancel your listing" : "Click to buy", mine ? ChatFormatting.RED : ChatFormatting.GREEN))));
        }

        for (int i = 45; i < 54; i++) container.setItem(i, Gui.filler());
        if (page > 0) container.setItem(45, Gui.icon(Items.ARROW, "Previous page", ChatFormatting.YELLOW));
        container.setItem(47, Gui.icon(Items.COMPASS, "Search", ChatFormatting.AQUA,
                Gui.text("Click, then click the chat message", ChatFormatting.GRAY),
                Gui.text("or type /ah search <text>", ChatFormatting.GRAY)));
        container.setItem(48, Gui.icon(Items.OAK_SIGN, "How to sell", ChatFormatting.AQUA,
                Gui.text("Hold an item and run", ChatFormatting.GRAY),
                Gui.text("/ah sell <price> [amount]", ChatFormatting.WHITE)));
        container.setItem(49, Gui.icon(Items.GOLD_INGOT, "Balance: " + Economy.format(Economy.get(p.getUUID())), ChatFormatting.GOLD,
                Gui.text("Page " + (page + 1) + "/" + pages + " - cheapest first", ChatFormatting.GRAY)));
        int claims = Auction.claimCount(p.getUUID());
        container.setItem(50, Gui.icon(Items.ENDER_CHEST, "Claim expired items (" + claims + ")", ChatFormatting.LIGHT_PURPLE));
        if (filter != null) container.setItem(51, Gui.icon(Items.BARRIER, "Clear search", ChatFormatting.RED,
                Gui.text("Searching: " + filter, ChatFormatting.GRAY)));
        if (page < pages - 1) container.setItem(53, Gui.icon(Items.ARROW, "Next page", ChatFormatting.YELLOW));
    }

    private void click(ServerPlayer p, int slot, ContainerInput type, int button) {
        if (slot < 45) {
            if (slot >= view.size()) return;
            Auction.Listing l = view.get(slot);
            String err = l.seller.equals(p.getUUID().toString()) ? Auction.cancel(p, l) : Auction.buy(p, l);
            if (err != null) p.sendSystemMessage(Msg.err(err));
        } else if (slot == 45) {
            page--;
        } else if (slot == 53) {
            page++;
        } else if (slot == 47) {
            p.closeContainer();
            p.sendSystemMessage(Msg.info("Search the auction house: ").append(Msg.suggest("[Click here, then type what to find]", "/ah search ", ChatFormatting.GREEN)));
            return;
        } else if (slot == 51 && filter != null) {
            open(p);
            return;
        } else if (slot == 50) {
            int n = Auction.claim(p);
            p.sendSystemMessage(n > 0 ? Msg.ok("Claimed " + n + " item(s).") : Msg.err("Nothing to claim."));
        }
        render(p);
    }
}
