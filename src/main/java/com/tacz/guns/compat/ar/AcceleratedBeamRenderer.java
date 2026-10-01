package com.tacz.guns.compat.ar;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AcceleratedBeamRenderer implements IAcceleratedRenderer<BeamRenderContext> {

	public static final AcceleratedBeamRenderer INSTANCE = new AcceleratedBeamRenderer();

	@Override
	public void render(VertexConsumer vertexConsumer, BeamRenderContext context, Matrix4f transform, Matrix3f normal, int light, int overlay, int color) {
		var halfWidth = context.width() / 2;
		var z = context.z();
		var endColor =  (context.fadeOut() ? 0 : 255) << 24 | color & 0xFF_FF_FF;
		var extension = VertexConsumerExtension.getAccelerated(vertexConsumer);

		extension.beginTransform(transform, normal);

		vertexConsumer.addVertex(-halfWidth, -halfWidth, 0).setColor(color).setUv(0, 0).setLight(light).setOverlay(overlay).setNormal(-1, 0, 0);
		vertexConsumer.addVertex(-halfWidth, halfWidth, 0).setColor(color).setUv(0, 1).setLight(light).setOverlay(overlay).setNormal(-1, 0, 0);
		vertexConsumer.addVertex(-halfWidth, halfWidth, z).setColor(endColor).setUv(1, 1).setLight(light).setOverlay(overlay).setNormal(-1, 0, 0);
		vertexConsumer.addVertex(-halfWidth, -halfWidth, z).setColor(endColor).setUv(1, 0).setLight(light).setOverlay(overlay).setNormal(-1, 0, 0);

		extension.beginTransform(transform, normal);

		vertexConsumer.addVertex(-halfWidth, halfWidth, 0).setColor(color).setUv(0, 0).setLight(light).setOverlay(overlay).setNormal(0, 1, 0);
		vertexConsumer.addVertex(halfWidth, halfWidth, 0).setColor(color).setUv(0, 1).setLight(light).setOverlay(overlay).setNormal(0, 1, 0);
		vertexConsumer.addVertex(halfWidth, halfWidth, z).setColor(endColor).setUv(1, 1).setLight(light).setOverlay(overlay).setNormal(0, 1, 0);
		vertexConsumer.addVertex(-halfWidth, halfWidth, z).setColor(endColor).setUv(1, 0).setLight(light).setOverlay(overlay).setNormal(0, 1, 0);

		extension.beginTransform(transform, normal);

		vertexConsumer.addVertex(halfWidth, halfWidth, 0).setColor(color).setUv(0, 0).setLight(light).setOverlay(overlay).setNormal(1, 0, 0);
		vertexConsumer.addVertex(halfWidth, -halfWidth, 0).setColor(color).setUv(0, 1).setLight(light).setOverlay(overlay).setNormal(1, 0, 0);
		vertexConsumer.addVertex(halfWidth, -halfWidth, z).setColor(endColor).setUv(1, 1).setLight(light).setOverlay(overlay).setNormal(1, 0, 0);
		vertexConsumer.addVertex(halfWidth, halfWidth, z).setColor(endColor).setUv(1, 0).setLight(light).setOverlay(overlay).setNormal(1, 0, 0);

		extension.beginTransform(transform, normal);

		vertexConsumer.addVertex(halfWidth, -halfWidth, 0).setColor(color).setUv(0, 1).setLight(light).setOverlay(overlay).setNormal(0, -1, 0);
		vertexConsumer.addVertex(-halfWidth, -halfWidth, 0).setColor(color).setUv(0, 1).setLight(light).setOverlay(overlay).setNormal(0, -1, 0);
		vertexConsumer.addVertex(-halfWidth, -halfWidth, z).setColor(endColor).setUv(1, 1).setLight(light).setOverlay(overlay).setNormal(0, -1, 0);
		vertexConsumer.addVertex(halfWidth, -halfWidth, z).setColor(endColor).setUv(1, 0).setLight(light).setOverlay(overlay).setNormal(0, -1, 0);

		extension.endTransform();
	}

}
