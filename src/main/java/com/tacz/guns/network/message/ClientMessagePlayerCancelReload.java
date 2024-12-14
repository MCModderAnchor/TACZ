package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerCancelReload {
    public ClientMessagePlayerCancelReload() {
    }

    public static void encode(ClientMessagePlayerCancelReload message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerCancelReload decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerCancelReload();
    }

    public static void handle(ClientMessagePlayerCancelReload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).cancelReload();
            });
        }
        context.setPacketHandled(true);
    }
}
