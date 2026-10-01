package com.tacz.guns.compat.ar;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.fml.ModList;

public class ARCompat {

	public static final String MOD_ID = "acceleratedrendering";

	public static boolean LOADED;

	public static void init() {
		LOADED = ModList.get().isLoaded(MOD_ID);
	}

	public static boolean shouldAccelerate() {
		return LOADED && ARCompatImpl.shouldAccelerate();
	}

	public static boolean isAccelerated(VertexConsumer vertexConsumer) {
		return LOADED && ARCompatImpl.isAccelerated(vertexConsumer);
	}

	public static void setRenderingLevel() {
		if (LOADED) ARCompatImpl.setRenderingLevel();
	}

	public static void resetRenderingLevel() {
		if (LOADED) ARCompatImpl.resetRenderingLevel();
	}

	public static void setRenderLayer(int layer) {
		if (LOADED) ARCompatImpl.setRenderLayer(layer);
	}

	public static void setRenderBeforeFunction(Runnable runnable) {
		if (LOADED) ARCompatImpl.setRenderBeforeFunction(runnable);
	}

	public static void setRenderAfterFunction(Runnable runnable) {
		if (LOADED) ARCompatImpl.setRenderAfterFunction(runnable);
	}

	public static Runnable getRenderBeforeFunction() {
		return LOADED ? ARCompatImpl.getRenderBeforeFunction() : Runnables.doNothing();
	}

	public static Runnable getRenderAfterFunction() {
		return LOADED ? ARCompatImpl.getRenderAfterFunction() : Runnables.doNothing();
	}

	public static void resetRenderLayer() {
		if (LOADED) ARCompatImpl.resetRenderLayer();
	}

	public static void resetRenderBeforeFunction() {
		if (LOADED) ARCompatImpl.resetRenderBeforeFunction();
	}

	public static void resetRenderAfterFunction() {
		if (LOADED) ARCompatImpl.resetRenderAfterFunction();
	}

	public static void disableAcceleration() {
		if (LOADED) ARCompatImpl.disableAcceleration();
	}

	public static void resetAcceleration() {
		if (LOADED) ARCompatImpl.resetAcceleration();
	}

	// 防止类意外加载 (直接在BeamRenderer类使用AcceleratedBeamRenderer.INSTANCE在会触发类加载)
	public static void renderLaser(
			VertexConsumer extension,
			float z,
			float width,
			boolean fadeOut,
			PoseStack poseStack,
			int color
	) {
		if (LOADED) ARCompatImpl.renderLaser(
				extension,
				z,
				width,
				fadeOut,
				poseStack,
				color
		);
	}
}
