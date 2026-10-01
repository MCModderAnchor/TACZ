package com.tacz.guns.compat.jei;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import com.tacz.guns.api.item.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GunModSubtype {
    public static ISubtypeInterpreter<ItemStack> getAmmoSubtype() {
        return new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext uidContext) {
                return getLegacyStringSubtypeInfo(stack, uidContext);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext uidContext) {
                if (stack.getItem() instanceof IAmmo iAmmo) {
                    return iAmmo.getAmmoId(stack).toString();
                }
                return "";
            }
        };
    }

    public static ISubtypeInterpreter<ItemStack> getGunSubtype() {
        return new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext uidContext) {
                return getLegacyStringSubtypeInfo(stack, uidContext);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext uidContext) {
                if (stack.getItem() instanceof IGun iGun) {
                    return iGun.getGunId(stack).toString();
                }
                return "";
            }
        };
    }

    public static ISubtypeInterpreter<ItemStack> getAttachmentSubtype() {
        return new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext uidContext) {
                return getLegacyStringSubtypeInfo(stack, uidContext);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext uidContext) {
                if (stack.getItem() instanceof IAttachment iAttachment) {
                    return iAttachment.getAttachmentId(stack).toString();
                }
                return "";
            }
        };
    }

    public static ISubtypeInterpreter<ItemStack> getTableSubType() {
        return new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext uidContext) {
                return getLegacyStringSubtypeInfo(stack, uidContext);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext uidContext) {
                if (stack.getItem() instanceof IBlock iBlock) {
                    return iBlock.getBlockId(stack).toString();
                }
                return "";
            }
        };
    }

    public static ISubtypeInterpreter<ItemStack> getAmmoBoxSubtype() {
        return new ISubtypeInterpreter<>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext uidContext) {
                return getLegacyStringSubtypeInfo(stack, uidContext);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext uidContext) {
                if (stack.getItem() instanceof IAmmoBox iAmmoBox) {
                    if (iAmmoBox.isAllTypeCreative(stack)) {
                        return "all_type_creative";
                    }
                    if (iAmmoBox.isCreative(stack)) {
                        return "creative";
                    }
                    return String.format("level_%d", iAmmoBox.getAmmoLevel(stack));
                }
                return "";
            }
        };
    }
}
