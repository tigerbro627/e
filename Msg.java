package dev.serverhub;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Msg {
    private Msg() {}

    public static MutableComponent ok(String s) { return Component.literal(s).withStyle(ChatFormatting.GREEN); }
    public static MutableComponent err(String s) { return Component.literal(s).withStyle(ChatFormatting.RED); }
    public static MutableComponent info(String s) { return Component.literal(s).withStyle(ChatFormatting.GOLD); }

    public static MutableComponent button(String label, String command, ChatFormatting color) {
        return Component.literal(label).withStyle(st -> st.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command)));
    }

    public static MutableComponent suggest(String label, String command, ChatFormatting color) {
        return Component.literal(label).withStyle(st -> st.withColor(color).withBold(true).withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand(command)));
    }

    public static String duration(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600, m = (s % 3600) / 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m";
        return s + "s";
    }
}
