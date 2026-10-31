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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerMessageSyncGunPack implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSyncGunPack> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_sync_gun_pack")
    );
    public static final StreamCodec<FriendlyByteBuf, ServerMessageSyncGunPack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ServerMessageSyncGunPack decode(FriendlyByteBuf buffer) {
            return new ServerMessageSyncGunPack(new GunPackSync.Part(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(GunPackSync.CHUNK_SIZE)));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, ServerMessageSyncGunPack message) {
            GunPackSync.Part part = message.part;
            buffer.writeUUID(part.transferId());
            buffer.writeVarInt(part.index());
            buffer.writeVarInt(part.compressedSize());
            buffer.writeVarInt(part.uncompressedSize());
            buffer.writeByteArray(part.data());
        }
    };
    private static final GunPackSync.Receiver RECEIVER = new GunPackSync.Receiver();

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final GunPackSync.Part part;

    public ServerMessageSyncGunPack(GunPackSync.Part part) {
        this.part = part;
    }

    /** 资源重载后仅调用一次，所有玩家复用同一组压缩分片。 */
    public static List<ServerMessageSyncGunPack> createMessages(Map<DataType, Map<ResourceLocation, String>> cache) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(256, GunPackSync.MAX_SYNC_SIZE));
        try {
            buffer.writeMap(cache, FriendlyByteBuf::writeEnum, (dataBuffer, map) ->
                    dataBuffer.writeMap(map, FriendlyByteBuf::writeResourceLocation,
                            (jsonBuffer, json) -> jsonBuffer.writeUtf(json, GunPackSync.MAX_SYNC_SIZE)));
            byte[] uncompressed = new byte[buffer.readableBytes()];
            buffer.readBytes(uncompressed);
            List<GunPackSync.Part> parts = GunPackSync.compressAndSplit(uncompressed);
            GunMod.LOGGER.info("Prepared gun pack sync: {} bytes -> {} compressed bytes, {} part(s)",
                    uncompressed.length, parts.getFirst().compressedSize(), parts.size());
            return parts.stream().map(ServerMessageSyncGunPack::new).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare gun pack sync", e);
        } finally {
            buffer.release();
        }
    }

    public static void handle(ServerMessageSyncGunPack message, IPayloadContext context) {
        Connection connection = context.connection();
        context.enqueueWork(() -> doSync(message, connection));
    }


    public GunPackSync.Part getPart() {
        return part;
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

    static Map<DataType, Map<ResourceLocation, String>> readCache(byte[] uncompressed) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(uncompressed));
        try {
            int types = buffer.readVarInt();
            if (types < 0 || types > DataType.values().length) {
                throw new DecoderException("Invalid gun pack sync data type count: " + types);
            }
            Map<DataType, Map<ResourceLocation, String>> cache = new EnumMap<>(DataType.class);
            for (int i = 0; i < types; i++) {
                DataType type = buffer.readEnum(DataType.class);
                int entries = buffer.readVarInt();
                if (entries < 0 || entries > buffer.readableBytes() / 2) {
                    throw new DecoderException("Invalid gun pack sync entry count: " + entries);
                }
                Map<ResourceLocation, String> data = new HashMap<>();
                for (int j = 0; j < entries; j++) {
                    ResourceLocation id = buffer.readResourceLocation();
                    if (data.put(id, buffer.readUtf(GunPackSync.MAX_SYNC_SIZE)) != null) {
                        throw new DecoderException("Duplicate gun pack sync resource: " + id);
                    }
                }
                if (cache.put(type, data) != null) {
                    throw new DecoderException("Duplicate gun pack sync data type: " + type);
                }
            }
            if (buffer.isReadable()) {
                throw new DecoderException("Trailing gun pack sync data");
            }
            return cache;
        } finally {
            buffer.release();
        }
    }
}
