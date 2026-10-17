package com.tacz.guns.network.message;

import com.tacz.guns.inventory.AmmoStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A virtual entry interaction, not a vanilla inventory-slot click. */
public record ClientMessageAmmoStorageClick(int containerId, int entryId, AmmoStorageMenu.Action action) {
    public static void encode(ClientMessageAmmoStorageClick message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.containerId);
        buf.writeVarInt(message.entryId);
        buf.writeEnum(message.action);
    }

    public static ClientMessageAmmoStorageClick decode(FriendlyByteBuf buf) {
        return new ClientMessageAmmoStorageClick(buf.readVarInt(), buf.readVarInt(), buf.readEnum(AmmoStorageMenu.Action.class));
    }

    public static void handle(ClientMessageAmmoStorageClick message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof AmmoStorageMenu menu
                    && menu.containerId == message.containerId) {
                menu.handleInteraction(player, message.entryId, message.action);
            }
        });
        context.setPacketHandled(true);
    }
}
