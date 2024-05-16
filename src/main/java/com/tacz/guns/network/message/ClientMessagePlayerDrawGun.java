package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerDrawGun {
    public ClientMessagePlayerDrawGun() {
    }

    public static void encode(ClientMessagePlayerDrawGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerDrawGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerDrawGun();
    }

    public static void handle(ClientMessagePlayerDrawGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                Inventory inventory = entity.getInventory();
                int selected = inventory.selected;
                IGunOperator.fromLivingEntity(entity).draw(() -> inventory.getItem(selected));
            });
        }
        context.setPacketHandled(true);
    }
}
