package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReloadKey {
    public static final KeyMapping RELOAD_KEY = new KeyMapping("key.tacz.reload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.category.tacz");

    public static boolean onReloadPress() {
        if (isInGame()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            if (player.getMainHandItem().getItem() instanceof IGun iGun) {
                // 如果使用背包直读，且没有换弹冷却机制，则在输入时就屏蔽换弹
                if (iGun.useInventoryAmmo(player.getMainHandItem())) {
                    return false;
                }
                IClientPlayerGunOperator.fromLocalPlayer(player).reload();
                return true;
            }
        }
        return false;
    }

    public static boolean onReloadControllerPress(boolean isPress) {
        return isPress && onReloadPress();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void autoReload(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != LogicalSide.CLIENT) {
            return;
        }

        if (!KeyConfig.AUTO_RELOAD.get()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || player.tickCount % 5 != 0) {
            return;
        }
        ItemStack currentGunItem = player.getMainHandItem();
        if (player.getMainHandItem().getItem() instanceof IGun iGun) {
            // 如果使用背包直读，且没有换弹冷却机制，则在输入时就屏蔽换弹
            if (iGun.useInventoryAmmo(player.getMainHandItem())) {
                return;
            }
            boolean flag = TimelessAPI.getCommonGunIndex(iGun.getGunId(currentGunItem))
                    .map(gunIndex -> gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT)
                    .orElse(false);

            int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (iGun.hasBulletInBarrel(currentGunItem) && flag ? 1 : 0);
            if (ammoCount > 0) {
                return;
            }
            IClientPlayerGunOperator.fromLocalPlayer(player).reload();
        }
    }
}
