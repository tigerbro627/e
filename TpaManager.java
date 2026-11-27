package dev.serverhub;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class TpaManager {
    private static final class Request {
        final UUID from, to;
        final boolean here; // true = "to" teleports to "from"
        final long expiresAt;
        Request(UUID from, UUID to, boolean here, long expiresAt) {
            this.from = from; this.to = to; this.here = here; this.expiresAt = expiresAt;
        }
    }

    private static final List<Request> requests = new ArrayList<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    private TpaManager() {}

    private static ServerPlayer player(UUID id) {
        return ServerHub.server.getPlayerList().getPlayer(id);
    }

    public static void send(ServerPlayer from, ServerPlayer to, boolean here) {
        if (from.getUUID().equals(to.getUUID())) {
            from.sendSystemMessage(Msg.err("You can't send a request to yourself."));
            return;
        }
        long now = System.currentTimeMillis();
        Long cd = cooldowns.get(from.getUUID());
        if (cd != null && cd > now) {
            from.sendSystemMessage(Msg.err("Please wait " + Msg.duration(cd - now) + " before teleporting again."));
            return;
        }
        requests.removeIf(r -> r.from.equals(from.getUUID())); // one outstanding request per player
        long timeout = Config.get().tpaTimeoutSeconds * 1000L;
        requests.add(new Request(from.getUUID(), to.getUUID(), here, now + timeout));

        String fromName = from.getName().getString();
        from.sendSystemMessage(Msg.ok("Request sent to " + to.getName().getString() + ". It expires in "
                + Msg.duration(timeout) + ". Use /tpcancel to cancel."));
        MutableComponent msg = Msg.info(fromName + (here ? " wants you to teleport to them. " : " wants to teleport to you. "))
                .append(Msg.button("[Accept]", "/tpaccept " + fromName, ChatFormatting.GREEN))
                .append(Msg.info(" "))
                .append(Msg.button("[Deny]", "/tpdeny " + fromName, ChatFormatting.RED));
        to.sendSystemMessage(msg);
    }

    private static Request find(UUID to, ServerPlayer fromOrNull) {
        long now = System.currentTimeMillis();
        Request best = null;
        for (Request r : requests) {
            if (!r.to.equals(to) || r.expiresAt <= now) continue;
            if (fromOrNull != null && !r.from.equals(fromOrNull.getUUID())) continue;
            if (best == null || r.expiresAt > best.expiresAt) best = r; // newest
        }
        return best;
    }

    public static void accept(ServerPlayer target, ServerPlayer fromOrNull) {
        Request r = find(target.getUUID(), fromOrNull);
        if (r == null) {
            target.sendSystemMessage(Msg.err("You have no pending teleport request."));
            return;
        }
        requests.remove(r);
        ServerPlayer sender = player(r.from);
        if (sender == null) {
            target.sendSystemMessage(Msg.err("That player is no longer online."));
            return;
        }
        if (r.here) teleport(target, sender);
        else teleport(sender, target);
        cooldowns.put(r.from, System.currentTimeMillis() + Config.get().tpaCooldownSeconds * 1000L);
        sender.sendSystemMessage(Msg.ok(target.getName().getString() + " accepted your request."));
        target.sendSystemMessage(Msg.ok("Teleport request accepted."));
    }

    public static void deny(ServerPlayer target, ServerPlayer fromOrNull) {
        Request r = find(target.getUUID(), fromOrNull);
        if (r == null) {
            target.sendSystemMessage(Msg.err("You have no pending teleport request."));
            return;
        }
        requests.remove(r);
        target.sendSystemMessage(Msg.ok("Teleport request denied."));
        ServerPlayer sender = player(r.from);
        if (sender != null) sender.sendSystemMessage(Msg.err(target.getName().getString() + " denied your request."));
    }

    public static void cancel(ServerPlayer from) {
        boolean removed = requests.removeIf(r -> r.from.equals(from.getUUID()));
        from.sendSystemMessage(removed ? Msg.ok("Your request was cancelled.") : Msg.err("You have no outgoing request."));
    }

    private static void teleport(ServerPlayer who, ServerPlayer dest) {
        ServerLevel level = (ServerLevel) dest.level();
        who.teleportTo(level, dest.getX(), dest.getY(), dest.getZ(), Set.of(), who.getYRot(), who.getXRot(), true);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Request> it = requests.iterator();
        while (it.hasNext()) {
            Request r = it.next();
            if (r.expiresAt > now) continue;
            it.remove();
            ServerPlayer s = player(r.from);
            if (s != null) s.sendSystemMessage(Msg.err("Your teleport request expired."));
        }
    }
}
