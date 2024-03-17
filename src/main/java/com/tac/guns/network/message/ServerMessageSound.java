package com.tac.guns.network.message;

import com.tac.guns.client.sound.SoundPlayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageSound {
    private final int entityId;
    private final ResourceLocation gunId;
    private final String soundName;
    private final float volume;
    private final float pitch;

    public ServerMessageSound(int entityId, ResourceLocation gunId, String soundName, float volume, float pitch) {
        this.entityId = entityId;
        this.gunId = gunId;
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
    }

    public static void encode(ServerMessageSound message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeResourceLocation(message.gunId);
        buf.writeUtf(message.soundName);
        buf.writeFloat(message.volume);
        buf.writeFloat(message.pitch);
    }

    public static ServerMessageSound decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ResourceLocation soundId = buf.readResourceLocation();
        String soundName = buf.readUtf();
        float volume = buf.readFloat();
        float pitch = buf.readFloat();
        return new ServerMessageSound(entityId, soundId, soundName, volume, pitch);
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

    public String getSoundName() {
        return soundName;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}
