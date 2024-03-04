package com.tac.guns.network.message;

import com.tac.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerAim {
    private final int gunItemIndex;
    private final boolean isAim;

    public ClientMessagePlayerAim(int gunItemIndex, boolean isAim) {
        this.gunItemIndex = gunItemIndex;
        this.isAim = isAim;
    }

    public static void encode(ClientMessagePlayerAim message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunItemIndex);
        buf.writeBoolean(message.isAim);
    }

    public static ClientMessagePlayerAim decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerAim(buf.readInt(), buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerAim message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                // 暂时只考虑主手
                if (entity.getInventory().selected != message.gunItemIndex) {
                    return;
                }
                ItemStack gunItem = entity.getInventory().getItem(message.gunItemIndex);
                IGunOperator.fromLivingEntity(entity).aim(gunItem, message.isAim);
            });
        }
        context.setPacketHandled(true);
    }
}
