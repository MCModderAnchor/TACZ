package com.tacz.guns.item;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.util.LuaEntityAccessor;
import com.tacz.guns.api.util.LuaNbtAccessor;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AimInaccuracyModifier;
import com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.CycleTaskHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Map;
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
     * 执行一次完整的射击逻辑，会考虑玩家的状态(是否在瞄准、是否在移动、是否在匍匐等)、配件数值影响、多弹丸散射、连发，播放开火音效、
     * @param consumeAmmo 本次射击是否消耗弹药
     */
    public void shootOnce(boolean consumeAmmo){
        GunData gunData = gunIndex.getGunData();
        BulletData bulletData = gunIndex.getBulletData();
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);

        // 获取配件数据缓存
        AttachmentCacheProperty cacheProperty = gunOperator.getCacheProperty();
        if (cacheProperty == null) {
            return;
        }

        // 散射影响
        InaccuracyType inaccuracyType = InaccuracyType.getInaccuracyType(shooter);
        float inaccuracy = Math.max(0, cacheProperty.<Map<InaccuracyType, Float>>getCache(InaccuracyModifier.ID).get(inaccuracyType));
        if (inaccuracyType == InaccuracyType.AIM) {
            inaccuracy = Math.max(0, cacheProperty.<Map<InaccuracyType, Float>>getCache(AimInaccuracyModifier.ID).get(inaccuracyType));
        }
        final float finalInaccuracy = inaccuracy;

        // 消音器影响
        Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
        final int soundDistance = silence.first();
        final boolean useSilenceSound = silence.right();

        // 子弹飞行速度
        float speed = cacheProperty.<Float>getCache(AmmoSpeedModifier.ID);
        float processedSpeed = Mth.clamp(speed / 20, 0, Float.MAX_VALUE);
        // 弹丸数量
        int bulletAmount = Math.max(bulletData.getBulletAmount(), 1);

        // 连发数量
        FireMode fireMode = abstractGunItem.getFireMode(itemStack);
        int cycles = fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1;
        // 连发间隔
        long period = fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1;

        CycleTaskHelper.addCycleTask(() -> {
            // 如果射击者死亡，取消射击
            if (shooter.isDeadOrDying()) {
                return false;
            }
            // 如果武器变了，取消射击
            if (!shooter.getMainHandItem().equals(itemStack)) {
                return false;
            }
            // 触发击发事件
            boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(shooter, itemStack, LogicalSide.SERVER));
            if (fire) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(shooter.getId(), itemStack), shooter);
                // 削减弹药
                if (consumeAmmo) {
                    if (!this.reduceAmmoOnce()) {
                        return false;
                    }
                }
                // 获取射击方向（pitch 和 yaw）
                float pitch = pitchSupplier != null ? pitchSupplier.get() : shooter.getXRot();
                float yaw = yawSupplier != null ? yawSupplier.get() : shooter.getYRot();
                // 生成子弹
                Level world = shooter.level();
                ResourceLocation ammoId = gunData.getAmmoId();
                for (int i = 0; i < bulletAmount; i++) {
                    boolean isTracer = bulletData.hasTracerAmmo() && gunOperator.nextBulletIsTracer(bulletData.getTracerCountInterval());
                    EntityKineticBullet bullet = new EntityKineticBullet(world, shooter, itemStack, ammoId, gunId, isTracer, gunData, bulletData);
                    bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, processedSpeed, finalInaccuracy);
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

    /**
     * 让枪械内的子弹减少一发。会遵从栓动、闭膛待击和开膛待机的规律，消耗枪管内子弹或者弹匣内子弹。
     * 如果没有可以消耗的子弹，这个方法会返回 false。例如栓动步枪，虽然弹匣内有子弹，但是在 bolt 之前枪管内没有子弹，那么就会返回 false，
     * @return 是否成功减少子弹。
     */
    public boolean reduceAmmoOnce() {
        Bolt boltType = TimelessAPI.getCommonGunIndex(abstractGunItem.getGunId(itemStack))
                .map(index -> index.getGunData().getBolt())
                .orElse(null);
        if (boltType == null) {
            return false;
        }
        if (boltType == Bolt.MANUAL_ACTION) {
            if (!abstractGunItem.hasBulletInBarrel(itemStack)) {
                return false;
            }
            abstractGunItem.setBulletInBarrel(itemStack, false);
        } else if (boltType == Bolt.CLOSED_BOLT) {
            if (abstractGunItem.getCurrentAmmoCount(itemStack) > 0) {
                abstractGunItem.reduceCurrentAmmoCount(itemStack);
            } else {
                if (!abstractGunItem.hasBulletInBarrel(itemStack)) {
                    return false;
                }
                abstractGunItem.setBulletInBarrel(itemStack, false);
            }
        } else {
            if (abstractGunItem.getCurrentAmmoCount(itemStack) == 0) {
                return false;
            }
            abstractGunItem.reduceCurrentAmmoCount(itemStack);
        }
        return true;
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
        long coolDown = gunIndex.getGunData().getShootInterval(this.shooter, fireMode);
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
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            return abstractGunItem.findAndExtractDummyAmmo(itemStack, neededAmount);
        } else {
            return shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                    .map(cap -> abstractGunItem.findAndExtractInventoryAmmos(cap, itemStack, neededAmount))
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

    public void setDataHolder(ShooterDataHolder dataHolder) {
        this.dataHolder = dataHolder;
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
}