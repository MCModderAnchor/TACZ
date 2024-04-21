package com.tac.guns.client.gui.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GunLevelUpToast implements Toast {
    private final Component title;
    private final Component subTitle;
    private final ItemStack icon;

    public GunLevelUpToast(ItemStack icon, Component titleComponent, @Nullable Component subtitle) {
        this.icon = icon;
        this.title = titleComponent;
        this.subTitle = subtitle;
    }

    @NotNull
    public Visibility render(@NotNull PoseStack pPoseStack, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        pToastComponent.blit(pPoseStack, 0, 0, 0, 0, this.width(), this.height());

        List<FormattedCharSequence> list = null;
        if (this.subTitle != null) {
            list = pToastComponent.getMinecraft().font.split(this.subTitle, 125);
        }
        int i = 16776960;
        if (list != null) {
            if (list.size() == 1) {
                pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 30.0F, 7.0F, i | -16777216);
                pToastComponent.getMinecraft().font.draw(pPoseStack, list.get(0), 30.0F, 18.0F, -1);
            } else {
                if (pTimeSinceLastVisible < 1500L) {
                    int k = Mth.floor(Mth.clamp((float) (1500L - pTimeSinceLastVisible) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
                    pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 30.0F, 11.0F, i | k);
                } else {
                    int i1 = Mth.floor(Mth.clamp((float) (pTimeSinceLastVisible - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
                    int l = this.height() / 2 - list.size() * 9 / 2;

                    for (FormattedCharSequence formattedCharSequence : list) {
                        pToastComponent.getMinecraft().font.draw(pPoseStack, formattedCharSequence, 30.0F, (float) l, 16777215 | i1);
                        l += 9;
                    }
                }
            }
        }

        pToastComponent.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(this.icon, 8, 8);
        return pTimeSinceLastVisible >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

}
