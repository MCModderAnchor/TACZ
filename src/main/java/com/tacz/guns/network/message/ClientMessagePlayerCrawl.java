package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;

import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMessagePlayerCrawl implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerCrawl> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_player_crawl")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessagePlayerCrawl> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, message -> message.isCrawl,
        ClientMessagePlayerCrawl::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean isCrawl;

    public ClientMessagePlayerCrawl(boolean isCrawl) {
        this.isCrawl = isCrawl;
    }

    public static void encode(ClientMessagePlayerCrawl message, RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(message.isCrawl);
    }

    public static ClientMessagePlayerCrawl decode(RegistryFriendlyByteBuf buf) {
        return new ClientMessagePlayerCrawl(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerCrawl message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = (net.minecraft.server.level.ServerPlayer) context.player();
                if (entity == null) {
                    return;
                }
                // 进入条件由服务端趴下逻辑统一验证；禁用功能后仍允许退出。
                IGunOperator.fromLivingEntity(entity).crawl(message.isCrawl);
            });
        }
    }
}
