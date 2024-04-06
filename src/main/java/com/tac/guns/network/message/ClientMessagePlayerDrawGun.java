package com.tac.guns.network.message;

import com.tac.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerDrawGun {
    private final int slotIndex;

    public ClientMessagePlayerDrawGun(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public static void encode(ClientMessagePlayerDrawGun message, FriendlyByteBuf buf) {
        buf.writeInt(message.slotIndex);
    }

    public static ClientMessagePlayerDrawGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerDrawGun(buf.readInt());
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
                IGunOperator.fromLivingEntity(entity).draw(() -> inventory.getItem(message.slotIndex));
            });
        }
        context.setPacketHandled(true);
    }
}
