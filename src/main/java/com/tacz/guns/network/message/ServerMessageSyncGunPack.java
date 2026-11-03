package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.network.GunPackSync;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class ServerMessageSyncGunPack {
    private static final GunPackSync.Receiver RECEIVER = new GunPackSync.Receiver();
    private final GunPackSync.Part part;

    public ServerMessageSyncGunPack(GunPackSync.Part part) {
        this.part = part;
    }

    /** 资源重载后仅调用一次，所有玩家复用同一组压缩分片。 */
    public static List<ServerMessageSyncGunPack> createMessages(Map<DataType, Map<ResourceLocation, String>> cache) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(256, GunPackSync.MAX_SYNC_SIZE));
        try {
            buf.writeMap(cache, FriendlyByteBuf::writeEnum, (buffer, map) ->
                    buffer.writeMap(map, FriendlyByteBuf::writeResourceLocation,
                            (buffer1, json) -> buffer1.writeUtf(json, GunPackSync.MAX_SYNC_SIZE)));
            byte[] uncompressed = new byte[buf.readableBytes()];
            buf.readBytes(uncompressed);
            List<GunPackSync.Part> parts = GunPackSync.compressAndSplit(uncompressed);
            GunMod.LOGGER.info("Prepared gun pack sync: {} bytes -> {} compressed bytes, {} part(s)",
                    uncompressed.length, parts.get(0).compressedSize(), parts.size());
            return parts.stream().map(ServerMessageSyncGunPack::new).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare gun pack sync", e);
        } finally {
            buf.release();
        }
    }

    public static void encode(ServerMessageSyncGunPack message, FriendlyByteBuf buf) {
        GunPackSync.Part part = message.part;
        buf.writeUUID(part.transferId());
        buf.writeVarInt(part.index());
        buf.writeVarInt(part.compressedSize());
        buf.writeVarInt(part.uncompressedSize());
        buf.writeByteArray(part.data());
    }

    public static ServerMessageSyncGunPack decode(FriendlyByteBuf buf) {
        return new ServerMessageSyncGunPack(new GunPackSync.Part(buf.readUUID(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readByteArray(GunPackSync.CHUNK_SIZE)));
    }

    public static void handle(ServerMessageSyncGunPack message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            Connection connection = context.getNetworkManager();
            context.enqueueWork(() -> doSync(message, connection));
        }
        context.setPacketHandled(true);
    }


    @OnlyIn(Dist.CLIENT)
    public static void clearPendingSync() {
        RECEIVER.clear();
    }

    @OnlyIn(Dist.CLIENT)
    private static void doSync(ServerMessageSyncGunPack message, Connection connection) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener == null || listener.getConnection() != connection || !connection.isConnected()) {
            return;
        }
        try {
            byte[] uncompressed = RECEIVER.accept(message.part);
            if (uncompressed == null) {
                return;
            }
            Map<DataType, Map<ResourceLocation, String>> cache = readCache(uncompressed);
            if (!connection.isMemoryConnection()) {
                CommonAssetsManager.clearInstance();
            }
            CommonNetworkCache.INSTANCE.fromNetwork(cache);
            // 等待完整快照收齐后再一次性重建索引。
            ClientIndexManager.reload();
        } catch (IOException | RuntimeException e) {
            RECEIVER.clear();
            GunMod.LOGGER.error("Failed to synchronize gun pack data", e);
            connection.disconnect(Component.literal("Failed to synchronize gun pack data"));
        }
    }

    private static Map<DataType, Map<ResourceLocation, String>> readCache(byte[] uncompressed) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(uncompressed));
        try {
            int types = buf.readVarInt();
            if (types < 0 || types > DataType.values().length) {
                throw new DecoderException("Invalid gun pack sync data type count: " + types);
            }
            Map<DataType, Map<ResourceLocation, String>> cache = new EnumMap<>(DataType.class);
            for (int i = 0; i < types; i++) {
                DataType type = buf.readEnum(DataType.class);
                int entries = buf.readVarInt();
                if (entries < 0 || entries > buf.readableBytes() / 2) {
                    throw new DecoderException("Invalid gun pack sync entry count: " + entries);
                }
                Map<ResourceLocation, String> data = new HashMap<>();
                for (int j = 0; j < entries; j++) {
                    ResourceLocation id = buf.readResourceLocation();
                    if (data.put(id, buf.readUtf(GunPackSync.MAX_SYNC_SIZE)) != null) {
                        throw new DecoderException("Duplicate gun pack sync resource: " + id);
                    }
                }
                if (cache.put(type, data) != null) {
                    throw new DecoderException("Duplicate gun pack sync data type: " + type);
                }
            }
            if (buf.isReadable()) {
                throw new DecoderException("Trailing gun pack sync data");
            }
            return cache;
        } finally {
            buf.release();
        }
    }
}
