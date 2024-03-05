package com.tac.guns.item;

import com.tac.guns.api.item.IAmmo;
import com.tac.guns.client.renderer.item.AmmoItemRenderer;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientAmmoIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.nbt.AmmoItemData;
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

public class AmmoItem extends Item implements IAmmo {
    public AmmoItem() {
        super(new Properties().stacksTo(1).tab(ModItems.AMMO_TAB));
    }

    public static @Nonnull AmmoItemData getData(@Nonnull ItemStack itemStack) {
        if (IAmmo.isAmmo(itemStack)) {
            return AmmoItemData.deserialization(itemStack.getOrCreateTag());
        }
        return new AmmoItemData();
    }

    public static ItemStack setData(@Nonnull ItemStack stack, @Nonnull AmmoItemData data) {
        if (IAmmo.isAmmo(stack)) {
            AmmoItemData.serialization(stack.getOrCreateTag(), data);
        }
        return stack;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return getData(stack).getAmmoStack();
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation ammoId = getData(stack).getAmmoId();
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
                AmmoItemData data = new AmmoItemData();
                data.setAmmoId(entry.getKey());
                data.setAmmoStack(entry.getValue().getStackSize());
                stacks.add(setData(this.getDefaultInstance(), data));
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
