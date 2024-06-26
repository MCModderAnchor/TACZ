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

public class AnimationManager {
    public static boolean onHoldOrAim(AbstractClientPlayer player, ClientGunIndex gunIndex) {
        if (otherAnimationIsPlaying(player)) {
            return true;
        }
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        float aimingProgress = operator.getSynAimingProgress();
        if (aimingProgress <= 0) {
            return playAnimation(player, gunIndex, Condition.HOLD);
        } else {
            return playAnimation(player, gunIndex, Condition.AIM);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean otherAnimationIsPlaying(AbstractClientPlayer player) {
        var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(PlayerAnimatorCompat.ANIMATION_DATA_ID);
        if (modifierLayer == null) {
            return false;
        }
        IAnimation animation = modifierLayer.getAnimation();
        if (animation instanceof KeyframeAnimationPlayer keyframe && keyframe.isActive()) {
            String name = (String) keyframe.getData().extraData.get("name");
            return !Condition.HOLD.isType(name) && !Condition.AIM.isType(name);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean playAnimation(AbstractClientPlayer player, ClientGunIndex gunIndex, Condition condition) {
        ResourceLocation id = gunIndex.getPlayerAnimator3rd();
        if (id == null) {
            return false;
        }
        if (!PlayerAnimatorAssetManager.INSTANCE.containsKey(id)) {
            return false;
        }
        return PlayerAnimatorAssetManager.INSTANCE.getAnimations(id, condition).map(keyframeAnimation -> {
            var associatedData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var modifierLayer = (ModifierLayer<IAnimation>) associatedData.get(PlayerAnimatorCompat.ANIMATION_DATA_ID);
            if (modifierLayer != null) {
                modifierLayer.setAnimation(new KeyframeAnimationPlayer(keyframeAnimation));
                return true;
            }
            return false;
        }).orElse(false);
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
                playAnimation(player, index, Condition.NORMAL_FIRE);
            } else {
                playAnimation(player, index, Condition.AIM_FIRE);
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
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> playAnimation(player, index, Condition.RELOAD));
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
        TimelessAPI.getClientGunIndex(iGun.getGunId(gunItemStack)).ifPresent(index -> playAnimation(player, index, Condition.MELEE));
    }
}
