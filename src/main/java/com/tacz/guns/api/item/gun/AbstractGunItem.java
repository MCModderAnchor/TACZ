package com.tacz.guns.api.item.gun;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.renderer.item.GunItemRenderer;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.tab.CustomTab;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.AttachmentPass;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AbstractGunItem extends Item implements IGun {
    protected AbstractGunItem(Properties pProperties) {
        super(pProperties);
    }

    private static Comparator<Map.Entry<ResourceLocation, ClientGunIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    /**
     * 拉栓完成时调用
     */
    public abstract void bolt(ItemStack gunItem);

    /**
     * 射击时触发
     */
    public abstract void shoot(ItemStack gunItem, float pitch, float yaw, boolean tracer, LivingEntity shooter);

    /**
     * 切换开火模式时调用
     */
    public abstract void fireSelect(ItemStack gunItem);

    /**
     * 换弹时触发枪械子弹更新时调用
     *
     * @param gunItem    枪械物品
     * @param ammoCount  填充的子弹数量
     * @param loadBarrel 是否需要往枪管里填子弹
     */
    public abstract void reloadAmmo(ItemStack gunItem, int ammoCount, boolean loadBarrel);

    /**
     * 能否添加到此 CustomTab 中
     *
     * @param tab   CustomTab
     * @param stack 待添加的物品
     */
    public abstract boolean canAddInTab(CustomTab tab, ItemStack stack);

    /**
     * 该方法具有通用的实现，放在此处
     */
    @Override
    public boolean allowAttachment(ItemStack gun, ItemStack attachmentItem) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null && iAttachment != null) {
            AttachmentType type = iAttachment.getType(attachmentItem);
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                Map<AttachmentType, AttachmentPass> map = gunIndex.getGunData().getAllowAttachments();
                if (map == null) {
                    return false;
                }
                AttachmentPass pass = map.get(type);
                if (pass == null) {
                    return false;
                }
                return pass.isAllow(attachmentId);
            }).orElse(false);
        } else {
            return false;
        }
    }

    /**
     * 该方法具有通用的实现，放在此处
     */
    @Override
    public boolean allowAttachmentType(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                Map<AttachmentType, AttachmentPass> map = gunIndex.getGunData().getAllowAttachments();
                if (map == null) {
                    return false;
                }
                return map.containsKey(type);
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
    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (modeTab instanceof CustomTab tab) {
            String key = tab.getKey();
            TimelessAPI.getAllClientGunIndex().stream().sorted(idNameSort()).forEach(entry -> {
                ClientGunIndex index = entry.getValue();
                if (key.equals(index.getType())) {
                    GunData gunData = index.getGunData();
                    ItemStack itemStack = GunItemBuilder.create()
                            .setId(entry.getKey())
                            .setFireMode(gunData.getFireModeSet().get(0))
                            .setAmmoCount(gunData.getAmmoAmount())
                            .setAmmoInBarrel(true)
                            .build();
                    if (canAddInTab(tab, itemStack)) {
                        stacks.add(itemStack);
                    }
                }
            });
        }
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
