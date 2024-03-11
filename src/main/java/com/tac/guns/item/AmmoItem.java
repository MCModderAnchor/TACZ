package com.tac.guns.item;

import com.tac.guns.api.item.IAmmo;
import com.tac.guns.client.renderer.item.AmmoItemRenderer;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientAmmoIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.builder.AmmoItemBuilder;
import com.tac.guns.item.nbt.AmmoItemData;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.index.CommonAmmoIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public class AmmoItem extends Item implements AmmoItemData {
    public AmmoItem() {
        super(new Properties().stacksTo(1).tab(ModItems.AMMO_TAB));
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        if (stack.getItem() instanceof IAmmo iAmmo) {
            return CommonGunPackLoader.getAmmoIndex(iAmmo.getAmmoId(stack))
                    .map(CommonAmmoIndex::getStackSize).orElse(1);
        }
        return 1;
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation ammoId = this.getAmmoId(stack);
        Optional<ClientAmmoIndex> ammoIndex = ClientGunPackLoader.getAmmoIndex(ammoId);
        if (ammoIndex.isPresent()) {
            return new TranslatableComponent(ammoIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(modeTab)) {
            ClientGunPackLoader.getAllAmmo().forEach(entry -> {
                ItemStack itemStack = AmmoItemBuilder.create().setId(entry.getKey()).build();
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
                return new AmmoItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }
}
