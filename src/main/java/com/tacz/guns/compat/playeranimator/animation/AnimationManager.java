package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.compat.playeranimator.Condition;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

public class AnimationManager {
    @SuppressWarnings("unchecked")
    public static boolean onHoldOrAim(AbstractClientPlayer player, ClientGunIndex gunIndex, float limbSwingAmount) {
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        float aimingProgress = operator.getSynAimingProgress();
        if (player.isSprinting()) {
            return playBaseAnimation(player, gunIndex, PlayerAnimatorCompat.BASE_ANIMATION_ID, Condition.RUN);
        }
        if (limbSwingAmount > 0.05) {
            return playBaseAnimation(player, gunIndex, PlayerAnimatorCompat.BASE_ANIMATION_ID, Condition.WALK);
        }
        if (aimingProgress <= 0) {
            return playBaseAnimation(player, gunIndex, PlayerAnimatorCompat.BASE_ANIMATION_ID, Condition.HOLD);
        } else {
            return playBaseAnimation(player, gunIndex, PlayerAnimatorCompat.BASE_ANIMATION_ID, Condition.AIM);
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean playBaseAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, ResourceLocation dataId, Condition condition) {
        ResourceLocation animator3rd = gunIndex.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return false;
        }
        if (!PlayerAnimatorAssetManager.INSTANCE.containsKey(animator3rd)) {
            return false;
        }
        return PlayerAnimatorAssetManager.INSTANCE.getAnimations(animator3rd, condition).map(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer != null) {
                if (modifierLayer.getAnimation() instanceof KeyframeAnimationPlayer animationPlayer && animationPlayer.isActive()) {
                    if (animationPlayer.getData().extraData.get("name") instanceof String name && !condition.name().toLowerCase(Locale.ENGLISH).equals(name)) {
                        modifierLayer.setAnimation(new KeyframeAnimationPlayer(keyframeAnimation));
                    }
                    return true;
                }
                modifierLayer.setAnimation(new KeyframeAnimationPlayer(keyframeAnimation));
                return true;
            }
            return false;
        }).orElse(false);
    }

    @SuppressWarnings("unchecked")
    public static boolean playAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, ResourceLocation dataId, Condition condition) {
        ResourceLocation animator3rd = gunIndex.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return false;
        }
        if (!PlayerAnimatorAssetManager.INSTANCE.containsKey(animator3rd)) {
            return false;
        }
        return PlayerAnimatorAssetManager.INSTANCE.getAnimations(animator3rd, condition).map(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer != null) {
                IAnimation animation = modifierLayer.getAnimation();
                if (animation != null && animation.isActive()) {
                    return true;
                }
                modifierLayer.setAnimation(new KeyframeAnimationPlayer(keyframeAnimation));
                return true;
            }
            return false;
        }).orElse(false);
    }

    @SuppressWarnings("unchecked")
    public static void stopAnimation(AbstractClientPlayer player, ResourceLocation dataId) {
        var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
        if (modifierLayer != null && modifierLayer.isActive()) {
            modifierLayer.setAnimation(null);
        }
    }

    @SubscribeEvent
    public void onFire(GunShootEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(player);
            float aimingProgress = operator.getSynAimingProgress();
            if (aimingProgress <= 0) {
                playAnimation(player, index, PlayerAnimatorCompat.MAIN_ANIMATION_ID, Condition.NORMAL_FIRE);
            } else {
                playAnimation(player, index, PlayerAnimatorCompat.MAIN_ANIMATION_ID, Condition.AIM_FIRE);
            }
        });
    }

    @SubscribeEvent
    public void onReload(GunReloadEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getEntity();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> playAnimation(player, index, PlayerAnimatorCompat.MAIN_ANIMATION_ID, Condition.RELOAD));
    }

    @SubscribeEvent
    public void onMelee(GunMeleeEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (!(shooter instanceof AbstractClientPlayer player)) {
            return;
        }
        ItemStack gunItemStack = event.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItemStack);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> playAnimation(player, index, PlayerAnimatorCompat.MAIN_ANIMATION_ID, Condition.MELEE));
    }
}
