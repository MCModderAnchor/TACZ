package com.tacz.guns.compat.ar;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ARCompatImpl {

	public static boolean shouldAccelerate() {
		return CoreFeature.isLoaded()
				&& AcceleratedEntityRenderingFeature.isEnabled()
				&& AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
				&& (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingHand() || (CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui()));
	}

	public static boolean isAccelerated(VertexConsumer vertexConsumer) {
		return VertexConsumerExtension.getAccelerated(vertexConsumer).isAccelerated();
	}

	public static void setRenderingLevel() {
		CoreFeature.setRenderingLevel();
	}

	public static void resetRenderingLevel() {
		CoreFeature.resetRenderingLevel();
	}

	public static void setRenderLayer(int layer) {
		CoreFeature.forceSetDefaultLayer(layer);
	}

	public static void setRenderBeforeFunction(Runnable runnable) {
		CoreFeature.forceSetDefaultLayerBeforeFunction(runnable);
	}

	public static void setRenderAfterFunction(Runnable runnable) {
		CoreFeature.forceSetDefaultLayerAfterFunction(runnable);
	}

	public static void resetRenderLayer() {
		CoreFeature.resetDefaultLayer();
	}

	public static void resetRenderBeforeFunction() {
		CoreFeature.resetDefaultLayerBeforeFunction();
	}

	public static void resetRenderAfterFunction() {
		CoreFeature.resetDefaultLayerAfterFunction();
	}

	public static void disableAcceleration() {
		AcceleratedEntityRenderingFeature.useVanillaPipeline();
		AcceleratedItemRenderingFeature.useVanillaPipeline();
		AcceleratedTextRenderingFeature.useVanillaPipeline();
	}

	public static void resetAcceleration() {
		AcceleratedEntityRenderingFeature.resetPipeline();
		AcceleratedItemRenderingFeature.resetPipeline();
		AcceleratedTextRenderingFeature.resetPipeline();
	}

	public static void renderLaser(
			VertexConsumer vertexConsumer,
			float z,
			float width,
			boolean fadeOut,
			PoseStack poseStack,
			int color
	) {
		VertexConsumerExtension.getAccelerated(vertexConsumer).doRender(
				//直接使用AcceleratedBeamRenderer进行渲染
				AcceleratedBeamRenderer.INSTANCE,
				// 渲染Beam所需要的参数
				new BeamRenderContext(z, width, fadeOut),
				poseStack.last().pose(),
				poseStack.last().normal(),
				LightTexture.pack(15, 15),
				OverlayTexture.NO_OVERLAY,
				color
		);
	}
}
