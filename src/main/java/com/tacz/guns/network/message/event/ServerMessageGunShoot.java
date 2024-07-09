package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageGunShoot {
    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunShoot(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunShoot message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeItem(message.gunItemStack);
    }

    public static ServerMessageGunShoot decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = buf.readItem();
        return new ServerMessageGunShoot(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunShoot message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void doClientEvent(ServerMessageGunShoot message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            GunShootEvent gunShootEvent = new GunShootEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
            MinecraftForge.EVENT_BUS.post(gunShootEvent);
        }
    }
}
