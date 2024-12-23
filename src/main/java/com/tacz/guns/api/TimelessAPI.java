package com.tacz.guns.api;

import com.tacz.guns.api.client.other.IThirdPersonAnimation;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TimelessAPI {
    @OnlyIn(Dist.CLIENT)
    public static Optional<GunDisplayInstance> getGunDisplay(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            ResourceLocation gunId = iGun.getGunId(stack);
            if (getCommonGunIndex(gunId).isEmpty()) {
                return Optional.empty();
            }
            ResourceLocation displayId = iGun.getGunDisplayId(stack);
            if (displayId.equals(DefaultAssets.DEFAULT_GUN_DISPLAY_ID)) {
                return getClientGunIndex(gunId).map(ClientGunIndex::getDefaultDisplay);
            } else {
                return getGunDisplay(displayId, gunId);
            }
        }
        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<ClientGunIndex> getClientGunIndex(ResourceLocation gunId) {
        return Optional.ofNullable(ClientIndexManager.GUN_INDEX.get(gunId));
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<GunDisplayInstance> getGunDisplay(ResourceLocation displayId, ResourceLocation fallbackGunId) {
        if (displayId == null || displayId.equals(DefaultAssets.DEFAULT_GUN_DISPLAY_ID)) {
            return getClientGunIndex(fallbackGunId).map(ClientGunIndex::getDefaultDisplay);
        }

        GunDisplayInstance instance = ClientIndexManager.GUN_DISPLAY.get(displayId);
        if (instance == null) {
            return getClientGunIndex(fallbackGunId).map(ClientGunIndex::getDefaultDisplay);
        }
        return Optional.of(instance);
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<ClientAttachmentIndex> getClientAttachmentIndex(ResourceLocation attachmentId) {
        return Optional.ofNullable(ClientIndexManager.ATTACHMENT_INDEX.get(attachmentId));
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<ClientAmmoIndex> getClientAmmoIndex(ResourceLocation ammoId) {
        return Optional.ofNullable(ClientIndexManager.AMMO_INDEX.get(ammoId));
    }

    @OnlyIn(Dist.CLIENT)
    public static Optional<ClientBlockIndex> getClientBlockIndex(ResourceLocation blockId) {
        return Optional.ofNullable(ClientIndexManager.BLOCK_INDEX.get(blockId));
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<Map.Entry<ResourceLocation, ClientGunIndex>> getAllClientGunIndex() {
        return ClientIndexManager.getAllGuns();
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<Map.Entry<ResourceLocation, ClientAmmoIndex>> getAllClientAmmoIndex() {
        return ClientIndexManager.getAllAmmo();
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<Map.Entry<ResourceLocation, ClientAttachmentIndex>> getAllClientAttachmentIndex() {
        return ClientIndexManager.getAllAttachments();
    }

    public static Optional<CommonBlockIndex> getCommonBlockIndex(ResourceLocation blockId) {
        return Optional.ofNullable(CommonAssetsManager.get().getBlockIndex(blockId));
    }

    public static Optional<CommonGunIndex> getCommonGunIndex(ResourceLocation gunId) {
        return Optional.ofNullable(CommonAssetsManager.get().getGunIndex(gunId));
    }

    public static Optional<CommonAttachmentIndex> getCommonAttachmentIndex(ResourceLocation attachmentId) {
        return Optional.ofNullable(CommonAssetsManager.get().getAttachmentIndex(attachmentId));
    }

    public static Optional<CommonAmmoIndex> getCommonAmmoIndex(ResourceLocation ammoId) {
        return Optional.ofNullable(CommonAssetsManager.get().getAmmoIndex(ammoId));
    }

    /**
     * @deprecated
     * 不再使用独立的配方同步，而是使用原版的配方加载器<br/>
     * 请用 {@link net.minecraft.world.item.crafting.RecipeManager#byKey(ResourceLocation)}和{@link net.minecraft.world.item.crafting.RecipeManager#getAllRecipesFor(RecipeType)}获取配方
     */
    @Deprecated
    public static Optional<GunSmithTableRecipe> getRecipe(ResourceLocation recipeId) {
        return Optional.empty();
    }

    public static Set<Map.Entry<ResourceLocation, CommonBlockIndex>> getAllCommonBlockIndex() {
        return CommonAssetsManager.get().getAllBlocks();
    }

    public static Set<Map.Entry<ResourceLocation, CommonGunIndex>> getAllCommonGunIndex() {
        return CommonAssetsManager.get().getAllGuns();
    }

    public static Set<Map.Entry<ResourceLocation, CommonAmmoIndex>> getAllCommonAmmoIndex() {
        return CommonAssetsManager.get().getAllAmmos();
    }

    public static Set<Map.Entry<ResourceLocation, CommonAttachmentIndex>> getAllCommonAttachmentIndex() {
        return CommonAssetsManager.get().getAllAttachments();
    }

    /**
     * @deprecated
     * 不再使用独立的配方同步，而是使用原版的配方加载器<br/>
     * 请用 {@link net.minecraft.world.item.crafting.RecipeManager#byKey(ResourceLocation)}和{@link net.minecraft.world.item.crafting.RecipeManager#getAllRecipesFor(RecipeType)}获取配方
     */
    @Deprecated
    public static Map<ResourceLocation, GunSmithTableRecipe> getAllRecipes() {
        return Map.of();
    }

    public static void registerThirdPersonAnimation(String name, IThirdPersonAnimation animation) {
        ThirdPersonManager.register(name, animation);
    }
}
