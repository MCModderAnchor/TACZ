package com.tac.guns.network.message;

import com.tac.guns.client.event.RenderCrosshairEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageHeadShot {
    public ServerMessageHeadShot() {
    }

    public static void encode(ServerMessageHeadShot message, FriendlyByteBuf buf) {
    }

    public static ServerMessageHeadShot decode(FriendlyByteBuf buf) {
        return new ServerMessageHeadShot();
    }

    public static void handle(ServerMessageHeadShot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> updateHeadShot());
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateHeadShot() {
        RenderCrosshairEvent.markHeadShotTimestamp();
    }
}
