package com.tacz.guns.api.item.gun;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.renderer.item.GunItemRenderer;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.tab.CustomTab;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.item.builder.GunItemBuilder;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AbstractGunItem extends Item implements IGun {
    protected AbstractGunItem(Properties pProperties) {
        super(pProperties);
    }

    /**
     * 拉栓完成时调用
     */
    public abstract void bolt(ItemStack gunItem);

    /**
     * 射击时触发
     */
    public abstract  void shoot(ItemStack gunItem, float pitch, float yaw, boolean tracer, LivingEntity shooter);

    /**
     * 切换开火模式时调用
     */
    public abstract void fireSelect(ItemStack gunItem);

    /**
     * 换弹时触发枪械子弹更新时调用
     * @param gunItem 枪械物品
     * @param ammoCount 填充的子弹数量
     * @param loadBarrel 是否需要往枪管里填子弹
     */
    public abstract void reloadAmmo(ItemStack gunItem, int ammoCount, boolean loadBarrel);

    public abstract String getTypeName();

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation gunId = this.getGunId(stack);
        Optional<ClientGunIndex> gunIndex = TimelessAPI.getClientGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return new TranslatableComponent(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (modeTab instanceof CustomTab tab) {
            String key = tab.getKey();
            TimelessAPI.getAllClientGunIndex().stream().sorted(idNameSort()).forEach(entry -> {
                ClientGunIndex index = entry.getValue();
                if (!index.getItemType().equals(getTypeName())) {
                    return;
                }
                if (key.equals(index.getType())) {
                    GunData gunData = index.getGunData();
                    ItemStack itemStack = GunItemBuilder.create()
                            .setId(entry.getKey())
                            .setFireMode(gunData.getFireModeSet().get(0))
                            .setAmmoCount(gunData.getAmmoAmount())
                            .setAmmoInBarrel(true)
                            .build();
                    stacks.add(itemStack);
                }
            });
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new GunItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }

    @Override
    public @Nonnull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
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

    private static Comparator<Map.Entry<ResourceLocation, ClientGunIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }
}
