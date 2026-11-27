package dev.serverhub;

import com.google.gson.JsonElement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class Auction {
    public static class Listing {
        public String id = UUID.randomUUID().toString();
        public String seller;
        public String sellerName;
        public long price;
        public long expiresAt;
        public JsonElement item;
        private transient ItemStack cache;

        /** Returns a copy of the listed stack (EMPTY if it can't be decoded). */
        public ItemStack stack() {
            if (cache == null) cache = ItemUtil.decode(item);
            return cache.copy();
        }
    }

    private static class Data {
        List<Listing> listings = new ArrayList<>();
        Map<String, List<JsonElement>> claims = new HashMap<>();
    }

    private static Data data = new Data();
    private static boolean dirty;

    private Auction() {}

    public static void load() {
        Data d = Storage.read("auction.json", Data.class);
        if (d != null) data = d;
        if (data.listings == null) data.listings = new ArrayList<>();
        if (data.claims == null) data.claims = new HashMap<>();
    }

    public static void save() {
        if (dirty) {
            Storage.write("auction.json", data);
            dirty = false;
        }
    }

    /** Cheapest listings first. */
    public static List<Listing> sorted() {
        List<Listing> l = new ArrayList<>(data.listings);
        l.sort(Comparator.comparingLong((Listing x) -> x.price).thenComparingLong(x -> x.expiresAt));
        return l;
    }

    public static boolean exists(Listing l) { return data.listings.contains(l); }

    /** @return an error message, or null on success (the seller is messaged on success). */
    public static String create(ServerPlayer p, ItemStack stack, long price) {
        Config cfg = Config.get();
        if (price < cfg.auctionMinPrice) return "Minimum price is " + Economy.format(cfg.auctionMinPrice) + ".";
        String me = p.getUUID().toString();
        long mine = data.listings.stream().filter(l -> l.seller.equals(me)).count();
        if (mine >= cfg.maxListingsPerPlayer) return "You can only have " + cfg.maxListingsPerPlayer + " listings at once.";

        Listing l = new Listing();
        l.seller = me;
        l.sellerName = p.getName().getString();
        l.price = price;
        l.expiresAt = System.currentTimeMillis() + cfg.auctionDurationHours * 3_600_000L;
        l.item = ItemUtil.encode(stack);
        data.listings.add(l);
        dirty = true;
        p.sendSystemMessage(Msg.ok("Listed " + stack.getCount() + "x " + stack.getHoverName().getString()
                + " for " + Economy.format(price) + "."));
        return null;
    }

    public static String buy(ServerPlayer buyer, Listing l) {
        if (!exists(l)) return "That listing is no longer available.";
        if (l.seller.equals(buyer.getUUID().toString())) return "You can't buy your own listing.";
        ItemStack stack = l.stack();
        if (stack.isEmpty()) return "That listing is corrupted.";
        if (!Economy.take(buyer.getUUID(), l.price)) return "You need " + Economy.format(l.price) + ".";

        data.listings.remove(l);
        dirty = true;
        long tax = l.price * Config.get().auctionTaxPercent / 100;
        UUID seller = UUID.fromString(l.seller);
        Economy.add(seller, l.price - tax);
        ItemUtil.give(buyer, stack);

        buyer.sendSystemMessage(Msg.ok("Bought " + stack.getCount() + "x " + stack.getHoverName().getString()
                + " for " + Economy.format(l.price) + "."));
        ServerPlayer sp = ServerHub.server.getPlayerList().getPlayer(seller);
        if (sp != null) {
            sp.sendSystemMessage(Msg.ok(buyer.getName().getString() + " bought your " + stack.getHoverName().getString()
                    + ". You received " + Economy.format(l.price - tax) + "."));
        }
        return null;
    }

    public static String cancel(ServerPlayer p, Listing l) {
        if (!exists(l)) return "That listing is no longer available.";
        if (!l.seller.equals(p.getUUID().toString())) return "That isn't your listing.";
        data.listings.remove(l);
        dirty = true;
        ItemStack stack = l.stack();
        if (!stack.isEmpty()) ItemUtil.give(p, stack);
        p.sendSystemMessage(Msg.ok("Listing cancelled and item returned."));
        return null;
    }

    public static int claimCount(UUID id) {
        List<JsonElement> c = data.claims.get(id.toString());
        return c == null ? 0 : c.size();
    }

    public static int claim(ServerPlayer p) {
        List<JsonElement> c = data.claims.remove(p.getUUID().toString());
        if (c == null) return 0;
        dirty = true;
        int n = 0;
        for (JsonElement e : c) {
            ItemStack s = ItemUtil.decode(e);
            if (!s.isEmpty()) {
                ItemUtil.give(p, s);
                n++;
            }
        }
        return n;
    }

    /** Moves expired listings into the seller's claim box. */
    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Listing> it = data.listings.iterator();
        while (it.hasNext()) {
            Listing l = it.next();
            if (l.expiresAt > now) continue;
            it.remove();
            data.claims.computeIfAbsent(l.seller, k -> new ArrayList<>()).add(l.item);
            dirty = true;
            ServerPlayer sp = ServerHub.server.getPlayerList().getPlayer(UUID.fromString(l.seller));
            if (sp != null) sp.sendSystemMessage(Msg.info("One of your auction listings expired. Use /ah claim to get the item back."));
        }
    }
}
