package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.UUID;

import static com.tacz.guns.util.InputExtraCheck.isInGame;
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
    // 射出一发子弹 → 原子化 +1
    public static void addShot(UUID playerUUID) {
        ShootBus.getInstance().counter.addTo(playerUUID, 1);
    }
    @SubscribeEvent
    // 每刻调用：处理所有累积子弹，然后清空
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
            GunMod.LOGGER.info("ShootBus:56 {} {}", playerUUID.toString(), bulletCount);
            ShooterDataHolder data = shooter.getDataHolder();
            // 射击
            shooter.shoot(player::getXRot, player::getYRot, System.currentTimeMillis() - data.baseTimestamp, bulletCount, true);
            // 立即从映射中删除，保证下一轮遍历只有真正开火的枪
            it.remove();
        }
    }

}
