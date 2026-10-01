package com.tacz.guns.network.message.handshake;

import com.tacz.guns.GunMod;
import com.tacz.guns.network.IMessage;
import com.tacz.guns.network.LoginIndexHolder;
import com.tacz.guns.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

public class Acknowledge extends LoginIndexHolder implements IMessage {
    public static final Acknowledge INSTANCE = new Acknowledge();
    public static final CustomPacketPayload.Type<Acknowledge> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "acknowledge")
    );
    public static final StreamCodec<FriendlyByteBuf, Acknowledge> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private Acknowledge() { }

    @Override
    public @NotNull CustomPacketPayload.Type<Acknowledge> type() {
        return TYPE;
    }

    public static final Marker ACKNOWLEDGE = MarkerManager.getMarker("HANDSHAKE_ACKNOWLEDGE");

    @Override
    public void handle(IPayloadContext context) {
        GunMod.LOGGER.debug(ACKNOWLEDGE, "Received acknowledgement from client");
        context.finishCurrentTask(NetworkHandler.Task.TYPE);
    }
}
