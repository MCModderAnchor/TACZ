package com.tacz.guns.compat.jei;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import net.minecraft.world.item.ItemStack;

public class GunModSubtype {
    public static IIngredientSubtypeInterpreter<ItemStack> getGunSubtype() {
        return (stack, context) -> {
            if (stack.getItem() instanceof IGun iGun) {
                return iGun.getGunId(stack).toString();
            }
            return IIngredientSubtypeInterpreter.NONE;
        };
    }

    public static IIngredientSubtypeInterpreter<ItemStack> getAmmoSubtype() {
        return (stack, context) -> {
            if (stack.getItem() instanceof IAmmo iAmmo) {
                return iAmmo.getAmmoId(stack).toString();
            }
            return IIngredientSubtypeInterpreter.NONE;
        };
    }
}
