package com.tac.guns.network.message;

import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.init.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerShoot {
    public static void encode(ClientMessagePlayerShoot message, FriendlyByteBuf buf) {}

    public static ClientMessagePlayerShoot decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerShoot();
    }

    public static void handle(ClientMessagePlayerShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null || !entity.getMainHandItem().is(ModItems.GUN.get())) {
                    return;
                }
                if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(entity, entity.getMainHandItem(), LogicalSide.SERVER))) {
                    return;
                }
                Level world = entity.level;
                EntityBullet bullet = new EntityBullet(world, entity);
                bullet.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, 10, 0);
                world.addFreshEntity(bullet);
            });
        }
        context.setPacketHandled(true);
    }
}
