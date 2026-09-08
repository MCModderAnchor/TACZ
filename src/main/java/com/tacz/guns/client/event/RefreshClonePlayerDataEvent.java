package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.sync.core.DataHolder;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import com.tacz.guns.util.DelayedTask;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.function.BooleanSupplier;

/**
 * 当玩家跨越维度时，客户端需要刷新一次玩家的配件属性缓存
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class RefreshClonePlayerDataEvent {
    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        LocalPlayer oldPlayer = event.getOldPlayer();
        LocalPlayer newPlayer = event.getNewPlayer();
        // 服务端只在实体重新加入世界时全量同步 SyncedEntityData。如果只有客户端重建了玩家实体（例如换皮肤模组/插件
        // 发送的 respawn 包），服务端不会重新同步，新实体会退回默认值（-1 的切枪/近战冷却），导致无法开火。
        // 因此把旧实体的数据复制过来；死亡重生、跨维度时服务端随后仍会全量同步覆盖。
        oldPlayer.reviveCaps();
        DataHolder oldHolder = SyncedEntityData.instance().getDataHolder(oldPlayer);
        DataHolder newHolder = SyncedEntityData.instance().getDataHolder(newPlayer);
        if (oldHolder != null && newHolder != null) {
            newHolder.dataMap = new HashMap<>(oldHolder.dataMap);
        }
        oldPlayer.invalidateCaps();
        // 基时间戳同理，只在服务端下发同步消息时更新，丢失会导致开火包的时间戳校验失败
        IClientPlayerGunOperator.fromLocalPlayer(newPlayer).getDataHolder().clientBaseTimestamp =
                IClientPlayerGunOperator.fromLocalPlayer(oldPlayer).getDataHolder().clientBaseTimestamp;
        // 但是这个事件触发时，玩家的背包并未同步，导致无法读取枪械数据进行配件属性缓存的刷新
        // 延迟 10 tick 执行缓存刷新就好了
        DelayedTask.add(() -> IGunOperator.fromLivingEntity(newPlayer).initialData(), 10);
    }

    /**
     * 延迟执行是通过这个方法执行的
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            try {
                DelayedTask.SUPPLIERS.removeIf(BooleanSupplier::getAsBoolean);
            } catch (Exception e) {
                DelayedTask.SUPPLIERS.clear();
                GunMod.LOGGER.catching(e);
            }
        }
    }
}
