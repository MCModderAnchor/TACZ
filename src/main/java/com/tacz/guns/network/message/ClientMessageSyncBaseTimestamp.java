package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.util.time.GunTime;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

public class ClientMessageSyncBaseTimestamp implements CustomPacketPayload {
    public static final ClientMessageSyncBaseTimestamp INSTANCE = new ClientMessageSyncBaseTimestamp();
    public static final CustomPacketPayload.Type<ClientMessageSyncBaseTimestamp> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_sync_base_timestamp")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessageSyncBaseTimestamp> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClientMessageSyncBaseTimestamp() { }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final Marker MARKER = MarkerManager.getMarker("SYNC_BASE_TIMESTAMP");


    public static void encode(ClientMessageSyncBaseTimestamp message, RegistryFriendlyByteBuf buf) { }

    public static ClientMessageSyncBaseTimestamp decode(RegistryFriendlyByteBuf buf) {
        return new ClientMessageSyncBaseTimestamp();
    }

    public static void handle(ClientMessageSyncBaseTimestamp message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isServerbound()) {
            long timestamp = GunTime.nowMillis();
            context.enqueueWork(() -> {
                ServerPlayer entity = (net.minecraft.server.level.ServerPlayer) context.player();
                if (entity == null) {
                    return;
                }
                ShooterDataHolder dataHolder = IGunOperator.fromLivingEntity(entity).getDataHolder();
                dataHolder.baseTimestamp = timestamp;
                GunMod.LOGGER.debug(MARKER, "Update server base timestamp: {}", dataHolder.baseTimestamp);
            });
        }
    }
}
