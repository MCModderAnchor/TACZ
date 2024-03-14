package com.tac.guns.network.message;

import com.tac.guns.inventory.GunRefitMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClientMessageRefit {
    public ClientMessageRefit() {
    }

    public static void encode(ClientMessageRefit message, FriendlyByteBuf buf) {
    }

    public static ClientMessageRefit decode(FriendlyByteBuf buf) {
        return new ClientMessageRefit();
    }

    public static void handle(ClientMessageRefit message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                entity.openMenu(new MenuProvider() {
                    @Override
                    @NotNull
                    public Component getDisplayName() {
                        return TextComponent.EMPTY;
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                        return new GunRefitMenu(id, inventory);
                    }
                });
            });
        }
        context.setPacketHandled(true);
    }
}
