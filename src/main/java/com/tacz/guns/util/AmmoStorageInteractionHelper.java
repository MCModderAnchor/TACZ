package com.tacz.guns.util;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AmmoStorageInteractionHelper {
    private AmmoStorageInteractionHelper() {
    }

    public static InteractionResult unloadAmmoBox(AmmoStorageBlockEntity storage, ItemStack boxStack, IAmmoBox ammoBox, Player player) {
        if (ammoBox.isAllTypeCreative(boxStack) || ammoBox.isCreative(boxStack)) {
            return InteractionResult.CONSUME;
        }
        ResourceLocation ammoId = ammoBox.getAmmoId(boxStack);
        int count = ammoBox.getAmmoCount(boxStack);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID) || count <= 0) {
            return InteractionResult.CONSUME;
        }
        ItemStack ammoStack = AmmoStorageHelper.createAmmoStack(ammoId, count);
        int inserted = storage.insertAmmo(ammoStack, false);
        if (inserted <= 0) {
            return InteractionResult.CONSUME;
        }
        int remaining = count - inserted;
        ammoBox.setAmmoCount(boxStack, remaining);
        if (remaining <= 0) {
            ammoBox.setAmmoId(boxStack, DefaultAssets.EMPTY_AMMO_ID);
        }
        player.displayClientMessage(Component.translatable("message.tacz.ammo_storage.unloaded", ammoStack.getHoverName(), inserted), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
