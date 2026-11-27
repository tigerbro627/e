package dev.serverhub;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Players earn money for killing mobs: 4 hearts (8 health) = $1, rounded to the nearest dollar. */
public final class MobRewards {
    private MobRewards() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!Config.get().mobRewardsEnabled) return;
            if (!(entity instanceof Mob)) return;
            if (!(source.getEntity() instanceof ServerPlayer killer)) return; // also covers arrows/tridents fired by a player

            long reward = Math.round(entity.getMaxHealth() / (double) Math.max(1, Config.get().healthPerDollar));
            if (reward <= 0) return;

            Economy.add(killer.getUUID(), reward);
            killer.sendSystemMessage(Msg.ok("+" + Economy.format(reward)), true); // action bar
        });
    }
}
