package com.tacz.guns.client.renderer.external;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.util.SlotAdapterHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Static external appearance shared by display guns and item VBO rendering. */
public class ExternalGunAppearance {
    private ExternalGunAppearance() {}

    public record AttachmentKey(AttachmentType type, ResourceLocation id) {}

    /** Only properties that change captured vertices or material belong in this key. */
    public record GeometryKey(long resourceRevision, ResourceLocation displayId,
                              List<AttachmentKey> attachments, boolean bullet,
                              boolean ammo, boolean useLod) {}

    public record Snapshot(GeometryKey key, ItemStack stack, GunDisplayInstance display,
                           ExternalGunModel model, ResourceLocation texture, boolean useLod) {}

    public record Variants(Snapshot near, Snapshot far) {
        public Snapshot select(boolean useLod) { return useLod ? far : near; }
    }

    /** Returns null only when the item cannot be represented by the current static capture path. */
    @Nullable
    public static Variants resolve(ItemStack source) {
        return resolve(source, true);
    }

    /**
     * copyStack is required for display-slot snapshots retained across frames. Immediate item capture
     * finishes before the render call returns, so it can use the caller's stack without a per-frame copy.
     */
    @Nullable
    public static Variants resolve(ItemStack source, boolean copyStack) {
        if (source.isEmpty()) return null;
        IGun gun = IGun.getIGunOrNull(source);
        if (gun == null) return null;
        GunDisplayInstance display = TimelessAPI.getGunDisplay(source).orElse(null);
        if (display == null) return null;
        ExternalGunModel model = display.getExternalGunModel();
        ResourceLocation texture = display.getModelTexture();
        Pair<ExternalGunModel, ResourceLocation> lod = display.getExternalLodModel();
        if (model == null || texture == null || display.getLodModel() != null && lod == null) return null;

        ItemStack stack = copyStack ? source.copy() : source;
        List<AttachmentKey> attachments = new ArrayList<>();
        boolean lodAffectsGeometry = lod != null;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            ItemStack attached = gun.getAttachment(stack, type);
            if (attached.isEmpty()) attached = gun.getBuiltinAttachment(stack, type);
            IAttachment item = IAttachment.getIAttachmentOrNull(attached);
            if (item == null) continue;
            ResourceLocation id = item.getAttachmentId(attached);
            // The display capture does not yet implement variants or slot adapters.
            if (TimelessAPI.getClientAttachmentVariantIndex(attached).isPresent()
                    || SlotAdapterHelper.getEffectiveSlotAdapter(stack, type, id) != null) return null;
            ClientAttachmentIndex index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) return null;
            lodAffectsGeometry |= index.getLodModel() != null;
            attachments.add(new AttachmentKey(type, id));
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
            if (farModel == null || farTexture == null) return null;
            GeometryKey farKey = new GeometryKey(revision, display.getDisplayId(), attachmentKeys, bullet, ammo, true);
            far = new Snapshot(farKey, stack, display, farModel, farTexture, true);
        }
        return new Variants(near, far);
    }
}
