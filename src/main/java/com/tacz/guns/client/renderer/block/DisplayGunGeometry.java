package com.tacz.guns.client.renderer.block;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.GeometryCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.client.renderer.item.SbmGunTransforms;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import com.tacz.guns.util.SlotAdapterHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.EnumMap;

public class DisplayGunGeometry {
    private DisplayGunGeometry() {}

    public static EnumMap<AttachmentType, ClientAttachmentIndex> resolveAttachments(IGun gun, ItemStack stack) {
        EnumMap<AttachmentType, ClientAttachmentIndex> indexes = new EnumMap<>(AttachmentType.class);
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            ItemStack attachment = effectiveAttachment(gun, stack, type);
            if (attachment.isEmpty()) continue;
            IAttachment item = IAttachment.getIAttachmentOrNull(attachment);
            if (item == null) continue;
            ResourceLocation id = item.getAttachmentId(attachment);
            ClientAttachmentIndex index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) return null;
            indexes.put(type, index);
        }
        return indexes;
    }

    public static ItemStack effectiveAttachment(IGun gun, ItemStack stack, AttachmentType type) {
        ItemStack attached = gun.getAttachment(stack, type);
        return attached.isEmpty() ? gun.getBuiltinAttachment(stack, type) : attached;
    }

    public static boolean capture(ExternalGunAppearance.Snapshot snapshot, ItemDisplayContext context,
                                  GeometryCollector collector) {
        return capture(snapshot, context, false, collector);
    }

    /** compensateItemRenderer is true for an immediate BEWLR capture after ItemRenderer's -0.5 translation. */
    public static boolean capture(ExternalGunAppearance.Snapshot snapshot, ItemDisplayContext context,
                                  boolean compensateItemRenderer, GeometryCollector collector) {
        ItemStack stack = snapshot.stack();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return false;
        EnumMap<AttachmentType, ClientAttachmentIndex> attachments = resolveAttachments(gun, stack);
        if (attachments == null) return false;
        ExternalGunModel external = snapshot.model();
        DisplayGunModelInstance instance = external.displayInstance();
        instance.configureGun(gun, stack, attachments);

        PoseStack pose = new PoseStack();
        var scales = snapshot.display().getTransform().getScale();
        switch (context) {
            case FIXED -> {
                // 展示槽位绕过 ItemRenderer；即时 FIXED 的 BEWLR 则收到其 -0.5 居中变换。
                if (compensateItemRenderer) pose.translate(0.5, 0.5, 0.5);
                SbmGunTransforms.applyFixed(pose, external, scales == null ? null : scales.getFixed());
            }
            case GROUND -> {
                pose.translate(0.5, 0.5, 0.5);
                SbmGunTransforms.applyGround(pose, external, scales == null ? null : scales.getGround());
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                SbmGunTransforms.applyThirdPerson(pose, external,
                        scales == null ? null : scales.getThirdPerson());
            }
            default -> { return false; }
        }
        renderModel(external, instance, pose, snapshot.texture(), collector);
        renderAttachments(instance, pose, snapshot.useLod(), gun, stack, attachments, collector);
        return true;
    }

    public static void renderAttachments(DisplayGunModelInstance instance, PoseStack pose, boolean useLod,
                                          IGun gun, ItemStack gunStack,
                                          EnumMap<AttachmentType, ClientAttachmentIndex> attachments,
                                          GeometryCollector collector) {
        for (var entry : attachments.entrySet()) {
            AttachmentType type = entry.getKey();
            ClientAttachmentIndex index = entry.getValue();
            ItemStack attached = effectiveAttachment(gun, gunStack, type);
            IAttachment attachmentItem = IAttachment.getIAttachmentOrNull(attached);
            if (attachmentItem == null) continue;
            ResourceLocation attachmentId = attachmentItem.getAttachmentId(attached);
            int position = instance.attachmentPosition(type);
            // LOD 模型可能省略挂点；旧渲染路径此时也不会绘制该配件。
            if (position < 0) continue;
            PoseStack attachmentPose = new PoseStack();
            attachmentPose.last().pose().mul(pose.last().pose());
            attachmentPose.last().normal().mul(pose.last().normal());
            instance.mulGlobalTransform(attachmentPose, position);
            // SBM 的骨骼矩阵用于变换已按模型绝对坐标烘焙的顶点；挂点原点还需加上 pivot。
            // pivot 已由烘焙器从 Blockbench 单位换算为方块单位，配件模型也使用同一坐标系。
            var mount = instance.getBone(position).definition();
            attachmentPose.translate(mount.pivotX(), mount.pivotY(), mount.pivotZ());

            ResourceLocation adapterId = SlotAdapterHelper.getEffectiveSlotAdapter(gunStack, type, attachmentId);
            ClientSlotAdapterIndex adapter = adapterId == null ? null
                    : TimelessAPI.getClientSlotAdapterIndex(adapterId).orElse(null);
            if (adapter != null) {
                renderOptionalModel(adapter.getExternalModel(), adapter.getModelTexture(), attachmentPose, collector);
                applyMountOffset(attachmentPose, adapter.getMountOffset());
            }

            ClientAttachmentVariantIndex variant = TimelessAPI.getClientAttachmentVariantIndex(attached).orElse(null);
            if (variant != null && variant.isOverlayMode()) {
                renderOptionalModel(variant.getExternalModel(), variant.getModelTexture(), attachmentPose, collector);
                applyMountOffset(attachmentPose, variant.getMountOffset());
            }

            ExternalGunModel attachment = index.getExternalModel();
            ResourceLocation attachmentTexture = index.getModelTexture();
            if (variant != null && variant.isReplaceMode()
                    && variant.getAttachmentModel() != null && variant.getModelTexture() != null) {
                attachment = variant.getExternalModel();
                attachmentTexture = variant.getModelTexture();
            }
            // 与 AttachmentRender 一致：普通配件的远距模型在 replace 变体之后选择。
            var lod = index.getExternalLodModel();
            if (useLod && lod != null) {
                attachment = lod.getLeft();
                attachmentTexture = lod.getRight();
            }
            renderOptionalModel(attachment, attachmentTexture, attachmentPose, collector);
        }
    }

    private static void renderOptionalModel(ExternalGunModel model, ResourceLocation texture,
                                            PoseStack pose, GeometryCollector collector) {
        if (model != null && texture != null)
            renderModel(model, model.displayInstance(), pose, texture, collector);
    }

    private static void applyMountOffset(PoseStack pose, Vector3f offset) {
        if (offset != null) {
            // AttachmentRender 的偏移处在旧模型坐标；SBM 副本的 X/Y 轴与之反向。
            pose.translate(-offset.x() / 16.0F, offset.y() / 16.0F, offset.z() / 16.0F);
        }
    }

    public static void renderModel(ExternalGunModel external, DisplayGunModelInstance instance, PoseStack pose,
                                    ResourceLocation texture, GeometryCollector collector) {
        var buffer = collector.buffer(RenderType.entityCutout(texture),
                DisplayGunRenderType.emissive(texture), VertexFormat.Mode.QUADS);
        external.model().renderBoneTree(instance, pose, buffer, 0, OverlayTexture.NO_OVERLAY,
                1, 1, 1, 1, true, true);
    }
}
