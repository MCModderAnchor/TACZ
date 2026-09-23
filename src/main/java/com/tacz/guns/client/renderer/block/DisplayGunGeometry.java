package com.tacz.guns.client.renderer.block;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.GeometryCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.renderer.item.SbmGunTransforms;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.EnumMap;

public final class DisplayGunGeometry {
    private DisplayGunGeometry() {}

    private static EnumMap<AttachmentType, ClientAttachmentIndex> resolveAttachments(IGun gun, ItemStack stack) {
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

    private static ItemStack effectiveAttachment(IGun gun, ItemStack stack, AttachmentType type) {
        ItemStack attached = gun.getAttachment(stack, type);
        return attached.isEmpty() ? gun.getBuiltinAttachment(stack, type) : attached;
    }

    static boolean capture(ItemStack stack, GunDisplayInstance display, ExternalGunModel external,
                           ResourceLocation texture, boolean useLod, GeometryCollector collector) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return false;
        EnumMap<AttachmentType, ClientAttachmentIndex> attachments = resolveAttachments(gun, stack);
        if (attachments == null) return false;
        DisplayGunModelInstance instance = external.displayInstance();
        instance.configureGun(gun, stack, attachments);

        PoseStack pose = new PoseStack();
        Vector3f fixedScale = display.getTransform().getScale() == null ? null : display.getTransform().getScale().getFixed();
        SbmGunTransforms.applyFixed(pose, external, fixedScale);

        renderModel(external, instance, pose, texture, collector);
        renderAttachments(instance, pose, useLod, attachments, collector);
        return true;
    }

    private static void renderAttachments(DisplayGunModelInstance instance, PoseStack pose, boolean useLod,
                                          EnumMap<AttachmentType, ClientAttachmentIndex> attachments,
                                          GeometryCollector collector) {
        for (var entry : attachments.entrySet()) {
            ClientAttachmentIndex index = entry.getValue();
            ExternalGunModel attachment = index.getExternalModel();
            ResourceLocation attachmentTexture = index.getModelTexture();
            var lod = index.getExternalLodModel();
            if (useLod && lod != null) {
                attachment = lod.getLeft();
                attachmentTexture = lod.getRight();
            }
            if (attachment == null || attachmentTexture == null) continue;
            int position = instance.attachmentPosition(entry.getKey());
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
            renderModel(attachment, attachment.displayInstance(), attachmentPose, attachmentTexture, collector);
        }
    }

    private static void renderModel(ExternalGunModel external, DisplayGunModelInstance instance, PoseStack pose,
                                    ResourceLocation texture, GeometryCollector collector) {
        var ordinary = collector.buffer(RenderType.entityCutout(texture), VertexFormat.Mode.QUADS);
        var emissive = collector.buffer(DisplayGunRenderType.emissive(texture), VertexFormat.Mode.QUADS);
        var router = new DisplayGunVertexRouter(ordinary, emissive);
        external.model().renderBoneTree(instance, pose, router, 0, OverlayTexture.NO_OVERLAY,
                1, 1, 1, 1, true, true);
    }
}
