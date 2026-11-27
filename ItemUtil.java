package dev.serverhub;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {
    private ItemUtil() {}

    private static RegistryOps<JsonElement> ops() {
        return RegistryOps.create(JsonOps.INSTANCE, ServerHub.server.registryAccess());
    }

    public static JsonElement encode(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(ops(), stack).getOrThrow();
    }

    public static ItemStack decode(JsonElement json) {
        return ItemStack.CODEC.parse(ops(), json).result().orElse(ItemStack.EMPTY);
    }

    /** Puts the item in the inventory, dropping whatever doesn't fit at the player's feet. */
    public static void give(ServerPlayer p, ItemStack stack) {
        ItemStack s = stack.copy();
        p.getInventory().add(s);
        if (!s.isEmpty()) p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), s));
    }

    /** True if every word of the query appears in the item's name or id. */
    public static boolean matches(ItemStack s, String query) {
        if (query == null || query.isBlank()) return true;
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().replace('_', ' ');
        String hay = (s.getHoverName().getString() + " " + id).toLowerCase(Locale.ROOT);
        for (String w : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!hay.contains(w)) return false;
        }
        return true;
    }

    public static ItemStack withLore(ItemStack base, List<Component> extra) {
        ItemStack s = base.copy();
        List<Component> lines = new ArrayList<>();
        ItemLore old = s.get(DataComponents.LORE);
        if (old != null) lines.addAll(old.lines());
        lines.addAll(extra);
        s.set(DataComponents.LORE, new ItemLore(lines));
        return s;
    }
}
