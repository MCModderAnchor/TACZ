package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerAutoShoot {
    private final boolean shooting;

    public ClientMessagePlayerAutoShoot(boolean shooting) {
        this.shooting = shooting;
    }

    public static void encode(ClientMessagePlayerAutoShoot message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.shooting);
    }

    public static ClientMessagePlayerAutoShoot decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerAutoShoot(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerAutoShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                ShooterDataHolder dataHolder = IGunOperator.fromLivingEntity(entity).getDataHolder();
                if (message.shooting) {
                    ItemStack mainHandItem = entity.getMainHandItem();
                    if (!(mainHandItem.getItem() instanceof IGun iGun)) {
                        return;
                    }
                    FireMode fireMode = iGun.getFireMode(mainHandItem);
                    if (!LivingEntityShoot.isAutoShootMode(fireMode, iGun, mainHandItem)) {
                        return;
                    }
                    dataHolder.isAutoShooting = true;
                } else {
                    dataHolder.isAutoShooting = false;
                }
            });
        }
        context.setPacketHandled(true);
    }
}
