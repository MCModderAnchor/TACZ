package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.compat.playeranimator.AnimationName;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AnimationManager {
    public static boolean hasPlayerAnimator3rd(ClientGunIndex gunIndex) {
        ResourceLocation location = gunIndex.getPlayerAnimator3rd();
        if (location == null) {
            return false;
        }
        return PlayerAnimatorAssetManager.INSTANCE.containsKey(location);
    }

    public static boolean isFlying(AbstractClientPlayer player) {
        return !player.onGround() && player.getAbilities().flying;
    }

    public static void playRotationAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex) {
        String animationName = AnimationName.EMPTY;
        ResourceLocation dataId = PlayerAnimatorCompat.ROTATION_ANIMATION;
        ResourceLocation animator3rd = gunIndex.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.INSTANCE.containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.INSTANCE.getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
        });
    }

    public static void playLowerAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, float limbSwingAmount) {
        // 如果玩家趴下，不播放下半身动画
        if (isPlayerLie(player)) {
            return;
        }
        // 如果玩家骑乘
        if (player.getVehicle() != null) {
            playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.RIDE_LOWER);
            return;
        }
        // 如果玩家在天上，下半身动画就是站立动画
        if (isFlying(player)) {
            playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.HOLD_LOWER);
            return;
        }
        if (player.isSprinting()) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.RUN_LOWER);
            }
            return;
        }
        if (limbSwingAmount > 0.05) {
            if (player.getPose() == Pose.CROUCHING) {
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_WALK_LOWER);
            } else {
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.WALK_LOWER);
            }
            return;
        }
        if (player.getPose() == Pose.CROUCHING) {
            playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.CROUCH_LOWER);
        } else {
            playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOWER_ANIMATION, AnimationName.HOLD_LOWER);
        }
    }

    public static void playLoopUpperAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, float limbSwingAmount) {
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        float aimingProgress = operator.getSynAimingProgress();
        if (aimingProgress <= 0) {
            // 疾跑时播放的动画
            if (!isFlying(player) && player.isSprinting()) {
                if (isPlayerLie(player)) {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_MOVE);
                } else if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.RUN_UPPER);
                }
                return;
            }

            // 行走时的动画
            if (!isFlying(player) && limbSwingAmount > 0.05) {
                if (isPlayerLie(player)) {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_MOVE);
                } else if (player.getPose() == Pose.CROUCHING) {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.CROUCH_WALK_UPPER);
                } else {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.WALK_UPPER);
                }
                return;
            }

            if (isPlayerLie(player)) {
                // 趴下时的动画
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE);
            } else {
                // 普通待命
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.HOLD_UPPER);
            }
        } else {
            if (isPlayerLie(player)) {
                // 趴下时瞄准
                if (!isFlying(player) && limbSwingAmount > 0.05) {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_MOVE);
                } else {
                    playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.LIE_AIM);
                }
            } else {
                // 普通瞄准
                playLoopAnimation(player, gunIndex, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, AnimationName.AIM_UPPER);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void playLoopAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, ResourceLocation dataId, String animationName) {
        ResourceLocation animator3rd = gunIndex.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.INSTANCE.containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.INSTANCE.getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            if (modifierLayer.getAnimation() instanceof KeyframeAnimationPlayer animationPlayer && animationPlayer.isActive()) {
                Object extraDataName = animationPlayer.getData().extraData.get("name");
                if (extraDataName instanceof String name && !animationName.equals(name)) {
                    AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
                    modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
                }
                return;
            }
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
        });
    }

    @SuppressWarnings("unchecked")
    public static void playOnceAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, ResourceLocation dataId, String animationName) {
        ResourceLocation animator3rd = gunIndex.getPlayerAnimator3rd();
        if (animator3rd == null) {
            return;
        }
        if (!PlayerAnimatorAssetManager.INSTANCE.containsKey(animator3rd)) {
            return;
        }
        PlayerAnimatorAssetManager.INSTANCE.getAnimations(animator3rd, animationName).ifPresent(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
            if (modifierLayer == null) {
                return;
            }
            IAnimation animation = modifierLayer.getAnimation();
            if (animation == null || !animation.isActive()) {
                AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
                modifierLayer.replaceAnimationWithFade(fadeModifier, new KeyframeAnimationPlayer(keyframeAnimation));
            }
        });
    }

    public static void stopAllAnimation(AbstractClientPlayer player) {
        stopAnimation(player, PlayerAnimatorCompat.LOWER_ANIMATION);
        stopAnimation(player, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION);
        stopAnimation(player, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
        stopAnimation(player, PlayerAnimatorCompat.ROTATION_ANIMATION);
    }

    @SuppressWarnings("unchecked")
    private static void stopAnimation(AbstractClientPlayer player, ResourceLocation dataId) {
        var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(dataId);
        if (modifierLayer != null && modifierLayer.isActive()) {
            AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTSINE);
            modifierLayer.replaceAnimationWithFade(fadeModifier, null);
        }
    }

    private static boolean isPlayerLie(AbstractClientPlayer player) {
        // MOJANG 的奇妙设计，趴下的姿势名称是 SWIMMING
        return !player.isSwimming() && player.getPose() == Pose.SWIMMING;
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
                if (isPlayerLie(player)) {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.LIE_NORMAL_FIRE);
                } else {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.NORMAL_FIRE_UPPER);
                }
            } else {
                if (isPlayerLie(player)) {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.LIE_AIM_FIRE);
                } else {
                    playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.AIM_FIRE_UPPER);
                }
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
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> {
            if (isPlayerLie(player)) {
                playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.LIE_RELOAD);
            } else {
                playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, AnimationName.RELOAD_UPPER);
            }
        });
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
        int randomIndex = shooter.getRandom().nextInt(3);
        String animationName = switch (randomIndex) {
            case 0 -> AnimationName.MELEE_UPPER;
            case 1 -> AnimationName.MELEE_2_UPPER;
            default -> AnimationName.MELEE_3_UPPER;
        };
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> playOnceAnimation(player, index, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, animationName));
    }

    @SubscribeEvent
    public void onDraw(GunDrawEvent event) {
        if (event.getLogicalSide().isServer()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        ItemStack currentGunItem = event.getCurrentGunItem();
        ItemStack previousGunItem = event.getPreviousGunItem();
        // 在切枪时，重置上半身动画
        if (currentGunItem.getItem() instanceof IGun && previousGunItem.getItem() instanceof IGun) {
            stopAnimation(player, PlayerAnimatorCompat.LOOP_UPPER_ANIMATION);
            stopAnimation(player, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
        }
    }
}
