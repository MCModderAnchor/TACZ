package com.tac.guns.network.message;

import com.tac.guns.api.event.common.LivingKillByGunEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ServerMessageGunKill {
    private final int killEntityId;
    private final int attackerId;
    private final ResourceLocation gunId;
    private final boolean isHeadShot;

    public ServerMessageGunKill(int killEntityId, int attackerId, ResourceLocation gunId, boolean isHeadShot) {
        this.killEntityId = killEntityId;
        this.attackerId = attackerId;
        this.gunId = gunId;
        this.isHeadShot = isHeadShot;
    }

    public static void encode(ServerMessageGunKill message, FriendlyByteBuf buf) {
        buf.writeInt(message.killEntityId);
        buf.writeInt(message.attackerId);
        buf.writeResourceLocation(message.gunId);
        buf.writeBoolean(message.isHeadShot);
    }

    public static ServerMessageGunKill decode(FriendlyByteBuf buf) {
        int killEntityId = buf.readInt();
        int attackerId = buf.readInt();
        ResourceLocation gunId = buf.readResourceLocation();
        boolean isHeadShot = buf.readBoolean();
        return new ServerMessageGunKill(killEntityId, attackerId, gunId, isHeadShot);
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
        @Nullable LivingEntity killedEntity = level.getEntity(message.killEntityId) instanceof LivingEntity livingEntity ? livingEntity : null;
        @Nullable LivingEntity attacker = level.getEntity(message.attackerId) instanceof LivingEntity livingEntity ? livingEntity : null;
        MinecraftForge.EVENT_BUS.post(new LivingKillByGunEvent(killedEntity, attacker, message.gunId, message.isHeadShot, LogicalSide.CLIENT));
    }
}
