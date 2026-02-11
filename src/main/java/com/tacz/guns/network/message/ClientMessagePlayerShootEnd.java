package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerShootEnd {
    /**
     * 这里的 timestamp 应该是基于 base timestamp 的相对值
     */
    private long timestamp;

    public ClientMessagePlayerShootEnd() {
    }

    public ClientMessagePlayerShootEnd(long timestamp) {
        this.timestamp = timestamp;
    }

    public static void encode(ClientMessagePlayerShootEnd message, FriendlyByteBuf buf) {
        buf.writeLong(message.timestamp);
    }

    public static ClientMessagePlayerShootEnd decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerShootEnd(buf.readLong());
    }

    public static void handle(ClientMessagePlayerShootEnd message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).stopFullAuto(message.timestamp);
            });
        }
        context.setPacketHandled(true);
    }
}
