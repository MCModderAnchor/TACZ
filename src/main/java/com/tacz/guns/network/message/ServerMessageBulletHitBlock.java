package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.client.particle.HitBlockEffectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public class ServerMessageBulletHitBlock implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ServerMessageBulletHitBlock> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "server_message_bullet_hit_block"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ServerMessageBulletHitBlock> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buf, message) -> encode(message, buf), ServerMessageBulletHitBlock::decode);
    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

    private final double hitX;
    private final double hitY;
    private final double hitZ;
    private final Direction direction;
    private final BlockPos blockPos;
    private final String ammoId;
    private final String gunId;
    private final String gunDisplayId;
    private final boolean incendiary;

    public ServerMessageBulletHitBlock(Vec3 hitPos, Direction direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        this.hitX = hitPos.x;
        this.hitY = hitPos.y;
        this.hitZ = hitPos.z;
        this.direction = direction;
        this.blockPos = blockPos;
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.incendiary = incendiary;
    }

    public ServerMessageBulletHitBlock(double hitX, double hitY, double hitZ, Direction direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.direction = direction;
        this.blockPos = blockPos;
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.incendiary = incendiary;
    }

    public static void encode(ServerMessageBulletHitBlock message, RegistryFriendlyByteBuf buf) {
        buf.writeDouble(message.hitX);
        buf.writeDouble(message.hitY);
        buf.writeDouble(message.hitZ);
        buf.writeEnum(message.direction);
        buf.writeBlockPos(message.blockPos);
        buf.writeUtf(message.ammoId);
        buf.writeUtf(message.gunId);
        buf.writeUtf(message.gunDisplayId);
        buf.writeBoolean(message.incendiary);
    }

    public static ServerMessageBulletHitBlock decode(RegistryFriendlyByteBuf buf) {
        double hitX = buf.readDouble();
        double hitY = buf.readDouble();
        double hitZ = buf.readDouble();
        Direction direction = buf.readEnum(Direction.class);
        BlockPos blockPos = buf.readBlockPos();
        String ammoId = buf.readUtf();
        String gunId = buf.readUtf();
        String gunDisplayId = buf.readUtf();
        boolean incendiary = buf.readBoolean();
        return new ServerMessageBulletHitBlock(hitX, hitY, hitZ, direction, blockPos, ammoId, gunId, gunDisplayId, incendiary);
    }

    public static void handle(ServerMessageBulletHitBlock message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> handleClient(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ServerMessageBulletHitBlock message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        HitBlockEffectHandler.handle(level,
                new Vec3(message.hitX, message.hitY, message.hitZ),
                message.direction,
                message.blockPos,
                message.ammoId,
                message.gunId,
                message.gunDisplayId,
                message.incendiary);
    }
}
