package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerBoltGun {
    public ClientMessagePlayerBoltGun() {
    }

    public static void encode(ClientMessagePlayerBoltGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerBoltGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerBoltGun();
    }

    public static void handle(ClientMessagePlayerBoltGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).bolt();
            });
        }
        context.setPacketHandled(true);
    }
}
