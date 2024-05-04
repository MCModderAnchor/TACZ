package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerAim {
    private final boolean isAim;

    public ClientMessagePlayerAim(boolean isAim) {
        this.isAim = isAim;
    }

    public static void encode(ClientMessagePlayerAim message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isAim);
    }

    public static ClientMessagePlayerAim decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerAim(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerAim message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).aim(message.isAim);
            });
        }
        context.setPacketHandled(true);
    }
}
