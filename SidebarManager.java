package dev.serverhub;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.*;

/** Per-player sidebar showing name and money. Sent with packets, so each player sees their own values. */
public final class SidebarManager {
    private static final String OBJ = "serverhub";
    private static Objective objective;
    private static final Map<UUID, String> last = new HashMap<>();
    private static final Set<UUID> hidden = new HashSet<>();

    private SidebarManager() {}

    private static Objective obj() {
        if (objective == null) {
            Scoreboard board = new Scoreboard();
            objective = board.addObjective(OBJ, ObjectiveCriteria.DUMMY,
                    Component.literal(Config.get().sidebarTitle).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
        }
        return objective;
    }

    public static void join(ServerPlayer p) {
        if (!Config.get().sidebarEnabled || hidden.contains(p.getUUID())) return;
        last.remove(p.getUUID());
        p.connection.send(new ClientboundSetObjectivePacket(obj(), ClientboundSetObjectivePacket.METHOD_ADD));
        p.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, obj()));
        update(p, true);
    }

    public static void leave(UUID id) {
        last.remove(id);
    }

    /** @return true if the sidebar is now shown. */
    public static boolean toggle(ServerPlayer p) {
        UUID id = p.getUUID();
        if (hidden.remove(id)) {
            join(p);
            return true;
        }
        hidden.add(id);
        p.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, (Objective) null));
        return false;
    }

    private static void update(ServerPlayer p, boolean force) {
        String name = p.getName().getString();
        String money = Economy.format(Economy.get(p.getUUID()));
        String sig = name + "|" + money;
        if (!force && sig.equals(last.get(p.getUUID()))) return;
        last.put(p.getUUID(), sig);

        List<Component> lines = List.<Component>of(
                Component.literal(" "),
                Component.literal("Name: ").withStyle(ChatFormatting.GRAY).append(Component.literal(name).withStyle(ChatFormatting.AQUA)),
                Component.literal("Money: ").withStyle(ChatFormatting.GRAY).append(Component.literal(money).withStyle(ChatFormatting.GREEN)),
                Component.literal("  "));
        for (int i = 0; i < lines.size(); i++) {
            p.connection.send(new ClientboundSetScorePacket("line" + i, OBJ, lines.size() - i,
                    Optional.of(lines.get(i)), Optional.<NumberFormat>of(BlankFormat.INSTANCE)));
        }
    }

    public static void tick() {
        if (!Config.get().sidebarEnabled) return;
        for (ServerPlayer p : ServerHub.server.getPlayerList().getPlayers()) {
            if (!hidden.contains(p.getUUID())) update(p, false);
        }
    }
}
