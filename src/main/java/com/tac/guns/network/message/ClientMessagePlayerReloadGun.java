package com.tac.guns.network.message;

import com.tac.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerReloadGun {
    private final int gunItemIndex;

    public ClientMessagePlayerReloadGun(int gunItemIndex){
        this.gunItemIndex = gunItemIndex;
    }

    public static void encode(ClientMessagePlayerReloadGun message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunItemIndex);
    }

    public static ClientMessagePlayerReloadGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerReloadGun(buf.readInt());
    }

    public static void handle(ClientMessagePlayerReloadGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if(entity == null){
                    return;
                }
                // 暂时只考虑主手能换弹。
                if(entity.getInventory().selected != message.gunItemIndex) {
                    return;
                }
                ItemStack gunItem = entity.getInventory().getItem(message.gunItemIndex);
                IGunOperator.fromLivingEntity(entity).reload(gunItem);
            });
        }
        context.setPacketHandled(true);
    }
}
