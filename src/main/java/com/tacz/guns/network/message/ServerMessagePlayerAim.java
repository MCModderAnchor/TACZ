package com.tacz.guns.network.message;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessagePlayerAim {
    private final boolean isAim;

    public ServerMessagePlayerAim(boolean isAim) {
        this.isAim = isAim;
    }

    public static void encode(ServerMessagePlayerAim message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isAim);
    }

    public static ServerMessagePlayerAim decode(FriendlyByteBuf buf) {
        return new ServerMessagePlayerAim(buf.readBoolean());
    }

    public static void handle(ServerMessagePlayerAim message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(message.isAim);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
