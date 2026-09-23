package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** 将 SBM 的普通／自发光顶点送到两个完整的 RenderType pass。 */
final class DisplayGunVertexRouter implements VertexConsumer {
    private final VertexConsumer normal;
    private final VertexConsumer emissive;
    private double x, y, z;
    private int red = 255, green = 255, blue = 255, alpha = 255;
    private float u, v;
    private int overlayU, overlayV, lightU, lightV;
    private float normalX, normalY, normalZ;

    DisplayGunVertexRouter(VertexConsumer normal, VertexConsumer emissive) {
        this.normal = normal;
        this.emissive = emissive;
    }

    @Override public VertexConsumer vertex(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
        return this;
    }
    @Override public VertexConsumer color(int r, int g, int b, int a) {
        red = r; green = g; blue = b; alpha = a;
        return this;
    }
    @Override public VertexConsumer uv(float u, float v) {
        this.u = u; this.v = v;
        return this;
    }
    @Override public VertexConsumer overlayCoords(int u, int v) {
        overlayU = u; overlayV = v;
        return this;
    }
    @Override public VertexConsumer uv2(int u, int v) {
        lightU = u; lightV = v;
        return this;
    }
    @Override public VertexConsumer normal(float x, float y, float z) {
        normalX = x; normalY = y; normalZ = z;
        return this;
    }
    @Override public void endVertex() {
        boolean isEmissive = lightU != 0 || lightV != 0;
        VertexConsumer target = isEmissive ? emissive : normal;
        target.vertex(x, y, z).color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(overlayU, overlayV).uv2(isEmissive ? lightU : 0, isEmissive ? lightV : 0)
                .normal(normalX, normalY, normalZ).endVertex();
        red = green = blue = alpha = 255;
        u = v = 0;
        overlayU = overlayV = lightU = lightV = 0;
        normalX = normalY = normalZ = 0;
    }
    @Override public void defaultColor(int r, int g, int b, int a) {
        normal.defaultColor(r, g, b, a);
        emissive.defaultColor(r, g, b, a);
    }
    @Override public void unsetDefaultColor() {
        normal.unsetDefaultColor();
        emissive.unsetDefaultColor();
    }
}
