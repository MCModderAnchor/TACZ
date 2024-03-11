package com.tac.guns.item;

import com.tac.guns.api.item.IGun;
import com.tac.guns.client.renderer.item.GunItemRenderer;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.builder.GunItemBuilder;
import com.tac.guns.item.nbt.GunItemData;
import com.tac.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class GunItem extends Item implements GunItemData {
    public GunItem() {
        super(new Properties().stacksTo(1).tab(ModItems.GUN_TAB));
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation gunId = this.getGunId(stack);
        Optional<ClientGunIndex> gunIndex = ClientGunPackLoader.getGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return new TranslatableComponent(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(modeTab)) {
            ClientGunPackLoader.getAllGuns().forEach(entry -> {
                GunData gunData = entry.getValue().getGunData();
                ItemStack itemStack = GunItemBuilder.create()
                        .setId(entry.getKey())
                        .setFireMode(gunData.getFireModeSet().get(0))
                        .setAmmoCount(gunData.getAmmoAmount())
                        .build();
                stacks.add(itemStack);
            });
        }
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
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> components, TooltipFlag flag) {
        if (stack.getItem() instanceof IGun iGun) {
            ClientGunPackLoader.getGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
                String tooltipKey = gunIndex.getTooltip();
                if (tooltipKey != null) {
                    components.add(new TranslatableComponent(tooltipKey));
                }
            });
        }
    }
}
