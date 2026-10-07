package com.tacz.guns.mixin.client;

import com.tacz.guns.client.gui.components.CreativePackFilters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    private float scrollOffs;
    @Unique
    private CreativePackFilters tacz$packFilters;

    protected CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tacz$initPackFilters(CallbackInfo ci) {
        if (tacz$packFilters == null) {
            tacz$packFilters = new CreativePackFilters((CreativeModeInventoryScreen) (Object) this, () -> scrollOffs = 0);
        }
        // 直接 this::addRenderableWidget 会导致编译器生成妙妙方法直接引用 CreativeModeInventoryScreenMixin 然后原地爆炸
        tacz$packFilters.init(widget -> addRenderableWidget(widget), selectedTab);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void tacz$selectPackFilterTab(CreativeModeTab tab, CallbackInfo ci) {
        if (tacz$packFilters != null) {
            tacz$packFilters.updateTab(tab, true);
        }
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void tacz$refreshPackFilterItems(Collection<ItemStack> items, CallbackInfo ci) {
        if (tacz$packFilters != null) {
            tacz$packFilters.updateTab(selectedTab, true);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void tacz$updatePackFilters(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (tacz$packFilters != null) {
            tacz$packFilters.updateTab(selectedTab, false);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void tacz$scrollPackFilters(double mouseX, double mouseY, double delta, CallbackInfoReturnable<Boolean> cir) {
        if (tacz$packFilters != null && tacz$packFilters.mouseScrolled(mouseX, mouseY, delta)) {
            cir.setReturnValue(true);
        }
    }
}
