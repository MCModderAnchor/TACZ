package com.tacz.guns.resource;

import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;

import java.util.Map;
import java.util.Set;

public interface ICommonResourceProvider {
    @Nullable GunData getGunData(ResourceLocation id);

    @Nullable AttachmentData getAttachmentData(ResourceLocation attachmentId);

    @Nullable BlockData getBlockData(ResourceLocation id);

    @Nullable RecipeFilter getRecipeFilter(ResourceLocation id);

    @Nullable CommonGunIndex getGunIndex(ResourceLocation gunId);

    @Nullable CommonAmmoIndex getAmmoIndex(ResourceLocation ammoId);

    @Nullable CommonAttachmentIndex getAttachmentIndex(ResourceLocation attachmentId);

    @Nullable CommonBlockIndex getBlockIndex(ResourceLocation blockId);

    @Nullable public LuaTable getScript(ResourceLocation scriptId);

    Set<Map.Entry<ResourceLocation, CommonGunIndex>> getAllGuns();

    Set<Map.Entry<ResourceLocation, CommonAmmoIndex>> getAllAmmos();

    Set<Map.Entry<ResourceLocation, CommonAttachmentIndex>> getAllAttachments();

    Set<Map.Entry<ResourceLocation, CommonBlockIndex>> getAllBlocks();

    Set<String> getAttachmentTags(ResourceLocation registryName);

    Set<String> getAllowAttachmentTags(ResourceLocation registryName);
}
