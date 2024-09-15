package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ServerMessageGunKill {
    private final int bulletId;
    private final int killEntityId;
    private final int attackerId;
    private final ResourceLocation gunId;
    private final boolean isHeadShot;
    private final float baseDamage;
    private final float headshotMultiplier;

    public ServerMessageGunKill(int bulletId, int killEntityId, int attackerId, ResourceLocation gunId, float baseDamage, boolean isHeadShot, float headshotMultiplier) {
        this.bulletId = bulletId;
        this.killEntityId = killEntityId;
        this.attackerId = attackerId;
        this.gunId = gunId;
        this.baseDamage = baseDamage;
        this.isHeadShot = isHeadShot;
        this.headshotMultiplier = headshotMultiplier;
    }

    public static void encode(ServerMessageGunKill message, FriendlyByteBuf buf) {
        buf.writeInt(message.bulletId);
        buf.writeInt(message.killEntityId);
        buf.writeInt(message.attackerId);
        buf.writeResourceLocation(message.gunId);
        buf.writeFloat(message.baseDamage);
        buf.writeBoolean(message.isHeadShot);
        buf.writeFloat(message.headshotMultiplier);
    }

    public static ServerMessageGunKill decode(FriendlyByteBuf buf) {
        int bulletId = buf.readInt();
        int killEntityId = buf.readInt();
        int attackerId = buf.readInt();
        ResourceLocation gunId = buf.readResourceLocation();
        float baseDamage = buf.readFloat();
        boolean isHeadShot = buf.readBoolean();
        float headshotMultiplier = buf.readFloat();
        return new ServerMessageGunKill(bulletId, killEntityId, attackerId, gunId, baseDamage, isHeadShot, headshotMultiplier);
    }

    public static void handle(ServerMessageGunKill message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onKill(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onKill(ServerMessageGunKill message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        @Nullable Entity bullet = level.getEntity(message.bulletId);
        @Nullable LivingEntity killedEntity = level.getEntity(message.killEntityId) instanceof LivingEntity livingEntity ? livingEntity : null;
        @Nullable LivingEntity attacker = level.getEntity(message.attackerId) instanceof LivingEntity livingEntity ? livingEntity : null;
        MinecraftForge.EVENT_BUS.post(new EntityKillByGunEvent(bullet, killedEntity, attacker, message.gunId, message.baseDamage, null, message.isHeadShot, message.headshotMultiplier, LogicalSide.CLIENT));
    }
}
