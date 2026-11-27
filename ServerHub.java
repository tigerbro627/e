package dev.serverhub;

import dev.serverhub.commands.AuctionCommands;
import dev.serverhub.commands.EconomyCommands;
import dev.serverhub.commands.HelpCommands;
import dev.serverhub.commands.SidebarCommands;
import dev.serverhub.commands.SellCommands;
import dev.serverhub.commands.ShopCommands;
import dev.serverhub.commands.TpaCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHub implements ModInitializer {
    public static final String MOD_ID = "serverhub";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MinecraftServer server;
    private static int tick;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
            EconomyCommands.register(dispatcher);
            AuctionCommands.register(dispatcher);
            ShopCommands.register(dispatcher, buildContext);
            TpaCommands.register(dispatcher);
            SidebarCommands.register(dispatcher);
            SellCommands.register(dispatcher);
            HelpCommands.register(dispatcher);
        });

        MobRewards.register();

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            Config.load();
            Economy.load();
            Auction.load();
            AdminShop.load();
            SellPrices.load();
            LOGGER.info("ServerHub loaded.");
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> SidebarManager.leave(handler.getPlayer().getUUID()));

        ServerLifecycleEvents.SERVER_STOPPING.register(s -> saveAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            tick++;
            if (tick % 20 == 0) {
                TpaManager.tick();
                Auction.tick();
                SidebarManager.tick();
            }
            if (tick % 600 == 0) saveAll();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> {
            ServerPlayer p = handler.getPlayer();
            Economy.setName(p.getUUID(), p.getName().getString());
            Economy.get(p.getUUID());
            SidebarManager.join(p);
            int claims = Auction.claimCount(p.getUUID());
            if (claims > 0) {
                p.sendSystemMessage(Msg.info("You have " + claims + " item(s) waiting. Use /ah claim."));
            }
        });
    }

    public static void saveAll() {
        Economy.save();
        Auction.save();
        AdminShop.save();
    }
}
