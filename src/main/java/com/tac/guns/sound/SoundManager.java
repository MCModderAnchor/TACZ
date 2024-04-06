package com.tac.guns.sound;

import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ServerMessageSound;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.PacketDistributor;

public class SoundManager {
    public static String SHOOT_SOUND = "shoot";
    public static String DRY_FIRE_SOUND = "dry_fire";
    public static String RELOAD_EMPTY_SOUND = "reload_empty";
    public static String RELOAD_TACTICAL_SOUND = "reload_tactical";
    public static String INSPECT_EMPTY_SOUND = "inspect_empty";
    public static String INSPECT_SOUND = "inspect";
    public static String DRAW_SOUND = "draw";

    public static void sendSoundToNearby(LivingEntity sourceEntity, int distance, ResourceLocation gunId, String soundName, float volume, float pitch) {
        if (sourceEntity.level instanceof ServerLevel serverLevel) {
            BlockPos pos = sourceEntity.blockPosition();
            ServerMessageSound soundMessage = new ServerMessageSound(sourceEntity.getId(), gunId, soundName, volume, pitch);
            serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < distance * distance)
                    .forEach(p -> NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), soundMessage));
        }
    }
}
