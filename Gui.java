package dev.serverhub.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public final class Gui {
    private Gui() {}

    public static MutableComponent text(String s, ChatFormatting color) {
        return Component.literal(s).withStyle(st -> st.withColor(color).withItalic(false));
    }

    public static ItemStack icon(ItemLike item, String name, ChatFormatting color, Component... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, text(name, color));
        if (lore.length > 0) s.set(DataComponents.LORE, new ItemLore(List.of(lore)));
        return s;
    }

    /** Looks an item up by id (e.g. "minecraft:lime_stained_glass_pane"), falling back if it doesn't exist. */
    public static ItemLike itemOr(String id, ItemLike fallback) {
        net.minecraft.world.item.Item i = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
        return i != null ? i : fallback;
    }

    public static ItemStack filler() {
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:gray_stained_glass_pane")).orElse(Items.PAPER));
        s.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return s;
    }
}
