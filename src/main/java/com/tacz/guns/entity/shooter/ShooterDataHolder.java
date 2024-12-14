package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.LuaValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ShooterDataHolder {
    /**
     * 基时间戳，用于一些需要精密计算时间的场景。目前只有 shoot 使用。
     */
    public long baseTimestamp = System.currentTimeMillis();
    /**
     * 射击时间戳，射击成功时更新，单位 ms。
     * 用于计算射击的冷却时间。
     */
    public long shootTimestamp = -1L;
    public long lastShootTimestamp = -1L;
    /**
     * 近战时间戳，按下刺刀按键时更新，单位 ms
     * 用于计算射击的冷却时间
     */
    public long meleeTimestamp = -1L;
    /**
     * 近战有前摇，这个就是用于前摇的计数器
     * > 0 时：开始前摇计数，每 tick 减一
     * == 0 时：执行刺刀近战
     * < 0 时，默认情况，什么也不做
     */
    public int meleePrepTickCount = -1;
    /**
     * 切枪时间戳，在切枪开始时更新，单位 ms。
     * 用于计算切枪进度。切枪进度完成后，才能进行各种操作。
     */
    public long drawTimestamp = -1L;
    /**
     * 拉栓时间戳，在拉栓开始时更新，单位 ms。
     */
    public long boltTimestamp = -1;
    public boolean isBolting = false;
    /**
     * 瞄准的进度，范围 0 ~ 1
     */
    public float aimingProgress = 0;
    /**
     * 瞄准时间戳，在每个 tick 更新，单位 ms。
     * 用于在每个 tick 计算: 距离上一次更新 aimingProgress 的时长，并依此计算 aimingProgress 的增量。
     */
    public long aimingTimestamp = -1L;
    /**
     * 为 true 时表示正在 执行瞄准 状态，aimingProgress 会在每个 tick 叠加，
     * 为 false 时表示正在 取消瞄准 状态，aimingProgress 会在每个 tick 递减。
     */
    public boolean isAiming = false;
    /**
     * 装弹时间戳，在开始装弹的瞬间更新，单位 ms。
     * 用于在每个 tick 计算: 从开始装弹 到 当前时间点 的时长，并依此计算出换弹的状态和冷却。
     */
    public long reloadTimestamp = -1;
    /**
     * 装填状态的缓存。会在每个 tick 进行更新。
     */
    @Nonnull
    public ReloadState.StateType reloadStateType = ReloadState.StateType.NOT_RELOADING;
    /**
     * 当前操作的枪械物品的 Supplier。在切枪时 (draw 方法) 更新。
     */
    @Nullable
    public Supplier<ItemStack> currentGunItem = null;
    /**
     * 缓存当前枪械的收枪时间，以确保下一次切枪的时候使用此时间计算收枪。
     * 此数值不会因 tacz$CurrentGunItem 提供的 ItemStack 改变而改变，因此应当在恰当的时机调用 updatePutAwayTime() 进行更新。
     */
    public float currentPutAwayTimeS = 0;
    /**
     * 与疾跑相关的参数，开镜时会阻止疾跑
     */
    public float sprintTimeS = 0;
    public long sprintTimestamp = -1;
    /**
     * 用来记录子弹击退能力，负数表示使用原版击退
     */
    public double knockbackStrength = -1;
    /**
     * 记录射击数，用以判定曳光弹
     */
    public int shootCount = 0;
    /**
     * 是否处于趴下状态
     */
    public boolean isCrawling = false;
    /**
     * 用于缓存 lua 脚本的数据
     */
    @Nullable
    public LuaValue scriptData = null;
    /**
     * 配件修改过的各种属性缓存
     */
    @Nullable
    public AttachmentCacheProperty cacheProperty = null;

    public void initialData() {
        // 重置各个状态
        shootTimestamp = -1;
        meleeTimestamp = -1;
        meleePrepTickCount = -1;
        isAiming = false;
        aimingProgress = 0;
        reloadTimestamp = -1;
        reloadStateType = ReloadState.StateType.NOT_RELOADING;
        sprintTimestamp = -1;
        sprintTimeS = 0;
        boltTimestamp = -1;
        isBolting = false;
        shootCount = 0;
        scriptData = null;
    }
}
