package dev.serverhub;

import java.util.*;

public final class Economy {
    private static class Data {
        Map<String, Long> balances = new HashMap<>();
        Map<String, String> names = new HashMap<>();
    }

    private static Data data = new Data();
    private static boolean dirty;

    private Economy() {}

    public static void load() {
        Data d = Storage.read("balances.json", Data.class);
        if (d != null) data = d;
        if (data.balances == null) data.balances = new HashMap<>();
        if (data.names == null) data.names = new HashMap<>();
    }

    public static void save() {
        if (dirty) {
            Storage.write("balances.json", data);
            dirty = false;
        }
    }

    public static long get(UUID id) {
        Long v = data.balances.get(id.toString());
        if (v == null) {
            v = Config.get().startingBalance;
            data.balances.put(id.toString(), v);
            dirty = true;
        }
        return v;
    }

    public static void set(UUID id, long amount) {
        data.balances.put(id.toString(), Math.max(0, amount));
        dirty = true;
    }

    public static void add(UUID id, long amount) {
        long r = get(id) + amount;
        if (r < 0) r = Long.MAX_VALUE; // overflow guard
        set(id, r);
    }

    /** @return false (and changes nothing) if the player can't afford it. */
    public static boolean take(UUID id, long amount) {
        long bal = get(id);
        if (bal < amount) return false;
        set(id, bal - amount);
        return true;
    }

    public static void setName(UUID id, String name) {
        data.names.put(id.toString(), name);
        dirty = true;
    }

    public static String nameOf(UUID id) {
        return data.names.getOrDefault(id.toString(), id.toString().substring(0, 8));
    }

    public static String format(long amount) {
        return Config.get().currencySymbol + String.format("%,d", amount);
    }

    public static List<Map.Entry<String, Long>> top(int n) {
        return data.balances.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(n).toList();
    }
}
