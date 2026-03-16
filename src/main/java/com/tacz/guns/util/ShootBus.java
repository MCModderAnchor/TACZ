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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

/**
 * 增加了一个专门用于处理全自动射击的类
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ShootBus {
    private static class ShootingSession {
        final long startNano;          // 开始射击时的 System.nanoTime()
        final double bulletsPerNano;   // 每秒射速转换为每纳秒子弹数 (RPM / 60 / 1e9)
        int lastTotalBullets;         // 上一次发射时计算的总子弹数（整数部分）

        ShootingSession(int rpm) {
            this.startNano = System.nanoTime();
            this.bulletsPerNano = rpm / 60.0 / 1_000_000_000.0;
            this.lastTotalBullets = 0;
        }
    }
    private static final ConcurrentHashMap<UUID, ShootingSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    static {
    }
    private ShootBus() {}

    public static void beginShot(UUID playerUUID, int rpm) {
        ACTIVE_SESSIONS.put(playerUUID, new ShootingSession(rpm));
    }

    public static void endShot(UUID playerUUID) {
        ACTIVE_SESSIONS.remove(playerUUID);
    }
    @SubscribeEvent
    /**
     * 每刻统一处理一次，把积攒的所有子弹作为一个弹射物发射出去
     */
    public static void processAndClear(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        long nowNano = System.nanoTime();
        Iterator<Map.Entry<UUID, ShootingSession>> it = ShootBus.ACTIVE_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ShootingSession> entry = it.next();
            UUID playerUUID = entry.getKey();
            ShootingSession session= entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUUID);
            if (player == null) {
                it.remove();
                continue;
            }
            //计算射出多少发和最后一次射击的精确时间戳

            int bulletCount = (int) ((nowNano - session.startNano) * session.bulletsPerNano + 1) - session.lastTotalBullets; //向下取整
            session.lastTotalBullets += bulletCount;
            //GunMod.LOGGER.info("{} {} {} {}", System.nanoTime(), bulletCount, session.startNano, session.lastTotalBullets);
            if(bulletCount == 0)
                return;
            // 射击
            IGunOperator shooter = IGunOperator.fromLivingEntity(player);
            ShooterDataHolder data = shooter.getDataHolder();
            shooter.shoot(player::getXRot, player::getYRot, System.currentTimeMillis() - data.baseTimestamp, bulletCount, true);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ACTIVE_SESSIONS.remove(serverPlayer.getUUID());
        }
    }
}
