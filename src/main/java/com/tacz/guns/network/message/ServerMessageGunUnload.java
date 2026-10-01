package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/** 仅在服务端确认本次主动卸弹实际减少了枪内弹药后发送给操作者。 */
public class ServerMessageGunUnload implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ServerMessageGunUnload> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "server_message_gun_unload"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ServerMessageGunUnload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buf, message) -> encode(message, buf), ServerMessageGunUnload::decode);
    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

    private final ResourceLocation gunId;
    private final ResourceLocation gunDisplayId;

    public ServerMessageGunUnload(ResourceLocation gunId, ResourceLocation gunDisplayId) {
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
    }

    public static void encode(ServerMessageGunUnload message, RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(message.gunId);
        buf.writeResourceLocation(message.gunDisplayId);
    }

    public static ServerMessageGunUnload decode(RegistryFriendlyByteBuf buf) {
        return new ServerMessageGunUnload(buf.readResourceLocation(), buf.readResourceLocation());
    }

    public static void handle(ServerMessageGunUnload message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> playSound(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void playSound(ServerMessageGunUnload message) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            SoundPlayManager.playUnloadSound(player, message.gunId, message.gunDisplayId);
        }
    }
}
