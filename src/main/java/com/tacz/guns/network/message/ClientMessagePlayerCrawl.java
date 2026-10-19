package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerCrawl {
    private final boolean isCrawl;

    public ClientMessagePlayerCrawl(boolean isCrawl) {
        this.isCrawl = isCrawl;
    }

    public static void encode(ClientMessagePlayerCrawl message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isCrawl);
    }

    public static ClientMessagePlayerCrawl decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerCrawl(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerCrawl message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                // 进入条件由服务端趴下逻辑统一验证；禁用功能后仍允许退出。
                IGunOperator.fromLivingEntity(entity).crawl(message.isCrawl);
            });
        }
        context.setPacketHandled(true);
    }
}
