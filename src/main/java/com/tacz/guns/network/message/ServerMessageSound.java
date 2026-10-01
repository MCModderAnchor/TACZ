package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ServerMessageSound implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSound> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_sound")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerMessageSound> STREAM_CODEC = NeoForgeStreamCodecs.composite(
        ByteBufCodecs.INT, ServerMessageSound::getEntityId,
        ResourceLocation.STREAM_CODEC, ServerMessageSound::getGunId,
        ResourceLocation.STREAM_CODEC, ServerMessageSound::getGunDisplayId,
        ByteBufCodecs.STRING_UTF8, ServerMessageSound::getSoundName,
        ByteBufCodecs.FLOAT, ServerMessageSound::getVolume,
        ByteBufCodecs.FLOAT, ServerMessageSound::getPitch,
        ByteBufCodecs.INT, ServerMessageSound::getDistance,
        ServerMessageSound::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final ResourceLocation gunId;
    private final ResourceLocation gunDisplayId;
    private final String soundName;
    private final float volume;
    private final float pitch;
    private final int distance;

    public ServerMessageSound(int entityId, ResourceLocation gunId, ResourceLocation gunDisplayId, String soundName, float volume, float pitch, int distance) {
        this.entityId = entityId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
        this.distance = distance;
    }

    public static void handle(ServerMessageSound message, IPayloadContext context) {
        context.enqueueWork(() -> SoundPlayManager.playMessageSound(message));
    }

    public int getEntityId() {
        return entityId;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    public ResourceLocation getGunDisplayId() {
        return gunDisplayId;
    }

    public String getSoundName() {
        return soundName;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public int getDistance() {
        return distance;
    }
}
