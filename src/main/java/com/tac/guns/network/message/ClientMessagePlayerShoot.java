package com.tac.guns.network.message;

import com.mojang.logging.LogUtils;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.item.IGun;
import com.tac.guns.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerShoot {
    public static void encode(ClientMessagePlayerShoot message, FriendlyByteBuf buf) {
    }

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
                if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(entity, entity.getMainHandItem(), LogicalSide.SERVER))) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).shoot(entity.getMainHandItem(), entity.getXRot(), entity.getYRot());
            });
        }
        context.setPacketHandled(true);
    }
}
