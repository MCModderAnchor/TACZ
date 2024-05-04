package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerZoom {
    public static void encode(ClientMessagePlayerZoom message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerZoom decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerZoom();
    }

    public static void handle(ClientMessagePlayerZoom message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).zoom();
            });
        }
        context.setPacketHandled(true);
    }
}
