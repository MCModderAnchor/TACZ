package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.UUID;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

/**
 * 增加了一个专门用于处理全自动射击的类
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ShootBus {
    private static final Object2IntOpenHashMap<UUID> SHOT_COUNTER = new Object2IntOpenHashMap<>();
    static {
        SHOT_COUNTER.defaultReturnValue(0);
    }
    private ShootBus() {}

    /**
     * 射击一发子弹（只记录）
     * @param playerUUID 射击者的UUID
     */
    public static void addShot(UUID playerUUID) {
        ShootBus.SHOT_COUNTER.addTo(playerUUID, 1);
    }
    @SubscribeEvent
    /**
     * 每刻统一处理一次，把积攒的所有子弹作为一个弹射物发射出去
     */
    public static void processAndClear(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Iterator<Object2IntMap.Entry<UUID>> it = ShootBus.SHOT_COUNTER.object2IntEntrySet().iterator();
        while (it.hasNext()) {
            Object2IntMap.Entry<UUID> entry = it.next();
            UUID playerUUID = entry.getKey();
            int bulletCount = entry.getIntValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUUID);
            if (player == null) {
                it.remove();
                continue;
            }
            IGunOperator shooter = IGunOperator.fromLivingEntity(player);
            ShooterDataHolder data = shooter.getDataHolder();
            // 射击
            shooter.shoot(player::getXRot, player::getYRot, System.currentTimeMillis() - data.baseTimestamp, bulletCount, true);
            //从中清除
            it.remove();
        }
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SHOT_COUNTER.removeInt(serverPlayer.getUUID());
        }
    }
}
