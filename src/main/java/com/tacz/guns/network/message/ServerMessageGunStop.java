package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageGunStop {

    private final int shooterId;

    public ServerMessageGunStop(int shooterId) {
        this.shooterId = shooterId;
    }

    public static void encode(ServerMessageGunStop message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
    }

    public static ServerMessageGunStop decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        return new ServerMessageGunStop(shooterId);
    }

    public static void handle(ServerMessageGunStop message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message, context));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void doClientEvent(ServerMessageGunStop message, NetworkEvent.Context context) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            IGunOperator.fromLivingEntity(shooter).stopFullAuto();
        }
    }
}
