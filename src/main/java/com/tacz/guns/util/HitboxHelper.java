package com.tacz.guns.util;

import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.config.common.OtherConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;
import java.util.WeakHashMap;

public final class HitboxHelper {
    // 玩家位置缓存表
    private static final WeakHashMap<Player, LinkedList<Vec3>> PLAYER_POSITION = new WeakHashMap<>();
    // 玩家命中箱缓存表
    private static final WeakHashMap<Player, LinkedList<AABB>> PLAYER_HITBOXES = new WeakHashMap<>();
    // 玩家速度缓存表
    private static final WeakHashMap<Player, LinkedList<Vec3>> PLAYER_VELOCITY = new WeakHashMap<>();
    // 命中箱缓存 Tick 上限
    private static final int SAVE_TICK = Mth.floor(OtherConfig.SERVER_HITBOX_LATENCY_MAX_SAVE_MS.get() / 1000 * 20 + 0.5);

    public static void onPlayerTick(Player player) {
        if (player.isSpectator()) {
            PLAYER_POSITION.remove(player);
            PLAYER_HITBOXES.remove(player);
            PLAYER_VELOCITY.remove(player);
            return;
        }
        LinkedList<Vec3> positions = PLAYER_POSITION.computeIfAbsent(player, p -> new LinkedList<>());
        LinkedList<AABB> boxes = PLAYER_HITBOXES.computeIfAbsent(player, p -> new LinkedList<>());
        LinkedList<Vec3> velocities = PLAYER_VELOCITY.computeIfAbsent(player, p -> new LinkedList<>());
        positions.addFirst(player.position());
        boxes.addFirst(player.getBoundingBox());
        velocities.addFirst(getPlayerVelocity(player));
        // Position 用于速度计算，所以只需要缓存 2 个位置
        if (positions.size() > 2) {
            positions.removeLast();
        }
        // 命中箱和速度缓存数量限制
        if (boxes.size() > SAVE_TICK) {
            boxes.removeLast();
            velocities.removeLast();
        }
    }

    public static void onPlayerLoggedOut(Player player) {
        PLAYER_POSITION.remove(player);
        PLAYER_HITBOXES.remove(player);
        PLAYER_VELOCITY.remove(player);
    }

    public static Vec3 getPlayerVelocity(Player entity) {
        LinkedList<Vec3> positions = PLAYER_POSITION.computeIfAbsent(entity, player -> new LinkedList<>());
        if (positions.size() > 1) {
            Vec3 currPos = positions.getFirst();
            Vec3 prevPos = positions.getLast();
            return new Vec3(currPos.x - prevPos.x, currPos.y - prevPos.y, currPos.z - prevPos.z);
        }
        return new Vec3(0, 0, 0);
    }

    public static AABB getBoundingBox(Player entity, int ping) {
        if (PLAYER_HITBOXES.containsKey(entity)) {
            LinkedList<AABB> boxes = PLAYER_HITBOXES.get(entity);
            int index = Mth.clamp(ping, 0, boxes.size() - 1);
            return boxes.get(index);
        }
        return entity.getBoundingBox();
    }

    public static Vec3 getVelocity(Player entity, int ping) {
        if (PLAYER_VELOCITY.containsKey(entity)) {
            LinkedList<Vec3> velocities = PLAYER_VELOCITY.get(entity);
            int index = Mth.clamp(ping, 0, velocities.size() - 1);
            return velocities.get(index);
        }
        return getPlayerVelocity(entity);
    }

    public static AABB getFixedBoundingBox(Entity entity, Entity owner) {
        AABB boundingBox = entity.getBoundingBox();
        Vec3 velocity = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        // hitbox 延迟补偿。只有射击者是玩家（且被击中者也是玩家）才进行此类延迟补偿计算
        if (OtherConfig.SERVER_HITBOX_LATENCY_FIX.get() && entity instanceof ServerPlayer player && owner instanceof ServerPlayer serverPlayerOwner) {
            int ping = Mth.floor((serverPlayerOwner.latency / 1000.0) * 20.0 + 0.5);
            boundingBox = getBoundingBox(player, ping);
            velocity = getVelocity(player, ping);
        }
        // 应用蹲伏导致的 hitbox 变形
        double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;
        boundingBox = boundingBox.expandTowards(0, expandHeight, 0);
        // 根据速度一定程度地扩展 hitbox
        boundingBox = boundingBox.expandTowards(velocity.x, velocity.y, velocity.z);
        // 玩家 hitbox 修正，可以通过 Config 调整
        double playerHitboxOffset = OtherConfig.SERVER_HITBOX_OFFSET.get();
        if (entity instanceof ServerPlayer) {
            if (entity.getVehicle() != null) {
                boundingBox = boundingBox.move(velocity.multiply(playerHitboxOffset / 2, playerHitboxOffset / 2, playerHitboxOffset / 2));
            }
            boundingBox = boundingBox.move(velocity.multiply(playerHitboxOffset, playerHitboxOffset, playerHitboxOffset));
        }
        // 给所有实体统一应用的 Hitbox 偏移，其数值为实验得出的定值。
        if (entity.getVehicle() != null || entity instanceof ITargetEntity) {
            boundingBox = boundingBox.move(velocity.multiply(-2.5, -2.5, -2.5));
        }
        boundingBox = boundingBox.move(velocity.multiply(-5, -5, -5));
        return boundingBox;
    }
}
