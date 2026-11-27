package dev.serverhub;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** What the server pays per item in /sell. Edit with /shopadmin sell <item> <price>. */
public final class SellPrices {
    private static class Data {
        Map<String, Long> prices = new LinkedHashMap<>();
    }

    private static Data data = new Data();

    private SellPrices() {}

    public static void load() {
        Data d = Storage.read("sellprices.json", Data.class);
        if (d == null) {
            d = new Data();
            String[][] defaults = {
                    {"dirt", "1"}, {"cobblestone", "1"}, {"stone", "2"}, {"sand", "1"}, {"gravel", "1"},
                    {"oak_log", "3"}, {"spruce_log", "3"}, {"birch_log", "3"}, {"wheat", "2"}, {"carrot", "2"},
                    {"potato", "2"}, {"rotten_flesh", "1"}, {"bone", "2"}, {"string", "2"}, {"coal", "5"},
                    {"copper_ingot", "5"}, {"redstone", "3"}, {"lapis_lazuli", "3"}, {"iron_ingot", "15"},
                    {"gold_ingot", "25"}, {"quartz", "4"}, {"gunpowder", "8"}, {"ender_pearl", "40"},
                    {"blaze_rod", "20"}, {"emerald", "100"}, {"diamond", "150"}
            };
            for (String[] e : defaults) d.prices.put("minecraft:" + e[0], Long.parseLong(e[1]));
            data = d;
            save();
        }
        data = d;
        if (data.prices == null) data.prices = new LinkedHashMap<>();
    }

    private static void save() {
        Storage.write("sellprices.json", data);
    }

    /** @return the price per item, or null if this item can't be sold. */
    public static Long price(ItemStack s) {
        return data.prices.get(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    public static void set(String id, long price) {
        data.prices.put(id, price);
        save();
    }

    public static boolean remove(String id) {
        boolean r = data.prices.remove(id) != null;
        if (r) save();
        return r;
    }

    public static Map<String, Long> all() { return data.prices; }
}
