package dev.serverhub;

import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AdminShop {
    public static class Entry {
        public String item;
        public long price;
        /** Full item data (enchantments, name, lore, custom data...). Null for plain items. */
        public JsonElement data;
        private transient ItemStack cache;

        public Entry() {}
        public Entry(String item, long price) { this.item = item; this.price = price; }

        /** A copy of the item sold by this entry (count 1), or EMPTY if invalid. */
        public ItemStack stack() {
            if (data != null) {
                if (cache == null) cache = ItemUtil.decode(data);
                return cache.copy();
            }
            Identifier id = item == null ? null : Identifier.tryParse(item);
            if (id == null) return ItemStack.EMPTY;
            Item i = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            return i == null ? ItemStack.EMPTY : new ItemStack(i);
        }
    }

    private static class Data {
        List<Entry> entries = new ArrayList<>();
    }

    private static Data data = new Data();
    private static boolean dirty;

    private AdminShop() {}

    public static void load() {
        Data d = Storage.read("shop.json", Data.class);
        if (d == null) {
            d = new Data();
            String[][] defaults = {
                    {"minecraft:pig_spawn_egg", "500"}, {"minecraft:cow_spawn_egg", "500"},
                    {"minecraft:sheep_spawn_egg", "500"}, {"minecraft:chicken_spawn_egg", "400"},
                    {"minecraft:horse_spawn_egg", "2000"}, {"minecraft:wolf_spawn_egg", "1500"},
                    {"minecraft:cat_spawn_egg", "1500"}, {"minecraft:fox_spawn_egg", "1500"},
                    {"minecraft:bee_spawn_egg", "1000"}, {"minecraft:villager_spawn_egg", "5000"}
            };
            for (String[] e : defaults) d.entries.add(new Entry(e[0], Long.parseLong(e[1])));
            data = d;
            dirty = true;
            save();
        }
        data = d;
        if (data.entries == null) data.entries = new ArrayList<>();
        data.entries.removeIf(e -> {
            boolean bad = e == null || e.stack().isEmpty();
            if (bad) ServerHub.LOGGER.warn("Removing invalid shop entry: {}", e == null ? "null" : e.item);
            return bad;
        });
    }

    public static void save() {
        if (dirty) {
            Storage.write("shop.json", data);
            dirty = false;
        }
    }

    public static List<Entry> entries() { return data.entries; }

    /** Finds a plain (no custom data) entry for an item id. */
    public static Entry find(String id) {
        for (Entry e : data.entries) if (e.data == null && e.item.equals(id)) return e;
        return null;
    }

    public static boolean set(String id, long price) {
        Entry e = find(id);
        boolean existed = e != null;
        if (e == null) data.entries.add(new Entry(id, price));
        else e.price = price;
        dirty = true;
        save();
        return existed;
    }

    public static boolean remove(String id) {
        boolean r = data.entries.removeIf(e -> e.data == null && e.item.equals(id));
        if (r) {
            dirty = true;
            save();
        }
        return r;
    }

    /** Adds an item with ALL of its data. @return its position number in the shop list. */
    public static int addStack(ItemStack held, long price) {
        ItemStack one = held.copy();
        one.setCount(1);
        Entry e = new Entry(BuiltInRegistries.ITEM.getKey(one.getItem()).toString(), price);
        e.data = ItemUtil.encode(one);
        data.entries.add(e);
        dirty = true;
        save();
        return data.entries.size();
    }

    public static boolean removeAt(int number) {
        if (number < 1 || number > data.entries.size()) return false;
        data.entries.remove(number - 1);
        dirty = true;
        save();
        return true;
    }

    public static boolean setPriceAt(int number, long price) {
        if (number < 1 || number > data.entries.size()) return false;
        data.entries.get(number - 1).price = price;
        dirty = true;
        save();
        return true;
    }
}
