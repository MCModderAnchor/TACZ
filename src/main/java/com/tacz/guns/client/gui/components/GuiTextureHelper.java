package com.tacz.guns.client.gui.components;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class GuiTextureHelper {
    private GuiTextureHelper() {}
    public static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture, int x, int y,
            int width, int height, int border, int sourceWidth, int sourceHeight, int u, int v) {
        int bx = Math.min(border, Math.min(width, sourceWidth) / 2);
        int by = Math.min(border, Math.min(height, sourceHeight) / 2);
        int[] dw = {bx, width - 2 * bx, bx}, dh = {by, height - 2 * by, by};
        int[] sw = {bx, sourceWidth - 2 * bx, bx}, sh = {by, sourceHeight - 2 * by, by};
        int dy = y, sy = v;
        for (int row = 0; row < 3; row++) {
            int dx = x, sx = u;
            for (int col = 0; col < 3; col++) {
                if (sw[col] > 0 && sh[row] > 0) {
                    for (int oy = 0; oy < dh[row]; oy += sh[row]) {
                        for (int ox = 0; ox < dw[col]; ox += sw[col]) {
                            graphics.blit(texture, dx + ox, dy + oy, sx, sy,
                                    Math.min(sw[col], dw[col] - ox), Math.min(sh[row], dh[row] - oy));
                        }
                    }
                }
                dx += dw[col]; sx += sw[col];
            }
            dy += dh[row]; sy += sh[row];
        }
    }
}
