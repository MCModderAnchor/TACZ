package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.mixin.client.RenderSystemAccessor;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class UnshadedVertexConsumer implements VertexConsumer {
    private static final float FALLBACK_NORMAL_X = 0.0F;
    private static final float FALLBACK_NORMAL_Y = 1.0F;
    private static final float FALLBACK_NORMAL_Z = 0.0F;

    private final VertexConsumer delegate;
    private final float normalX;
    private final float normalY;
    private final float normalZ;

    private UnshadedVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;

        Vector3f[] lightDirections = RenderSystemAccessor.getShaderLightDirections();
        Vector3f primaryLight = lightDirections.length > 0 ? lightDirections[0] : null;
        if (primaryLight == null) {
            this.normalX = FALLBACK_NORMAL_X;
            this.normalY = FALLBACK_NORMAL_Y;
            this.normalZ = FALLBACK_NORMAL_Z;
            return;
        }

        float maxComponent = Math.max(Math.abs(primaryLight.x()),
                Math.max(Math.abs(primaryLight.y()), Math.abs(primaryLight.z())));
        if (maxComponent < 1.0E-6F) {
            this.normalX = FALLBACK_NORMAL_X;
            this.normalY = FALLBACK_NORMAL_Y;
            this.normalZ = FALLBACK_NORMAL_Z;
            return;
        }

        // The entity shader does not normalize vertex normals. Scaling the primary light direction
        // until its largest component reaches one makes minecraft_mix_light clamp to full brightness.
        this.normalX = primaryLight.x() / maxComponent;
        this.normalY = primaryLight.y() / maxComponent;
        this.normalZ = primaryLight.z() / maxComponent;
    }

    private UnshadedVertexConsumer(VertexConsumer delegate, float normalX, float normalY, float normalZ) {
        this.delegate = delegate;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }

    public static VertexConsumer wrap(VertexConsumer consumer) {
        if (consumer instanceof UnshadedVertexConsumer) {
            return consumer;
        }
        return new UnshadedVertexConsumer(consumer);
    }

    public static VertexConsumer wrapFixedNormal(VertexConsumer consumer) {
        return new UnshadedVertexConsumer(consumer,
                FALLBACK_NORMAL_X, FALLBACK_NORMAL_Y, FALLBACK_NORMAL_Z);
    }

    public VertexConsumer getDelegate() {
        return delegate;
    }

    public Matrix3f createNormalMatrix() {
        // Accelerated meshes store +Y as their canonical unshaded normal. The second matrix column
        // maps that normal to the shader light direction captured for this render call.
        return new Matrix3f()
                .m10(normalX)
                .m11(normalY)
                .m12(normalZ);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(normalX, normalY, normalZ);
        return this;
    }

}
