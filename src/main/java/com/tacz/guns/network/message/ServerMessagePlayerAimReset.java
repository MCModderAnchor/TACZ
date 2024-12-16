package com.tacz.guns.network.message;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerMessagePlayerAimReset {
    public static final ServerMessagePlayerAimReset INSTANCE;

    static {
        // noinspection InstantiationOfUtilityClass
        INSTANCE = new ServerMessagePlayerAimReset();
    }

    private ServerMessagePlayerAimReset() {
    }

    public static void encode(ServerMessagePlayerAimReset message, FriendlyByteBuf buf) {
    }

    public static ServerMessagePlayerAimReset decode(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(ServerMessagePlayerAimReset message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aimReset(true);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
