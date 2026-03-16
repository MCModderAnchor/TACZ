package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.util.LuaEntityAccessor;
import com.tacz.guns.api.util.LuaNbtAccessor;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.network.message.ServerMessageGunStop;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.CycleTaskHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import org.antlr.v4.parse.ANTLRParser;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ModernKineticGunScriptAPI {
    public static String MARKER = "ScriptAPI";

    private LivingEntity shooter;

    private ShooterDataHolder dataHolder;

    private ItemStack itemStack;

    private AbstractGunItem abstractGunItem;

    private CommonGunIndex gunIndex;

    private ResourceLocation gunId;

    private ResourceLocation gunDisplayId;

    private Supplier<Float> pitchSupplier;

    private Supplier<Float> yawSupplier;

    private LuaNbtAccessor nbtUtil;

    private LuaEntityAccessor entityAccessor;

    /**
     * 获取玩家的属性缓存中缓存的值。
     * 请勿获取拥有复杂数据结构的属性值，该行为是未定义的。
     *
     * @param id 属性 id，请参阅 {@link GunProperties}
     * @see com.tacz.guns.api.CacheModifiableByScript
     * @return 属性的值
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    public LuaValue getCachedProperty(String id) {
        GunProperty<?> property = GunProperties.all().get(id);
        if (property == null) {
            throw new LuaError("unknown gun property: " + id);
        }
        AttachmentCacheProperty cache = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
        if (cache == null) {
            return LuaValue.NIL;
        }
        return CoerceJavaToLua.coerce(cache.getCache(id));
    }

    /**
     * 执行一次完整的射击逻辑，会考虑玩家的状态(是否在瞄准、是否在移动、是否在匍匐等)、配件数值影响、多弹丸散射、连发，播放开火音效、
     * @param consumeAmmo 本次射击是否消耗弹药
     */
    public void shootOnce(boolean consumeAmmo, int count) {
        GunMod.LOGGER.info("{}", count);
        GunData gunData = gunIndex.getGunData();
        BulletData bulletData = gunIndex.getBulletData();
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);

        // 获取配件数据缓存
        AttachmentCacheProperty cacheProperty = gunOperator.getCacheProperty();
        if (cacheProperty == null) {
            return;
        }

        //Handle Heat Data
        float heatInaccuracy = 1f;
        if(hasHeatData()) {
            GunHeatData heatData = Objects.requireNonNull(gunIndex.getGunData().getHeatData());
            float heatMax = modifyProperty(GunProperties.RuntimeOnly.MAX_HEAT, Float.class, heatData.getHeatMax());
            float heatPercentage = (getHeatAmount() / heatMax);
            heatInaccuracy *= Mth.lerp(heatPercentage, heatData.getMinInaccuracy(), heatData.getMaxInaccuracy());
        }

        // 散射影响
        InaccuracyType inaccuracyType = InaccuracyType.getInaccuracyType(shooter);
        final float unmodifiedInaccuracy = cacheProperty.getCache(GunProperties.INACCURACY).get(inaccuracyType) * heatInaccuracy;
        final float inaccuracy = Math.max(0, modifyProperty(GunProperties.INACCURACY, Float.class, unmodifiedInaccuracy));

        // 消音器影响
        // 使用消音这个选项对于射手来说是在客户端处理的，脚本改了没用，所以干脆不让改了
        Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
        final int soundDistance = modifyProperty(GunProperties.RuntimeOnly.SOUND_DISTANCE, Integer.class, silence.left());
        final boolean useSilenceSound = silence.right();

        // 子弹飞行速度
        float speed = modifyProperty(GunProperties.AMMO_SPEED, Float.class, cacheProperty.getCache(GunProperties.AMMO_SPEED));
        speed *= AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get();
        float processedSpeed = Mth.clamp(speed / 20, 0, Float.MAX_VALUE);
        // 弹丸数量
        int bulletAmount = modifyProperty(GunProperties.RuntimeOnly.BULLET_AMOUNT, Integer.class, Math.max(bulletData.getBulletAmount(), 1));

        // 连发数量
        FireMode fireMode = abstractGunItem.getFireMode(itemStack);
        int cycles = modifyProperty(GunProperties.RuntimeOnly.BURST_COUNT, Integer.class, fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1);
        // 连发间隔
        long period = modifyProperty(GunProperties.RuntimeOnly.BURST_SHOOT_INTERVAL, Long.class, fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1);

        CycleTaskHelper.addCycleTask(() -> {
            int bulletCount = count;
            // 如果射击者死亡，取消射击
            if (shooter.isDeadOrDying()) {
                return false;
            }
            // 如果武器变了，取消射击
            if (!shooter.getMainHandItem().equals(itemStack) || shooter.getMainHandItem().isEmpty()) {
                return false;
            }
            // 触发击发事件
            boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(shooter, itemStack, LogicalSide.SERVER));
            if (fire) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(shooter.getId(), itemStack), shooter);
                // 削减弹药
                if (consumeAmmo) {
                    int i = bulletCount;
                    while (i > 0) {
                        if (!this.reduceAmmoOnce()) {
                            bulletCount = 0;
                            break;
                        }
                        i-=1;
                    }
                    if(bulletCount <= 0) {
                        return false;
                    }
                }
                //Handle Heat Data
                if(gunIndex.getGunData().hasHeatData()) {
                    Optional.ofNullable(gunIndex.getScript())
                            .map(script -> checkFunction(script.get("handle_shoot_heat")))
                            .ifPresentOrElse(
                                    func -> func.call(CoerceJavaToLua.coerce(this)),
                                    this::handleShootHeat
                            );
                }
                // 获取射击方向（pitch 和 yaw）
                float pitch = pitchSupplier != null ? pitchSupplier.get() : shooter.getXRot();
                float yaw = yawSupplier != null ? yawSupplier.get() : shooter.getYRot();
                // 生成子弹
                Level world = shooter.level();
                ResourceLocation ammoId = gunData.getAmmoId();
                for (int i = 0; i < bulletAmount; i++) {
                    boolean isTracer = bulletData.hasTracerAmmo() && gunOperator.nextBulletIsTracer(bulletData.getTracerCountInterval());
                    EntityKineticBullet bullet = new EntityKineticBullet(world, shooter, itemStack, ammoId, gunId,
                            gunDisplayId, isTracer, gunData, bulletData, bulletCount);
                    bullet.applyShotgunDamageSpread(bulletAmount);
                    abstractGunItem.doBulletSpread(dataHolder, itemStack, shooter, bullet, i, processedSpeed,
                            inaccuracy, pitch, yaw);
                    world.addFreshEntity(bullet);
                }
                // 播放枪声
                if (soundDistance > 0) {
                    String soundId = useSilenceSound ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
                    SoundManager.sendSoundToNearby(shooter, soundDistance, gunId, gunDisplayId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
                }
            }
            return true;
        }, period, cycles);
    }

    private <T> T modifyProperty(GunProperty<?> property, Class<T> type, T value) {
        return abstractGunItem.modifyProperty(dataHolder, itemStack, shooter, property, type, value);
    }

    private <T> T modifyProperty(String id, Class<T> type, T value) {
        return abstractGunItem.modifyProperty(dataHolder, itemStack, shooter, id, type, value);
    }

    /**
     * 处理一次射击的过热变化
     */
    public void handleShootHeat() {
        GunHeatData heatData = gunIndex.getGunData().getHeatData();
        if (heatData == null) {
            return;
        }
        float newHeat = Math.min(abstractGunItem.getHeatAmount(itemStack) + heatData.getHeatPerShot(), heatData.getHeatMax());
        abstractGunItem.setHeatAmount(itemStack, newHeat);
        if (newHeat >= heatData.getHeatMax()) {
            abstractGunItem.setOverheatLocked(itemStack, true);
        }
    }
    /**
     * 让枪械内的子弹减少一发。会遵从栓动、闭膛待击和开膛待机的规律，消耗枪管内子弹或者弹匣内子弹。
     *
     * @return 是否成功减少子弹。
     */
    public boolean reduceAmmoOnce() {
        return  reduceAmmoOnce(false);
    }
    /**
     * 让枪械内的子弹减少一发。会遵从栓动、闭膛待击和开膛待机的规律，消耗枪管内子弹或者弹匣内子弹。
     *
     * @param simulate 如果为 {@code true}，则仅测试是否可以消耗子弹，不实际修改数量；
     *                 如果为 {@code false}，则真实消耗子弹。
     * @return 是否成功减少子弹（或是否可以减少）。
     */
    public boolean reduceAmmoOnce(boolean simulate) {
        Bolt boltType = TimelessAPI.getCommonGunIndex(abstractGunItem.getGunId(itemStack))
                .map(index -> index.getGunData().getBolt())
                .orElse(null);
        // 膛内是否有子弹
        boolean hasAmmoInBarrel = abstractGunItem.hasBulletInBarrel(itemStack) && boltType != Bolt.OPEN_BOLT;
        // 背包内是否还有子弹 (创造模式是否消耗背包备弹)
        boolean hasInventoryAmmo = abstractGunItem.hasInventoryAmmo(shooter, itemStack, isReloadingNeedConsumeAmmo());
        // 判断没有子弹的条件 (背包直读且包内没子弹 / 非背包直读且弹匣子弹数 < 1)
        boolean noAmmo = useInventoryAmmo() && !hasInventoryAmmo ||
                !useInventoryAmmo() && abstractGunItem.getCurrentAmmoCount(itemStack) < 1;
        if (boltType == null) {
            return false;
        }
        // 栓动逻辑
        if (boltType == Bolt.MANUAL_ACTION) {
            // 没有膛内子弹无法射击
            if (!hasAmmoInBarrel) {
                return false;
            }
            // 没有弹匣内的子弹则消耗枪膛内的子弹
            abstractGunItem.setBulletInBarrel(itemStack, false);
            return true;
        }
        // 闭膛逻辑
        if (boltType == Bolt.CLOSED_BOLT) {
            // 如果有弹匣内的子弹则优先消耗弹匣内的子弹
            if (!noAmmo) {
                // 如果背包直读则背包内射击后弹药 - 1
                if (useInventoryAmmo()) {
                    return consumeAmmoFromPlayer(1, simulate) == 1;
                }
                // 如果非背包直读则弹匣内子弹 - 1
                if(!simulate) {
                    abstractGunItem.reduceCurrentAmmoCount(itemStack);
                }
                return true;
            }
            // 没有膛内子弹无法射击
            if (!hasAmmoInBarrel) {
                return false;
            }
            // 没有弹匣内的子弹则消耗枪膛内的子弹
            if(!simulate) {
                abstractGunItem.setBulletInBarrel(itemStack, false);
            }
            return true;
        }
        // 开膛逻辑
        if (boltType == Bolt.OPEN_BOLT) {
            // 没有子弹无法射击
            if (noAmmo) {
                return false;
            }
            // 如果背包直读则背包内射击后弹药 - 1
            if (useInventoryAmmo()) {
                return consumeAmmoFromPlayer(1, simulate) == 1;
            }
            // 如果非背包直读则弹匣内子弹 - 1
            if(!simulate) {
                abstractGunItem.reduceCurrentAmmoCount(itemStack);
            }
            return true;
        }
        // 非三种已知 Bolt 类型 (目前不会出现)，默认返回 false
        return false;
    }


    /**
     * 获取从开始换弹到现在经历的时间，单位为 ms
     *
     * @return 开始换弹到现在经历的时间，单位为 ms
     */
    public long getReloadTime() {
        if (dataHolder.reloadTimestamp == -1) {
            return 0;
        }
        return System.currentTimeMillis() - dataHolder.reloadTimestamp;
    }

    /**
     * 获取从开始拉栓到现在经历的时间，单位为 ms
     *
     * @return 开始拉栓到现在经历的时间，单位为 ms
     */
    public long getBoltTime() {
        if (!dataHolder.isBolting) {
            return 0;
        }
        return System.currentTimeMillis() - dataHolder.boltTimestamp;
    }

    /**
     * 获取枪械的射击间隔，单位毫秒）
     *
     * @return 射击间隔
     */
    public long getShootInterval() {
        FireMode fireMode = abstractGunItem.getFireMode(itemStack);
        if (fireMode == FireMode.BURST) {
            long coolDown = (long) (gunIndex.getGunData().getBurstData().getMinInterval() * 1000f);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            return Math.max(coolDown, 0L);
        }
        long coolDown = gunIndex.getGunData().getShootInterval(this.shooter, fireMode, itemStack);
        // 给 5 ms 的窗口时间，以平衡延迟
        coolDown = coolDown - 5;
        return Math.max(coolDown, 0L);
    }

    /**
     * 返回上次射击的 timestamp(系统时间)，单位为毫秒。此值在切枪时会重置为 -1。
     *
     * @return 上次射击的 timestamp，在切枪时会重置为 -1。
     */
    public long getLastShootTimestamp() {
        return dataHolder.lastShootTimestamp + dataHolder.baseTimestamp;
    }

    /**
     * 调整射击间隔。
     * 射击间隔比较特殊，它在客户端和服务端上是分别计算的。因此你还需要在状态机脚本中重复进行一次这个操作。
     *
     * @param alpha 需要加上或减少的射击间隔，单位为毫秒。正数即增加射击间隔，负数则是减少。
     * @see GunAnimationStateContext#adjustClientShootInterval
     */
    public void adjustShootInterval(long alpha) {
        dataHolder.shootTimestamp += alpha;
    }

    /**
     * 调整换弹时间
     *
     * @param alpha 需要加上或减少的换弹时间，单位为毫秒。正数即增加换弹时间（加快换弹进度），负数则是减少（减慢换弹进度）。
     */
    public void adjustReloadTime(long alpha) {
        dataHolder.reloadTimestamp -= alpha;
    }

    /**
     * 调整拉栓时间
     *
     * @param alpha 需要加上或减少的拉栓时间，单位为毫秒。正数即增加拉栓时间（加快拉栓进度），负数则是减少（减慢拉栓进度）。
     */
    public void adjustBoltTime(long alpha) {
        dataHolder.boltTimestamp -= alpha;
    }

    /**
     * 获取瞄准进度。
     *
     * @return 范围 0~1。0 代表未瞄准，1 代表瞄准完成。
     */
    public float getAimingProgress() {
        return dataHolder.aimingProgress;
    }

    /**
     * 获取玩家当前的换弹状态。
     *
     * @return 玩家当前的换弹状态 (序数)
     */
    public int getReloadStateType() {
        return dataHolder.reloadStateType.ordinal();
    }

    /**
     * 获取枪械当前的开火模式（全自动、半自动、连发等）。
     *
     * @return 开火模式 (序数)
     */
    public int getFireMode() {
        return abstractGunItem.getFireMode(itemStack).ordinal();
    }

    /**
     * 获取当前玩家射击是否需要消耗弹药。经过设置，创造模式的玩家可以不消耗弹药射击。
     *
     * @return 射击是否需要消耗弹药
     */
    public boolean isShootingNeedConsumeAmmo() {
        return IGunOperator.fromLivingEntity(shooter).consumesAmmoOrNot();
    }

    /**
     * 获取当前玩家换弹是否需要消耗弹药。一般来说创造模式下不需要消耗弹药。
     *
     * @return 换弹是否需要消耗弹药
     */
    public boolean isReloadingNeedConsumeAmmo() {
        return IGunOperator.fromLivingEntity(shooter).needCheckAmmo();
    }

    /**
     * 获取当前枪械需要的弹药数量。
     *
     * @return 当前枪械需要的弹药数量
     */
    public int getNeededAmmoAmount() {
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(itemStack, gunIndex.getGunData());
        int currentAmmoCount = abstractGunItem.getCurrentAmmoCount(itemStack);
        return maxAmmoCount - currentAmmoCount;
    }

    /**
     * 获取弹匣中的备弹数。
     *
     * @return 返回弹匣中的备弹数，不计算已在枪管中的弹药。
     */
    public int getAmmoAmount() {
        return abstractGunItem.getCurrentAmmoCount(itemStack);
    }

    /**
     * 获取枪械弹匣的最大备弹数。
     *
     * @return 返回枪械弹匣的最大备弹数，不计算已在枪管中的弹药。
     */
    public int getMaxAmmoCount() {
        return AttachmentDataUtils.getAmmoCountWithAttachment(itemStack, gunIndex.getGunData());
    }

    /**
     * 获取枪械扩容等级。
     *
     * @return 扩容等级，范围 0 ~ 3。0 表示没有安装扩容弹匣，1 ~ 3 表示安装了扩容等级 1 ~ 3 的扩容弹匣
     */
    public int getMagExtentLevel() {
        return AttachmentDataUtils.getMagExtendLevel(itemStack, gunIndex.getGunData());
    }
    /**
     * 尽可能多地从玩家身上 (或者虚拟备弹) 消耗掉弹药，返回消耗的数量
     *
     * @param neededAmount 需要的弹药数量
     * @return 实际消耗的弹药数量
     */
    public int consumeAmmoFromPlayer(int neededAmount) {
        return consumeAmmoFromPlayer(neededAmount, false);
    }
    /**
     * 尽可能多地从玩家身上 (或者虚拟备弹) 消耗掉弹药，返回消耗的数量
     *
     * @param neededAmount 需要的弹药数量
     * @param simulate 如果为 {@code true}，则仅测试是否可以消耗子弹，不实际修改数量；
     *                 如果为 {@code false}，则真实消耗子弹。
     * @return 实际消耗的弹药数量
     */
    public int consumeAmmoFromPlayer(int neededAmount, boolean simulate) {
        // 如果处于背包直读并且创造模式不消耗的情况
        if (useInventoryAmmo() && !isReloadingNeedConsumeAmmo()) {
            return neededAmount;
        }
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            return abstractGunItem.findAndExtractDummyAmmo(itemStack, neededAmount);
        } else {
            return shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                    .map(cap -> abstractGunItem.findAndExtractInventoryAmmo(cap, itemStack, 1, true))
                    .orElse(0);
        }
    }

    /**
     * 检查玩家身上（或者虚拟备弹）是否有弹药可以消耗，通常用于循环换弹的打断。
     * 创造模式的玩家会直接返回 true
     * @return 玩家身上（或者虚拟备弹）是否有弹药可以消耗
     */
    public boolean hasAmmoToConsume(){
        if (!isReloadingNeedConsumeAmmo()) {
            return true;
        }
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            return abstractGunItem.getDummyAmmoAmount(itemStack) > 0;
        }
        return shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> {
            // 背包检查
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack checkAmmoStack = cap.getStackInSlot(i);
                if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(itemStack, checkAmmoStack)) {
                    return true;
                }
                if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(itemStack, checkAmmoStack)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    /**
     * 将子弹推入弹匣。
     *
     * @param amount 需要推入的子弹数量
     * @return 多余的子弹
     */
    public int putAmmoInMagazine(int amount) {
        if (amount < 0) {
            return 0;
        }
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(itemStack, gunIndex.getGunData());
        int currentAmmoCount = abstractGunItem.getCurrentAmmoCount(itemStack);
        int newAmmoCount = currentAmmoCount + amount;
        if (maxAmmoCount < newAmmoCount) {
            abstractGunItem.setCurrentAmmoCount(itemStack, maxAmmoCount);
            return newAmmoCount - maxAmmoCount;
        } else {
            abstractGunItem.setCurrentAmmoCount(itemStack, newAmmoCount);
            return 0;
        }
    }

    /**
     * 将子弹从弹匣移除。
     *
     * @param amount 需要移除的数量
     * @return 成功移除的数量
     */
    public int removeAmmoFromMagazine(int amount) {
        if (amount < 0) {
            return 0;
        }
        int currentAmmoCount = abstractGunItem.getCurrentAmmoCount(itemStack);
        if (currentAmmoCount < amount) {
            abstractGunItem.setCurrentAmmoCount(itemStack, 0);
            return currentAmmoCount;
        } else {
            abstractGunItem.setCurrentAmmoCount(itemStack, currentAmmoCount - amount);
            return amount;
        }
    }

    /**
     * 获取弹匣内子弹数量。
     *
     * @return 弹匣内子弹数量
     */
    public int getAmmoCountInMagazine() {
        return abstractGunItem.getCurrentAmmoCount(itemStack);
    }

    /**
     * 获取枪膛内是否有子弹。
     *
     * @return 枪膛内是否有子弹.如果是开膛待击的枪械，则此方法返回 false。
     */
    public boolean hasAmmoInBarrel() {
        Bolt boltType = gunIndex.getGunData().getBolt();
        return boltType != Bolt.OPEN_BOLT && abstractGunItem.hasBulletInBarrel(itemStack);
    }

    /**
     * 设置枪膛内是否有子弹
     */
    public void setAmmoInBarrel(boolean ammoInBarrel) {
        abstractGunItem.setBulletInBarrel(itemStack, ammoInBarrel);
    }

    /**
     * 将任意 lua 对象数据缓存到玩家数据中。用于脚本中异步传递数据，或者跨方法传递数据。
     *
     * @param luaValue 缓存的 lua 对象
     */
    public void cacheScriptData(LuaValue luaValue) {
        this.dataHolder.scriptData = luaValue;
    }

    /**
     * 将玩家数据中缓存的 lua 对象取出。
     *
     * @return 缓存的 lua 对象
     */
    public LuaValue getCachedScriptData() {
        return dataHolder.scriptData;
    }

    /**
     * 获取在枪械 data 中声明的脚本参数
     *
     * @return 脚本参数表
     */
    public LuaTable getScriptParams() {
        LuaTable param = gunIndex.getScriptParam();
        return param == null ? new LuaTable() : param;
    }

    /**
     * 委托延迟的循环任务，在主线程执行，是线程安全的，但是时间不是严格的，粒度取决于 TPS。
     *
     * @param value    应当是一个返回 boolean 的 LuaFunction。如果返回 false ，则将退出循环。
     * @param delayMs  延迟执行的时间。
     * @param periodMs 循环执行的间隔。
     * @param cycles   最大循环次数。-1 代表无限次。
     */
    public void safeAsyncTask(LuaValue value, long delayMs, long periodMs, int cycles) {
        LuaFunction func = value.checkfunction();
        CycleTaskHelper.addCycleTask(() -> func.call().checkboolean(), delayMs, periodMs, cycles);
    }

    /**
     * 获取当前系统时间，单位毫秒。
     *
     * @return 当前系统时间
     */
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取枪械的配件 ID
     *
     * @return 配件 ID, 如果类型错误或者对应的配件不存在则返回空配件 ID 'tacz:empty'
     */
    public String getAttachment(String type) {
        try {
            AttachmentType t = AttachmentType.valueOf(type);
            return abstractGunItem.getAttachmentId(itemStack, t).toString();
        } catch (IllegalArgumentException e) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID.toString();
        }
    }

    /**
     * 返回一个当前枪械物品的 NBT 访问器。除非要保存持久化数据，你不应该频繁调用这个方法。<br/>
     * 参见 {@link LuaNbtAccessor}
     * @return NBT 访问器
     */
    public LuaNbtAccessor getNbt() {
        return nbtUtil;
    }

    /**
     * 返回一个关于当前开枪实体的工具。这个访问器提供了一些常用的方法，例如发送系统消息、发送ActionBar、创建文本组件等。<br/>
     * 参见 {@link LuaEntityAccessor}
     * @return 实体访问器
     */
    public LuaEntityAccessor getEntityUtil() {
        if (entityAccessor == null) {
            entityAccessor = new LuaEntityAccessor(shooter);
        }
        return entityAccessor;
    }

    public void setShooter(LivingEntity shooter) {
        this.shooter = shooter;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        initGunItem();
    }

    public void setPitchSupplier(Supplier<Float> pitchSupplier) {
        this.pitchSupplier = pitchSupplier;
    }

    public void setYawSupplier(Supplier<Float> yawSupplier) {
        this.yawSupplier = yawSupplier;
    }

    public LivingEntity getShooter() {
        return shooter;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public AbstractGunItem getAbstractGunItem() {
        return abstractGunItem;
    }

    public CommonGunIndex getGunIndex() {
        return gunIndex;
    }

    public void setHeatAmount(float amount) {
        abstractGunItem.setHeatAmount(itemStack, amount);
    }

    public float getHeatAmount() {
        return abstractGunItem.getHeatAmount(itemStack);
    }

    public boolean hasHeatData() {
        return gunIndex.getGunData().getHeatData() != null;
    }

    public float getHeatMinRpm() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMinRpmMod();
        return 0f;
    }

    public float getHeatMaxRpm() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMaxRpmMod();
        return 0f;
    }

    public float getHeatMinInaccuracy() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMinInaccuracy();
        return 0f;
    }

    public float getHeatMaxInaccuracy() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getMaxInaccuracy();
        return 0f;
    }

    public float getHeatMax() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getHeatMax();
        return 0f;
    }

    public float getHeatPerShot() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getHeatPerShot();
        return 0f;
    }

    public boolean isOverheatLocked() {
        return abstractGunItem.isOverheatLocked(itemStack);
    }

    public void setOverheatLocked(boolean locked) {
        abstractGunItem.setOverheatLocked(itemStack, locked);
    }

    public long getOverheatTime() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getOverHeatTime();
        return 0;
    }

    public long getCoolingDelay() {
        if(hasHeatData()) return gunIndex.getGunData().getHeatData().getCoolingDelay();
        return 0;
    }

    public float calcHeatReduction(long heatTimestamp) {
        GunHeatData heatData = gunIndex.getGunData().getHeatData();
        if (heatData != null) {
            return ((float)(System.currentTimeMillis() - heatTimestamp) / 10000f)
                    * heatData.getCoolingMultiplier();
        }
        return 0f;
    }

    // TODO: 测试检查 enum 值是否可以直接在 lua 中调用，以简化这个功能为下面那个方法
    public int getBoltByInt() {
        Bolt bolt = gunIndex.getGunData().getBolt();
        if (bolt == Bolt.MANUAL_ACTION) {
            return 1;
        }
        if (bolt == Bolt.CLOSED_BOLT) {
            return 2;
        }
        if (bolt == Bolt.OPEN_BOLT) {
            return 3;
        }
        return 0;
    }

    public Bolt getBolt() {
        return gunIndex.getGunData().getBolt();
    }

    public void setDataHolder(ShooterDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public boolean useInventoryAmmo() {
        return abstractGunItem.useInventoryAmmo(itemStack);
    }

    ShooterDataHolder getDataHolder() {
        return this.dataHolder;
    }

    private void initGunItem() {
        if (itemStack == null || !(itemStack.getItem() instanceof AbstractGunItem gunItem)) {
            gunIndex = null;
            abstractGunItem = null;
            return;
        }
        gunId = gunItem.getGunId(itemStack);
        gunDisplayId = gunItem.getGunDisplayId(itemStack);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        gunIndex = gunIndexOptional.orElse(null);
        abstractGunItem = gunItem;
        if (itemStack.hasTag()) {
            nbtUtil = new LuaNbtAccessor(itemStack.getTag());
        }
    }


    private LuaFunction checkFunction(LuaValue luaValue) {
        if (luaValue.isfunction()) {
            return (LuaFunction) luaValue;
        } else if (luaValue.isnil()) {
            return null;
        } else {
            throw new LuaError("bad argument: function or nil expected, got " + luaValue.typename());
        }
    }
}