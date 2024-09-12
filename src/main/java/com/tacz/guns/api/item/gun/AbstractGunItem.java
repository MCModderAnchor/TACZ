package com.tacz.guns.api.item.gun;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.renderer.item.GunItemRenderer;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
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
     * 拉栓完成时调用
     */
    public abstract void bolt(ItemStack gunItem);

    /**
     * 射击时触发
     */
    public abstract void shoot(ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, boolean tracer, LivingEntity shooter);

    /**
     * 切换开火模式时调用
     */
    public abstract void fireSelect(ItemStack gunItem);

    public abstract void melee(LivingEntity user, ItemStack gunItem);

    /**
     * 换弹前的检查，用于检查背包是否有弹药等
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
     * 执行换弹逻辑
     * @param shooter 进行换弹的实体
     * @param gunItem 枪械物品
     * @param loadBarrel 是否需要往枪管里填子弹
     */
    public void doReload(LivingEntity shooter, ItemStack gunItem, boolean loadBarrel) {
        ResourceLocation gunId = this.getGunId(gunItem);
        CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (gunIndex == null) {
            return;
        }

        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, gunIndex.getGunData());
        int currentAmmoCount = getCurrentAmmoCount(gunItem);
        int needAmmoCount = maxAmmoCount - currentAmmoCount;

        int updatedAmmoCount = currentAmmoCount;
        switch (gunIndex.getGunData().getReloadData().getType()) {
            case MAGAZINE -> {
                if (IGunOperator.fromLivingEntity(shooter).needCheckAmmo()) {
                    if (useDummyAmmo(gunItem)) {
                        updatedAmmoCount += findAndExtractDummyAmmo(gunItem, needAmmoCount);
                    } else {
                        updatedAmmoCount += shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                                .map(cap -> findAndExtractInventoryAmmos(cap, gunItem, needAmmoCount))
                                .orElse(0);
                    }
                } else {
                    updatedAmmoCount = maxAmmoCount;
                }
            }
            case FUEL -> {
                if (IGunOperator.fromLivingEntity(shooter).needCheckAmmo()) {
                    if (useDummyAmmo(gunItem)) {
                        if (findAndExtractDummyAmmo(gunItem, 1) > 0) {
                            updatedAmmoCount = maxAmmoCount;
                        }
                    } else {
                        if (shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                                .map(cap -> findAndExtractInventoryAmmos(cap, gunItem, 1))
                                .orElse(0) > 0) {
                            updatedAmmoCount = maxAmmoCount;
                        }
                    }
                } else {
                    updatedAmmoCount = maxAmmoCount;
                }
            }
            default -> {
                // 未实现
            }
        }

        finishReload(gunItem, updatedAmmoCount, loadBarrel);
    }

    @Override
    public void dropAllAmmo(Player player, ItemStack gunItem) {
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
     * @param itemHandler 目标实体的背包，该方法具有通用的实现，放在此处
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
     * 换弹完成时调用，用于更新枪械子弹数量，该方法具有通用的实现，放在此处
     * @param gunItem 枪械物品
     * @param ammoCount 填充的子弹数量
     * @param loadBarrel 是否需要往枪管里填子弹
     */

    public void finishReload(ItemStack gunItem, int ammoCount, boolean loadBarrel) {
        ResourceLocation gunId = getGunId(gunItem);
        Bolt boltType = TimelessAPI.getCommonGunIndex(gunId).map(index -> index.getGunData().getBolt()).orElse(null);
        this.setCurrentAmmoCount(gunItem, ammoCount);
        if (loadBarrel && (boltType == Bolt.MANUAL_ACTION || boltType == Bolt.CLOSED_BOLT)) {
            this.reduceCurrentAmmoCount(gunItem);
            this.setBulletInBarrel(gunItem, true);
        }
    }

    /**
     * 换弹时触发枪械子弹更新时调用
     *
     * @param gunItem    枪械物品
     * @param ammoCount  填充的子弹数量
     * @param loadBarrel 是否需要往枪管里填子弹
     */
    @Deprecated
    public void reloadAmmo(ItemStack gunItem, int ammoCount, boolean loadBarrel) {
        throw new UnsupportedOperationException("this method is deprecated, please use ‘doReload’ instead");
    }

    /**
     * 该方法具有通用的实现，放在此处
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
     * 该方法具有通用的实现，放在此处
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
     * 该方法具有通用的实现，放在此处
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
     * 该方法具有通用的实现，放在此处
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
     * 阻止玩家手臂挥动动画的播放
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    /**
     * 该方法具有通用的实现，放在此处
     */
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
     * 该方法具有通用的实现，放在此处
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
