package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerShootBegin {

    private long timestamp;

    public ClientMessagePlayerShootBegin() {
    }

    public ClientMessagePlayerShootBegin(long timestamp) {
        this.timestamp = timestamp;
    }

    public static void encode(ClientMessagePlayerShootBegin message, FriendlyByteBuf buf) {
        buf.writeLong(message.timestamp);
    }

    public static ClientMessagePlayerShootBegin decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerShootBegin(buf.readLong());
    }

    public static void handle(ClientMessagePlayerShootBegin message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).startFullAuto(message.timestamp);
            });
        }
        context.setPacketHandled(true);
    }
}
