package com.tacz.guns.client.renderer.item;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.MeshDrawResult;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.StaticMeshRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.client.renderer.external.ExternalGunEffects;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.time.Duration;

/** 第三人称手持、掉落物及固定展示枪械的即时 VBO 渲染入口。 */
public class ExternalGunItemVboRenderer {
    private static final ResourceLocation OWNER = new ResourceLocation("tacz", "external_item_guns");
    private static boolean wasEnabled;
    private static final Duration IDLE_RETENTION = Duration.ofSeconds(300);

    private ExternalGunItemVboRenderer() {
    }

    /** 不同物品显示上下文会烘焙不同的固定变换，因此分别缓存静态几何。 */
    private record ItemGeometryKey(ExternalGunAppearance.GeometryKey appearance, ItemDisplayContext context) {
    }

    public static StaticGunRenderResult tryRender(ItemStack stack, ItemDisplayContext context,
                                                  boolean useLod, PoseStack pose, MultiBufferSource buffers,
                                                  int packedLight, int packedOverlay) {
        if (!RenderConfig.STATIC_ITEM_GUN_VBO.get()) {
            if (wasEnabled) {
                StaticMeshRenderer.invalidateOwner(OWNER);
            }
            wasEnabled = false;
            return StaticGunRenderResult.UNSUPPORTED;
        }
        wasEnabled = true;
        if (!StaticMeshRenderer.isOwnerEnabled(OWNER)) {
            return StaticGunRenderResult.UNSUPPORTED;
        }
        if (ItemPreviewRenderContext.isPreview() || DisplayItemRenderContext.isActive()
                || context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.GROUND
                && (context != ItemDisplayContext.FIXED || ScreenItemVboContext.isActive())) {
            return StaticGunRenderResult.UNSUPPORTED;
        }
        // 后端或参数明确不支持时直接回退，不能一直停留在等待状态。
        if (!StaticMeshRenderer.supportsInstanceAttributes(packedLight, packedOverlay)) {
            return StaticGunRenderResult.UNSUPPORTED;
        }
        ExternalGunAppearance.Resolution resolution = ExternalGunAppearance.resolve(stack, false);
        if (resolution.pending()) {
            return StaticGunRenderResult.PENDING;
        }

        ExternalGunAppearance.Variants appearances = resolution.variants();
        if (appearances == null) {
            return StaticGunRenderResult.UNSUPPORTED;
        }

        ExternalGunAppearance.Snapshot appearance = appearances.select(useLod);
        ExternalGunAppearance.Snapshot previous = appearances.select(!useLod);
        MeshDrawResult result = StaticMeshRenderer.tryDraw(
                OWNER,
                new ItemGeometryKey(appearance.key(), context),
                new ItemGeometryKey(previous.key(), context),
                collector -> DisplayGunGeometry.capture(appearance, context, true, collector),
                pose, packedLight, packedOverlay, IDLE_RETENTION
        );
        if (result.drawn()) {
            ExternalGunAppearance.Snapshot selected = result.usedPrevious() ? previous : appearance;
            if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                ExternalGunEffects.renderThirdPerson(selected, pose, buffers);
            }
            return new StaticGunRenderResult(StaticGunRenderResult.Status.SUBMITTED, selected.useLod());
        }
        // 正常准备或可能部分绘制的调用继续接管；明确不支持／已禁用时走旧路径。
        if (result == MeshDrawResult.UNSUPPORTED) {
            return StaticGunRenderResult.UNSUPPORTED;
        }
        return StaticGunRenderResult.PENDING;
    }
}
