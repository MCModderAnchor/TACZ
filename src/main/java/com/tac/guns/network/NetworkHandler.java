package com.tac.guns.network;

import com.tac.guns.GunMod;
import com.tac.guns.network.message.ClientMessagePlayerFireSelect;
import com.tac.guns.network.message.ClientMessagePlayerShoot;
import com.tac.guns.network.message.ServerMessageShoot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    private static final String VERSION = "1.0.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(GunMod.MOD_ID, "network"),
            () -> VERSION, it -> it.equals(VERSION), it -> it.equals(VERSION));

    public static void init() {
        CHANNEL.registerMessage(0, ClientMessagePlayerShoot.class, ClientMessagePlayerShoot::encode, ClientMessagePlayerShoot::decode, ClientMessagePlayerShoot::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, ServerMessageShoot.class, ServerMessageShoot::encode, ServerMessageShoot::decode, ServerMessageShoot::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, ClientMessagePlayerFireSelect.class, ClientMessagePlayerFireSelect::encode, ClientMessagePlayerFireSelect::decode, ClientMessagePlayerFireSelect::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToClientPlayer(Object message, Player player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), message);
    }

    public static void sendToNearby(Level world, BlockPos pos, Object toSend) {
        if (world instanceof ServerLevel) {
            ServerLevel ws = (ServerLevel) world;

            ws.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 192 * 192)
                    .forEach(p -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), toSend));
        }
    }

    public static void sendToNearby(Entity entity, Object toSend, int distance) {
        if (entity.level instanceof ServerLevel) {
            ServerLevel ws = (ServerLevel) entity.level;
            BlockPos pos = entity.blockPosition();

            ws.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < distance * distance)
                    .forEach(p -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), toSend));
        }
    }

    public static void sendToNearby(Entity entity, Object toSend) {
        if (entity.level instanceof ServerLevel) {
            ServerLevel ws = (ServerLevel) entity.level;
            BlockPos pos = entity.blockPosition();

            ws.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 192 * 192)
                    .forEach(p -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), toSend));
        }
    }
}
