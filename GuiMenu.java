package dev.serverhub.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** A vanilla 6-row chest menu where every click is cancelled and forwarded to a handler. Works with unmodded clients. */
public final class GuiMenu extends ChestMenu {
    public interface Handler {
        void onClick(ServerPlayer player, int slot, ContainerInput type, int button);
    }

    private final Handler handler;

    private GuiMenu(int id, Inventory inv, Container container, Handler handler) {
        super(MenuType.GENERIC_9x6, id, inv, container, 6);
        this.handler = handler;
    }

    public static void open(ServerPlayer p, Component title, Container container, Handler handler) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GuiMenu(id, inv, container, handler), title));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput type, Player player) {
        if (player instanceof ServerPlayer sp && slotId >= 0 && slotId < 54
                && (type == ContainerInput.PICKUP || type == ContainerInput.QUICK_MOVE)) {
            handler.onClick(sp, slotId, type, button);
        }
        // Never let items move; resync the client's view.
        if (player.containerMenu == this) this.broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
