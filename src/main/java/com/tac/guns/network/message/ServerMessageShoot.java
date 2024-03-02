package com.tac.guns.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageShoot {
    private int shooterId;

    public static void encode(ServerMessageShoot message, FriendlyByteBuf buf) {
        buf.writeInt(message.shooterId);
    }

    public static ServerMessageShoot decode(FriendlyByteBuf buf) {
        ServerMessageShoot message = new ServerMessageShoot();
        message.shooterId = buf.readInt();
        return message;
    }

    public static void handle(ServerMessageShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
    }
}
