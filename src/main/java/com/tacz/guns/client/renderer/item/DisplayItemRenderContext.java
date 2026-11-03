package com.tacz.guns.client.renderer.item;

/** 展示柜／挂板的旧模型入口；与物品框的 FIXED 调用区分。 */
public final class DisplayItemRenderContext {
    private static int depth;

    private DisplayItemRenderContext() {}

    public static boolean isActive() { return depth > 0; }

    public static void render(Runnable action) {
        depth++;
        try { action.run(); }
        catch (RuntimeException failure) {
            depth--;
            throw failure; // 旧模型异常交给原版错误日志／崩溃处理，不能静默继续。
        }
        depth--;
    }
}
