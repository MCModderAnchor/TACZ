package com.tacz.guns.network.message;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageSound {
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

    public ServerMessageSound(int entityId, ResourceLocation gunId, String soundName, float volume, float pitch, int distance) {
        this(entityId, gunId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID, soundName, volume, pitch, distance);
    }

    public static void encode(ServerMessageSound message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeResourceLocation(message.gunId);
        buf.writeResourceLocation(message.gunDisplayId);
        buf.writeUtf(message.soundName);
        buf.writeFloat(message.volume);
        buf.writeFloat(message.pitch);
        buf.writeInt(message.distance);
    }

    public static ServerMessageSound decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ResourceLocation gunId = buf.readResourceLocation();
        ResourceLocation gunDisplayId = buf.readResourceLocation();
        String soundName = buf.readUtf();
        float volume = buf.readFloat();
        float pitch = buf.readFloat();
        int distance = buf.readInt();
        return new ServerMessageSound(entityId, gunId, gunDisplayId, soundName, volume, pitch, distance);
    }

    public static void handle(ServerMessageSound message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> SoundPlayManager.playMessageSound(message));
        }
        context.setPacketHandled(true);
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
