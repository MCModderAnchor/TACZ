package com.tacz.guns.api.item.gun;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.renderer.item.GunItemRenderer;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractGunItem extends Item implements IGun {
    protected AbstractGunItem(Properties pProperties) {
        super(pProperties);
    }

    private static Comparator<Map.Entry<ResourceLocation, CommonGunIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    /**
     * 开始拉栓时调用，返回 bolt 状态
     * @return bolt 状态。ture 代表开始 bolt，false 则代表不开始。
     */
    public abstract boolean startBolt(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 拉栓 tick 时调用，返回是否仍在 bolt 状态
     * @return 是否仍在 bolt 状态
     */
    public abstract boolean tickBolt(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 射击时触发
     */
    public abstract void shoot(ShooterDataHolder dataHolder, ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter);

    /**
     * 开始换弹时调用
     */
    public abstract boolean startReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 换弹时每个 tick 调用
     * @return 如果返回的类型是 NOT_RELOADING 则下一个 tick 不再继续调用
     */
    public abstract ReloadState tickReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 尝试打断换弹时调用
     */
    public abstract void interruptReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 切换开火模式时调用
     */
    public abstract void fireSelect(ShooterDataHolder dataHolder, ItemStack gunItem);

    /**
     * 近战时调用
     */
    public abstract void melee(ShooterDataHolder dataHolder, LivingEntity user, ItemStack gunItem);

    /**
     * 换弹前的检查，完成如下检查：枪内弹药是否已经填满？玩家背包是否有可用弹药？
     * @param shooter 准备换弹的实体
     * @param gunItem 枪械物品
     * @return 是否满足换弹条件
     */
    public boolean canReload(LivingEntity shooter, ItemStack gunItem) {
        ResourceLocation gunId = this.getGunId(gunItem);
        CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (gunIndex == null) {
            return false;
        }

        int currentAmmoCount = getCurrentAmmoCount(gunItem);
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, gunIndex.getGunData());
        if (currentAmmoCount >= maxAmmoCount) {
            return false;
        }
        if (useDummyAmmo(gunItem)) {
            return getDummyAmmoAmount(gunItem) > 0;
        }
        return shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> {
            // 背包检查
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack checkAmmoStack = cap.getStackInSlot(i);
                if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                    return true;
                }
                if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    /**
     * 将枪内的弹药全部退至背包（如果背包满了会丢到地上）。不会退枪膛内的弹药。
     * 目前，仅更换弹匣配件时调用。
     * @param player 玩家
     * @param gunItem 枪械物品
     */
    @Override
    public void dropAllAmmo(Player player, ItemStack gunItem) {
        //TODO 这里操作的对象不应该是 Player 而是 LivingEntity。此外枪膛内的子弹也要退
        int ammoCount = getCurrentAmmoCount(gunItem);
        if (ammoCount <= 0) {
            return;
        }
        ResourceLocation gunId = getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            // 如果使用的是虚拟备弹，返还至虚拟备弹
            if (useDummyAmmo(gunItem)) {
                setCurrentAmmoCount(gunItem, 0);
                // 燃料罐类型的换弹不返还
                if (index.getGunData().getReloadData().getType().equals(FeedType.FUEL)) {
                    return;
                }
                addDummyAmmoAmount(gunItem, ammoCount);
                return;
            }

            ResourceLocation ammoId = index.getGunData().getAmmoId();
            // 创造模式类型的换弹，只填满子弹总数，不进行任何卸载弹药逻辑
            if (player.isCreative()) {
                int maxAmmCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData());
                setCurrentAmmoCount(gunItem, maxAmmCount);
                return;
            }
            // 燃料罐类型的只清空不返还
            if (index.getGunData().getReloadData().getType().equals(FeedType.FUEL)) {
                setCurrentAmmoCount(gunItem, 0);
                return;
            }
            TimelessAPI.getCommonAmmoIndex(ammoId).ifPresent(ammoIndex -> {
                int stackSize = ammoIndex.getStackSize();
                int tmpAmmoCount = ammoCount;
                int roundCount = tmpAmmoCount / (stackSize + 1);
                for (int i = 0; i <= roundCount; i++) {
                    int count = Math.min(tmpAmmoCount, stackSize);
                    ItemStack ammoItem = AmmoItemBuilder.create().setId(ammoId).setCount(count).build();
                    ItemHandlerHelper.giveItemToPlayer(player, ammoItem);
                    tmpAmmoCount -= stackSize;
                }
                setCurrentAmmoCount(gunItem, 0);
            });
        });
    }

    /**
     * 枪械寻弹和扣除背包弹药逻辑
     * @param itemHandler 目标实体的背包
     * @param gunItem 枪械物品
     * @param needAmmoCount 需要的弹药(物品)数量
     * @return 寻找到的弹药(物品)数量
     */
    public int findAndExtractInventoryAmmos(IItemHandler itemHandler, ItemStack gunItem, int needAmmoCount) {
        int cnt = needAmmoCount;
        // 背包检查
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack checkAmmoStack = itemHandler.getStackInSlot(i);
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                ItemStack extractItem = itemHandler.extractItem(i, cnt, false);
                cnt = cnt - extractItem.getCount();
                if (cnt <= 0) {
                    break;
                }
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                int extractCount = Math.min(boxAmmoCount, cnt);
                int remainCount = boxAmmoCount - extractCount;
                iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                if (remainCount <= 0) {
                    iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                cnt = cnt - extractCount;
                if (cnt <= 0) {
                    break;
                }
            }
        }
        return needAmmoCount - cnt;
    }

    /**
     * 扣除虚拟弹药逻辑，该方法具有通用的实现，放在此处
     * @param gunItem 枪械物品
     * @param needAmmoCount 需要的弹药(物品)数量
     * @return 找到的弹药(物品)数量
     */
    public int findAndExtractDummyAmmo(ItemStack gunItem, int needAmmoCount) {
        int dummyAmmoCount = getDummyAmmoAmount(gunItem);
        int extractCount = Math.min(dummyAmmoCount, needAmmoCount);
        addDummyAmmoAmount(gunItem, -extractCount);
        return extractCount;
    }

    /**
     * 检查枪械是否允许安装指定的物品作为配件
     */
    @Override
    public boolean allowAttachment(ItemStack gun, ItemStack attachmentItem) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null && iAttachment != null) {
            ResourceLocation gunId = iGun.getGunId(gun);
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
            return AllowAttachmentTagMatcher.match(gunId, attachmentId);
        }
        return false;
    }

    /**
     * 检查枪械是否允许安装某种类型的配件
     */
    @Override
    public boolean allowAttachmentType(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                List<AttachmentType> allowAttachments = gunIndex.getGunData().getAllowAttachments();
                if (allowAttachments == null) {
                    return false;
                }
                return allowAttachments.contains(type);
            }).orElse(false);
        } else {
            return false;
        }
    }

    /**
     * 获取枪械的显示名称
     */
    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation gunId = this.getGunId(stack);
        Optional<ClientGunIndex> gunIndex = TimelessAPI.getClientGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return Component.translatable(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    /**
     * 获取某一类 TabType 的所有枪械物品的实例。用于填充创造物品栏和枪械制造台。
     */
    public static NonNullList<ItemStack> fillItemCategory(GunTabType type) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonGunIndex().stream().sorted(idNameSort()).forEach(entry -> {
            CommonGunIndex index = entry.getValue();
            GunData gunData = index.getGunData();
            String key = type.name().toLowerCase(Locale.US);
            String indexType = index.getType();
            if (key.equals(indexType)) {
                ItemStack itemStack = GunItemBuilder.create()
                        .setId(entry.getKey())
                        .setFireMode(gunData.getFireModeSet().get(0))
                        .setAmmoCount(gunData.getAmmoAmount())
                        .setAmmoInBarrel(true)
                        .build();
                stacks.add(itemStack);
            }
        });
        return stacks;
    }

    /**
     * 阻止玩家手臂挥动
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new GunItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }

    /**
     * 获取在 Tooltip 中渲染的图片
     */
    @Override
    @Nonnull
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            Optional<CommonGunIndex> optional = TimelessAPI.getCommonGunIndex(this.getGunId(stack));
            if (optional.isPresent()) {
                CommonGunIndex gunIndex = optional.get();
                ResourceLocation ammoId = gunIndex.getGunData().getAmmoId();
                return Optional.of(new GunTooltip(stack, iGun, ammoId, gunIndex));
            }
        }
        return Optional.empty();
    }
}
