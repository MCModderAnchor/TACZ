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
                PLAYER_HITBOXES.remove(event.player);
                PLAYER_VELOCITY.remove(event.player);
                return;
            }
            LinkedList<AABB> boxes = PLAYER_HITBOXES.computeIfAbsent(event.player, player -> new LinkedList<>());
            LinkedList<Vec3> velocities = PLAYER_VELOCITY.computeIfAbsent(event.player, player -> new LinkedList<>());
            boxes.addFirst(event.player.getBoundingBox());
            velocities.addFirst(getPlayerVelocity(event.player));
            // 命中箱缓存数量限制
            if (boxes.size() > SAVE_TICK || velocities.size() > SAVE_TICK) {
                boxes.removeLast();
                velocities.removeLast();
            }
        }
    }

    private static Vec3 getPlayerVelocity(Player player) {
        return new Vec3(player.getX() - player.xOld, player.getY() - player.yOld, player.getZ() - player.zOld);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
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
