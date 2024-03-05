package com.tac.guns.network.message;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunShootEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerShoot {
    public ClientMessagePlayerShoot(){}

    public static void encode(ClientMessagePlayerShoot message, FriendlyByteBuf buf) {}

    public static ClientMessagePlayerShoot decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerShoot();
    }

    public static void handle(ClientMessagePlayerShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if(entity == null){
                    return;
                }
                IGunOperator.fromLivingEntity(entity).shoot(entity.getXRot(), entity.getYRot());
            });
        }
        context.setPacketHandled(true);
    }
}
