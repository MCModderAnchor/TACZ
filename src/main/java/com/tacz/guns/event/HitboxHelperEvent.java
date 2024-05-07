package com.tacz.guns.event;

import com.tacz.guns.config.common.OtherConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedList;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber
public class HitboxHelperEvent {
    private static final WeakHashMap<Player, LinkedList<AABB>> PLAYER_HITBOXES = new WeakHashMap<>();
    private static final int SAVE_TICK = Mth.floor(OtherConfig.SERVER_HITBOX_LATENCY_MAX_SAVE_MS.get() / 1000 * 20 + 0.5);

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!OtherConfig.SERVER_HITBOX_LATENCY_FIX.get()) {
            return;
        }
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            if (event.player.isSpectator()) {
                PLAYER_HITBOXES.remove(event.player);
                return;
            }
            LinkedList<AABB> boxes = PLAYER_HITBOXES.computeIfAbsent(event.player, player -> new LinkedList<>());
            boxes.addFirst(event.player.getBoundingBox());
            // 命中箱缓存
            if (boxes.size() > SAVE_TICK) {
                boxes.removeLast();
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYER_HITBOXES.remove(event.getEntity());
    }

    public static AABB getBoundingBox(Player entity, int ping) {
        if (PLAYER_HITBOXES.containsKey(entity)) {
            LinkedList<AABB> boxes = PLAYER_HITBOXES.get(entity);
            int index = Mth.clamp(ping, 0, boxes.size() - 1);
            return boxes.get(index);
        }
        return entity.getBoundingBox();
    }
}
