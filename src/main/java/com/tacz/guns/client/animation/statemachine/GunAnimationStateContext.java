package com.tacz.guns.client.animation.statemachine;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.util.LuaNbtAccessor;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.luaj.vm2.LuaTable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("unused")
public class GunAnimationStateContext extends ItemAnimationStateContext {
    private ItemStack currentGunItem;
    private IGun iGun;
    private GunDisplayInstance display;
    private GunData gunData;
    private float partialTicks;
    private float walkDistAnchor = 0f;
    private LuaNbtAccessor nbtUtil;

    private <T> Optional<T> processGunData(BiFunction<IGun, GunDisplayInstance, T> processor) {
        if (iGun != null && display != null) {
            return Optional.ofNullable(processor.apply(iGun, display));
        }
        return Optional.empty();
    }

    private <T> Optional<T> processGunOperator(Function<IClientPlayerGunOperator, T> processor) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return Optional.ofNullable(processor.apply(IClientPlayerGunOperator.fromLocalPlayer(player)));
        }
        return Optional.empty();
    }

    private <T> Optional<T> processRemoteGunOperator(Function<IGunOperator, T> processor) {
        return processCameraEntity(entity -> {
            if (entity instanceof LivingEntity) {
                IGunOperator gunOperator = IGunOperator.fromLivingEntity((LivingEntity) entity);
                return processor.apply(gunOperator);
            }
            return null;
        });
    }

    private <T> Optional<T> processCameraEntity(Function<Entity, T> processor) {
        Entity entity = Minecraft.getInstance().cameraEntity;
        if (entity != null) {
            return Optional.ofNullable(processor.apply(entity));
        }
        return Optional.empty();
    }

    /**
     * 获取枪膛中是否有子弹。
     * @return 枪膛中是否有子弹。如果是开膛待击的枪械，则此方法返回 false。
     */
    public boolean hasBulletInBarrel() {
        return processGunData((iGun, gunIndex) -> {
            Bolt boltType = gunData.getBolt();
            return boltType != Bolt.OPEN_BOLT && iGun.hasBulletInBarrel(currentGunItem);
        }).orElse(false);
    }

    /**
     * 获取枪械的射击间隔，单位毫秒
     * @return 射击间隔
     */
    public long getShootInterval() {
        return processCameraEntity(entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                FireMode fireMode = iGun.getFireMode(currentGunItem);
                if (fireMode == FireMode.BURST) {
                    long coolDown = (long) (gunData.getBurstData().getMinInterval() * 1000f);
                    return Math.max(coolDown, 0L);
                }
                long coolDown = gunData.getShootInterval(livingEntity, fireMode);
                return Math.max(coolDown, 0L);
            }
            return 0L;
        }).orElse(0L);
    }

    /**
     * 返回上次射击的 timestamp(系统时间)，单位为毫秒。此值在切枪时会重置为 -1。
     * @return 上次射击的 timestamp，在切枪时会重置为 -1。
     */
    public long getLastShootTimestamp() {
        return processGunOperator(operator -> operator.getDataHolder().clientLastShootTimestamp).orElse(-1L);
    }

    /**
     * 获取当前系统时间，单位毫秒。
     * @return 当前系统时间
     */
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 调整射击间隔。(仅在客户端表现)
     * @param alpha 需要加上或减少的射击间隔，单位为毫秒。正数即增加射击间隔，负数则是减少。
     */
    public void adjustClientShootInterval(long alpha) {
        processGunOperator(operator -> {
            long timestamp = operator.getDataHolder().clientShootTimestamp;
            operator.getDataHolder().clientShootTimestamp = timestamp + alpha;
            return null;
        });
    }

    /**
     * 获取弹匣中的备弹数。
     * @return 返回弹匣中的备弹数，不计算已在枪管中的弹药。
     */
    public int getAmmoCount() {
        return processGunData((iGun, gunIndex) -> iGun.getCurrentAmmoCount(currentGunItem)).orElse(0);
    }

    /**
     * 获取枪械弹匣的最大备弹数。
     * @return 返回枪械弹匣的最大备弹数，不计算已在枪管中的弹药。
     */
    public int getMaxAmmoCount() {
        return processGunData(
                (iGun, gunIndex) ->
                        AttachmentDataUtils.getAmmoCountWithAttachment(currentGunItem, gunData)
        ).orElse(0);
    }

    /**
     * 检查玩家身上（或者虚拟备弹）是否有弹药可以消耗，通常用于循环换弹的打断。
     * 创造模式的玩家会直接返回 true
     * @return 玩家身上（或者虚拟备弹）是否有弹药可以消耗
     */
    public boolean hasAmmoToConsume(){
        if (!processRemoteGunOperator(IGunOperator::needCheckAmmo).orElse(true)) {
            return true;
        }
        if (iGun.useDummyAmmo(currentGunItem)) {
            return iGun.getDummyAmmoAmount(currentGunItem) > 0;
        }
        return processCameraEntity(entity ->
                    entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                        .map(cap -> {
                            // 背包检查
                            for (int i = 0; i < cap.getSlots(); i++) {
                                ItemStack checkAmmoStack = cap.getStackInSlot(i);
                                if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(currentGunItem, checkAmmoStack)) {
                                    return true;
                                }
                                if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(currentGunItem, checkAmmoStack)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .orElse(false)
                ).orElse(false);
    }

    /**
     * 获取枪械扩容等级。
     * @return 扩容等级，范围 0 ~ 3。0 表示没有安装扩容弹匣，1 ~ 3 表示安装了扩容等级 1 ~ 3 的扩容弹匣
     */
    public int getMagExtentLevel() {
        return processGunData(
                (iGun, gunIndex) ->
                        AttachmentDataUtils.getMagExtendLevel(currentGunItem, gunData)
        ).orElse(0);
    }

    /**
     * 获取枪械当前的开火模式。
     * @return FireMode 枚举的 ordinal 值
     */
    public int getFireMode() {
        return processGunData((iGun, gunIndex) -> iGun.getFireMode(currentGunItem).ordinal()).orElse(0);
    }

    /**
     * 获取持枪玩家的瞄准进度。
     * @return 持枪玩家的瞄准进度，取值范围：0 ~ 1。
     *         0 代表没有喵准，1 代表喵准完成。
     */
    public float getAimingProgress() {
        return processGunOperator(operator -> operator.getClientAimingProgress(partialTicks)).orElse(0f);
    }

    /**
     * 获取玩家当前是否在瞄准。如果正在瞄准，aiming progress 会增加，否则减少。
     * @return 玩家当前是否在瞄准
     */
    public boolean isAiming() {
        return processGunOperator(IClientPlayerGunOperator::isAim).orElse(false);
    }

    /**
     * 获取玩家的射击冷却。
     * @return 玩家的射击冷却，单位为毫秒(ms)。
     */
    public long getShootCoolDown() {
        return processGunOperator(IClientPlayerGunOperator::getClientShootCoolDown).orElse(0L);
    }

    /**
     * 获取玩家的换弹状态
     * @return 玩家的换弹状态
     */
    public int getReloadStateType() {
        return processCameraEntity(entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                return IGunOperator.fromLivingEntity(livingEntity).getSynReloadState().getStateType().ordinal();
            }
            return ReloadState.StateType.NOT_RELOADING.ordinal();
        }).orElse(ReloadState.StateType.NOT_RELOADING.ordinal());
    }

    /**
     * 获取玩家的按键输入是否为上。
     * @return 玩家的按键输入是否为上 (对应着移动中的前进按键，如 W)
     */
    public boolean isInputUp() {
        return Optional.ofNullable(Minecraft.getInstance().player).map(player -> player.input.up).orElse(false);
    }

    /**
     * 获取玩家的按键输入是否为下。
     * @return 玩家的按键输入是否为下 (对应着移动中的后退按键，如 S)
     */
    public boolean isInputDown() {
        return Optional.ofNullable(Minecraft.getInstance().player).map(player -> player.input.down).orElse(false);
    }

    /**
     * 获取玩家的按键输入是否为左。
     * @return 玩家的按键输入是否为左 (对应着移动中的左移按键，如 A)
     */
    public boolean isInputLeft() {
        return Optional.ofNullable(Minecraft.getInstance().player).map(player -> player.input.left).orElse(false);
    }

    /**
     * 获取玩家的按键输入是否为右。
     * @return 玩家的按键输入是否为右 (对应着移动中的右移按键，如 D)
     */
    public boolean isInputRight() {
        return Optional.ofNullable(Minecraft.getInstance().player).map(player -> player.input.right).orElse(false);
    }

    /**
     * 获取玩家的按键输入是否为跳跃。
     * @return 玩家的按键输入是否为跳跃 (对应着移动中的跳跃按键，如 Space)
     */
    public boolean isInputJumping() {
        return Optional.ofNullable(Minecraft.getInstance().player).map(player -> player.input.jumping).orElse(false);
    }

    /**
     * 获取玩家当前是否正在匍匐
     * @return 玩家当前是否正在匍匐
     */
    public boolean isCrawl() {
        return processGunOperator(IClientPlayerGunOperator::isCrawl).orElse(false);
    }

    /**
     * 获取玩家是否接触地面
     * @return 玩家是否接触地面
     */
    public boolean isOnGround() {
        return processCameraEntity(Entity::onGround).orElse(false);
    }

    /**
     * 获取 玩家是否蹲伏
     * @return 玩家是否蹲伏
     */
    public boolean isCrouching() {
        return processCameraEntity(Entity::isCrouching).orElse(false);
    }

    /**
     * 在玩家当前的行走距离打上锚点。此后，getWalkDist() 将返回与此锚点的相对值
     */
    public void anchorWalkDist() {
        processCameraEntity(entity -> {
            walkDistAnchor = entity.walkDist + (entity.walkDist - entity.walkDistO) * partialTicks;
            return null;
        });
    }

    /**
     * 获取与锚点相对的行走距离。如果没有打锚点，则直接获取行走距离。
     * @return 与锚点相对的行走距离。如果没有打锚点，则直接返回行走距离。
     */
    public float getWalkDist() {
        return processCameraEntity(entity -> {
            float currentWalkDist = entity.walkDist + (entity.walkDist - entity.walkDistO) * partialTicks;
            return currentWalkDist - walkDistAnchor;
        }).orElse(0f);
    }

    /**
     * 从指定序号的抛壳窗弹出一枚弹壳
     * @param index 抛壳窗序号
     */
    public void popShellFrom(int index) {
        BedrockGunModel gunModel = display.getGunModel();
        if (display.getShellEjection() != null && gunModel != null) {
            ShellRender shellRender = gunModel.getShellRender(index);
            if (shellRender != null) {
                shellRender.addShell(display.getShellEjection().getRandomVelocity());
            }
        }
    }

    /**
     * 获取在枪械 display 中声明的状态机参数
     *
     * @return 状态机参数表
     */
    public LuaTable getStateMachineParams() {
        LuaTable param = display.getStateMachineParam();
        return param == null ? new LuaTable() : param;
    }

    /**
     * 获取当前枪械物品的 NBT 数据访问器。<br/>
     * 注意，你不应该在客户端侧修改 NBT 数据，这可能会导致与服务端的数据不一致。<br/>
     * 你应该确保在状态机脚本内仅进行读操作
     * @return NBT 数据访问器
     */
    public LuaNbtAccessor getNbtAccessor() {
        return nbtUtil;
    }

    /**
     * 获取枪械的配件 ID
     *
     * @return 配件 ID, 如果类型错误或者对应的配件不存在则返回空配件 ID 'tacz:empty'
     */
    public String getAttachment(String type) {
        try {
            AttachmentType t = AttachmentType.valueOf(type);
            return iGun.getAttachmentId(currentGunItem, t).toString();
        } catch (IllegalArgumentException e) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID.toString();
        }
    }

    /**
     * 状态机脚本请不要调用此方法。此方法用于状态机更新时设置当前的物品对象。
     */
    public void setCurrentGunItem(ItemStack currentGunItem) {
        this.currentGunItem = currentGunItem;
        this.iGun = IGun.getIGunOrNull(currentGunItem);
        if (iGun != null) {
            display = TimelessAPI.getGunDisplay(currentGunItem).orElse(null);
            gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(currentGunItem))
                    .map(ClientGunIndex::getGunData).orElse(null);
        }
        if (currentGunItem.hasTag()) {
            nbtUtil = new LuaNbtAccessor(currentGunItem.getTag());
        }
    }

    /**
     * 获取最后一次更新时的 partialTicks
     * @return 状态机最后一次更新的 partialTicks.
     */
    public float getPartialTicks() {
        return partialTicks;
    }

    /**
     * 状态机脚本请不要调用此方法。此方法用于状态机更新时设置 partialTicks。
     */
    public void setPartialTicks(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}
