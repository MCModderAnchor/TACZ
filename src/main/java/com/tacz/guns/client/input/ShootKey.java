package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.pojo.data.gun.BurstData;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ShootKey {
    private static boolean burstContinuousShootState = false;
    private static long burstNextTimestamp = -1L;
    private static int burstShootCounter = 0;

    public static final KeyMapping SHOOT_KEY = new KeyMapping("key.tacz.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.tacz");

    @SubscribeEvent
    public static void autoShoot(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (SHOOT_KEY.isDown() && fireMode == FireMode.AUTO) {
                operator.shoot();
                return;
            }
            if (burstContinuousShootState && fireMode == FireMode.BURST) {
                burstShoot(iGun, mainHandItem, operator);
            }
        }
    }

    @SubscribeEvent
    public static void semiShoot(InputEvent.MouseInputEvent event) {
        if (isInGame() && SHOOT_KEY.matchesMouse(event.getButton())) {
            // 松开鼠标，重置 DryFire 状态
            if (event.getAction() == GLFW.GLFW_RELEASE) {
                SoundPlayManager.resetDryFireSound();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) {
                return;
            }
            if (IGun.mainhandHoldGun(player)) {
                FireMode fireMode = IGun.getMainhandFireMode(player);
                if (fireMode == FireMode.UNKNOWN) {
                    player.sendMessage(new TranslatableComponent("message.tacz.fire_select.fail"), Util.NIL_UUID);
                }
                if (fireMode == FireMode.SEMI) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).shoot();
                }
                if (fireMode == FireMode.BURST) {
                    burstContinuousShootState = true;
                }
            }
        }
    }

    public static void resetBurstState() {
        burstContinuousShootState = false;
        burstShootCounter = 0;
    }

    private static void burstShoot(IGun iGun, ItemStack mainHandItem, IClientPlayerGunOperator operator) {
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            BurstData burstData = index.getGunData().getBurstData();
            // 如果还没到下一组时间
            if (System.currentTimeMillis() < burstNextTimestamp) {
                return;
            }
            // 开始连发射击
            ShootResult result = operator.shoot();
            if (result == ShootResult.SUCCESS) {
                burstShootCounter += 1;
                // 检查连发计数
                if (burstShootCounter >= Math.max(burstData.getCount(), 1)) {
                    // 连发到达上限，记录相关数据
                    long interval = Math.max((long) (burstData.getMinInterval() * 1000), 50L);
                    burstNextTimestamp = System.currentTimeMillis() + interval;
                    burstContinuousShootState = burstData.isContinuousShoot() && SHOOT_KEY.isDown();
                    burstShootCounter = 0;
                }
            } else if (result != ShootResult.COOL_DOWN) {
                resetBurstState();
            }
        });
    }
}
