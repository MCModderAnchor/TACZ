package com.tacz.guns.client.renderer.item;

public final class ItemPreviewRenderContext {
    private static float alpha = 1.0F;

    private ItemPreviewRenderContext() {
    }

    public static float alpha() {
        return alpha;
    }

    public static boolean isPreview() {
        return alpha() < 1.0F;
    }

    public static void setAlpha(float alpha) {
        ItemPreviewRenderContext.alpha = alpha;
    }
}
