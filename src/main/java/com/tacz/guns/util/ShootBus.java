package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.UUID;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

/**
 * 增加了一条专门用于处理高频射击的线
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ShootBus {
    private static ShootBus instance;
    private final Object2IntOpenHashMap<UUID> counter = new Object2IntOpenHashMap<>();

    public ShootBus() {
        if (instance == null) {
            instance = this;
        }
        counter.defaultReturnValue(0); // 未找到时返回 0，而非 null
    }
    public static ShootBus getInstance() {
        return instance;
    }

    /**
     * 射击一发子弹（只记录）
     * @param playerUUID 设计者的UUID
     */
    public static void addShot(UUID playerUUID) {
        ShootBus.getInstance().counter.addTo(playerUUID, 1);
    }
    @SubscribeEvent
    /**
     * 每刻统一处理一次，把积攒的所有子弹作为一个弹射物发射出去
     */
    public static void processAndClear(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Iterator<Object2IntMap.Entry<UUID>> it = ShootBus.getInstance().counter.object2IntEntrySet().iterator();
        while (it.hasNext()) {
            Object2IntMap.Entry<UUID> entry = it.next();
            UUID playerUUID = entry.getKey();
            int bulletCount = entry.getIntValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;
            IGunOperator shooter = IGunOperator.fromLivingEntity(player);
            ShooterDataHolder data = shooter.getDataHolder();
            // 射击
            shooter.shoot(player::getXRot, player::getYRot, System.currentTimeMillis() - data.baseTimestamp, bulletCount, true);
            //从中清除
            it.remove();
        }
    }

}
