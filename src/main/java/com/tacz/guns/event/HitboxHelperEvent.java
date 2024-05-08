package com.tacz.guns.event;

import com.tacz.guns.config.common.OtherConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedList;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber
public class HitboxHelperEvent {
    // 玩家位置缓存表
    private static final WeakHashMap<Player, LinkedList<Vec3>> PLAYER_POSITION = new WeakHashMap<>();
    // 玩家命中箱缓存表
    private static final WeakHashMap<Player, LinkedList<AABB>> PLAYER_HITBOXES = new WeakHashMap<>();
    // 玩家速度缓存表
    private static final WeakHashMap<Player, LinkedList<Vec3>> PLAYER_VELOCITY = new WeakHashMap<>();
    // 命中箱缓存 Tick 上限
    private static final int SAVE_TICK = Mth.floor(OtherConfig.SERVER_HITBOX_LATENCY_MAX_SAVE_MS.get() / 1000 * 20 + 0.5);

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!OtherConfig.SERVER_HITBOX_LATENCY_FIX.get()) {
            return;
        }
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            if (event.player.isSpectator()) {
                PLAYER_POSITION.remove(event.player);
                PLAYER_HITBOXES.remove(event.player);
                PLAYER_VELOCITY.remove(event.player);
                return;
            }
            LinkedList<Vec3> positions = PLAYER_POSITION.computeIfAbsent(event.player, player -> new LinkedList<>());
            LinkedList<AABB> boxes = PLAYER_HITBOXES.computeIfAbsent(event.player, player -> new LinkedList<>());
            LinkedList<Vec3> velocities = PLAYER_VELOCITY.computeIfAbsent(event.player, player -> new LinkedList<>());
            positions.addFirst(event.player.position());
            boxes.addFirst(event.player.getBoundingBox());
            velocities.addFirst(getPlayerVelocity(event.player));
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

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYER_POSITION.remove(event.getEntity());
        PLAYER_HITBOXES.remove(event.getEntity());
        PLAYER_VELOCITY.remove(event.getEntity());
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
}
