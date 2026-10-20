package com.tacz.guns.client.renderer.external;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import com.tacz.guns.util.SlotAdapterHelper;
import com.tacz.guns.util.VariantHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 展示枪械与物品 VBO 渲染共用的静态外观。 */
public class ExternalGunAppearance {
    private ExternalGunAppearance() {
    }

    public record AttachmentKey(AttachmentType type, ResourceLocation id,
                                @Nullable ResourceLocation variantId,
                                @Nullable ResourceLocation slotAdapterId) {
    }

    /** 几何键只包含会改变捕获顶点或材质的属性。 */
    public record GeometryKey(long resourceRevision, ResourceLocation displayId,
                              List<AttachmentKey> attachments, boolean bullet,
                              boolean ammo, boolean useLod) {
    }

    public record Snapshot(GeometryKey key, ItemStack stack, GunDisplayInstance display,
                           ExternalGunModel model, ResourceLocation texture, boolean useLod) {
    }

    public record Variants(Snapshot near, Snapshot far) {
        public Snapshot select(boolean useLod) {
            return useLod ? far : near;
        }
    }

    public enum Status {
        READY,
        PENDING,
        UNSUPPORTED
    }

    public record Resolution(Status status, @Nullable Variants variants) {
        public static final Resolution PENDING = new Resolution(Status.PENDING, null);
        public static final Resolution UNSUPPORTED = new Resolution(Status.UNSUPPORTED, null);

        public boolean pending() {
            return status == Status.PENDING;
        }
    }

    public static Resolution resolve(ItemStack source) {
        return resolve(source, true);
    }

    /**
     * 展示槽位的快照需要跨帧保留，因此必须复制物品栈。
     * 即时物品捕获在当前渲染调用返回前完成，可以直接使用调用方的物品栈。
     */
    public static Resolution resolve(ItemStack source, boolean copyStack) {
        if (source.isEmpty()) {
            return Resolution.UNSUPPORTED;
        }

        IGun gun = IGun.getIGunOrNull(source);
        if (gun == null) {
            return Resolution.UNSUPPORTED;
        }

        GunDisplayInstance display = TimelessAPI.getGunDisplay(source).orElse(null);
        if (display == null) {
            return Resolution.UNSUPPORTED;
        }

        List<AttachmentModels> dependencies = attachmentModels(gun, source);
        if (dependencies == null) {
            return Resolution.UNSUPPORTED;
        }

        // 每次只解析一次依赖；所有后台任务结束后才读取旧模型的兼容性信息。
        boolean prepared = display.getExternalModels().prepare();
        for (AttachmentModels dependency : dependencies) {
            prepared &= dependency.prepare();
        }
        if (!prepared) {
            return Resolution.PENDING;
        }

        ExternalGunModel model = display.getExternalGunModel();
        if (model == null) {
            return Resolution.UNSUPPORTED;
        }

        ResourceLocation texture = display.getModelTexture();
        Pair<ExternalGunModel, ResourceLocation> lod = display.getExternalLodModel();
        if (texture == null || display.getLodModel() != null && lod == null) {
            return Resolution.UNSUPPORTED;
        }

        ItemStack stack = copyStack ? source.copy() : source;
        List<AttachmentKey> attachments = new ArrayList<>();
        boolean lodAffectsGeometry = lod != null;
        for (AttachmentModels dependency : dependencies) {
            if (!dependency.supported()) {
                return Resolution.UNSUPPORTED;
            }
            lodAffectsGeometry |= dependency.index().getLodModel() != null;
            attachments.add(dependency.key());
        }

        long revision = ClientIndexManager.resourceRevision();
        List<AttachmentKey> attachmentKeys = List.copyOf(attachments);
        boolean bullet = gun.hasBulletInBarrel(stack);
        boolean ammo = gun.getCurrentAmmoCount(stack) > 0;
        GeometryKey nearKey = new GeometryKey(revision, display.getDisplayId(), attachmentKeys, bullet, ammo, false);
        Snapshot near = new Snapshot(nearKey, stack, display, model, texture, false);
        Snapshot far = near;
        if (lodAffectsGeometry) {
            ExternalGunModel farModel = lod == null ? model : lod.getLeft();
            ResourceLocation farTexture = lod == null ? texture : lod.getRight();
            if (farModel == null || farTexture == null) {
                return Resolution.UNSUPPORTED;
            }
            GeometryKey farKey = new GeometryKey(revision, display.getDisplayId(), attachmentKeys, bullet, ammo, true);
            far = new Snapshot(farKey, stack, display, farModel, farTexture, true);
        }
        return new Resolution(Status.READY, new Variants(near, far));
    }

    public record AttachmentModels(AttachmentKey key, ClientAttachmentIndex index,
                                    @Nullable ClientAttachmentVariantIndex variant,
                                    @Nullable ClientSlotAdapterIndex adapter) {
        public boolean prepare() {
            boolean prepared = index.getExternalModels().prepare();
            if (variant != null) {
                prepared &= variant.getExternalModels().prepare();
            }
            if (adapter != null) {
                prepared &= adapter.getExternalModels().prepare();
            }
            return prepared;
        }

        public boolean supported() {
            boolean replacesBase = variant != null && variant.isReplaceMode()
                    && variant.getAttachmentModel() != null && variant.getModelTexture() != null;
            if (!replacesBase && index.getAttachmentModel() != null && index.getModelTexture() != null
                    && index.getExternalModel() == null) {
                return false;
            }
            if (variant != null && variant.getAttachmentModel() != null && variant.getModelTexture() != null
                    && variant.getExternalModel() == null) {
                return false;
            }
            if (index.getLodModel() != null && index.getExternalLodModel() == null) {
                return false;
            }
            return adapter == null || adapter.getAdapterModel() == null || adapter.getModelTexture() == null
                    || adapter.getExternalModel() != null;
        }
    }

    public static @Nullable List<AttachmentModels> attachmentModels(IGun gun, ItemStack stack) {
        List<AttachmentModels> models = new ArrayList<>();
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }

            ItemStack attached = gun.getAttachment(com.tacz.guns.client.util.ClientRegistryAccess.registries(), stack, type);
            if (attached.isEmpty()) {
                attached = gun.getBuiltinAttachment(stack, type);
            }

            IAttachment item = IAttachment.getIAttachmentOrNull(attached);
            if (item == null) {
                continue;
            }

            ResourceLocation id = item.getAttachmentId(attached);
            ClientAttachmentIndex index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) {
                return null;
            }

            ClientAttachmentVariantIndex variant = TimelessAPI.getClientAttachmentVariantIndex(attached).orElse(null);
            ResourceLocation variantId = variant == null ? null : VariantHelper.getVariant(attached);
            ResourceLocation selectedAdapter = SlotAdapterHelper.getEffectiveSlotAdapter(stack, type, id);
            ClientSlotAdapterIndex adapter = selectedAdapter == null ? null
                    : TimelessAPI.getClientSlotAdapterIndex(selectedAdapter).orElse(null);
            AttachmentKey key = new AttachmentKey(type, id, variantId, adapter == null ? null : selectedAdapter);
            models.add(new AttachmentModels(key, index, variant, adapter));
        }
        return models;
    }
}
