package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageEnvironmentSound {
    private final BlockPos entityBlockPos;
    private final String registrySoundName;
    private final float volume;
    private final float pitch;
    private final int distance;

    public ServerMessageEnvironmentSound(BlockPos entityBlockPos,  String soundName, float volume, float pitch, int distance) {
        this.entityBlockPos = entityBlockPos;
        this.registrySoundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
        this.distance = distance;
    }

    public static void encode(ServerMessageEnvironmentSound message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.entityBlockPos);
        buf.writeUtf(message.registrySoundName);
        buf.writeFloat(message.volume);
        buf.writeFloat(message.pitch);
        buf.writeInt(message.distance);
    }

    public static ServerMessageEnvironmentSound decode(FriendlyByteBuf buf) {
        BlockPos entityBlockPos = buf.readBlockPos();
        String soundName = buf.readUtf();
        float volume = buf.readFloat();
        float pitch = buf.readFloat();
        int distance = buf.readInt();
        return new ServerMessageEnvironmentSound(entityBlockPos, soundName, volume, pitch, distance);
    }

    public static void handle(ServerMessageEnvironmentSound message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> SoundPlayManager.playMessageEnvironmentSound(message));
        }
        context.setPacketHandled(true);
    }

    public BlockPos getEntityBlockPos() {
        return entityBlockPos;
    }

    public String getRegistrySoundName() {
        return this.registrySoundName;
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
