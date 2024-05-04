package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerFireSelect {
    public static void encode(ClientMessagePlayerFireSelect message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerFireSelect decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerFireSelect();
    }

    public static void handle(ClientMessagePlayerFireSelect message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).fireSelect();
            });
        }
        context.setPacketHandled(true);
    }
}
