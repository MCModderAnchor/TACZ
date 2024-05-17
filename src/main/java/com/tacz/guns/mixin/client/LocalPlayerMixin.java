package com.tacz.guns.mixin.client;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("ALL")
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements IClientPlayerGunOperator {
    private final @Unique LocalPlayer tac$player = (LocalPlayer) (Object) this;
    private final @Unique LocalPlayerDataHolder tac$data = new LocalPlayerDataHolder(tac$player);
    private final @Unique LocalPlayerAim tac$aim = new LocalPlayerAim(tac$data, tac$player);
    private final @Unique LocalPlayerBolt tac$bolt = new LocalPlayerBolt(tac$data, tac$player);
    private final @Unique LocalPlayerDraw tac$draw = new LocalPlayerDraw(tac$data, tac$player);
    private final @Unique LocalPlayerFireSelect tac$fireSelect = new LocalPlayerFireSelect(tac$data, tac$player);
    private final @Unique LocalPlayerInspect tac$inspect = new LocalPlayerInspect(tac$data, tac$player);
    private final @Unique LocalPlayerReload tac$reload = new LocalPlayerReload(tac$data, tac$player);
    private final @Unique LocalPlayerShoot tac$shoot = new LocalPlayerShoot(tac$data, tac$player);

    @Unique
    @Override
    public ShootResult shoot() {
        return tac$shoot.shoot();
    }

    @Unique
    @Override
    public void draw(ItemStack lastItem) {
        tac$draw.draw(lastItem);
    }

    @Unique
    @Override
    public void bolt() {
        tac$bolt.bolt();
    }

    @Unique
    @Override
    public void reload() {
        tac$reload.reload();
    }

    @Unique
    @Override
    public void inspect() {
        tac$inspect.inspect();
    }

    @Override
    public void fireSelect() {
        tac$fireSelect.fireSelect();
    }

    @Override
    public void aim(boolean isAim) {
        tac$aim.aim(isAim);
    }

    @Unique
    @Override
    public float getClientAimingProgress(float partialTicks) {
        return tac$aim.getClientAimingProgress(partialTicks);
    }

    @Unique
    @Override
    public long getClientShootCoolDown() {
        return tac$shoot.getClientShootCoolDown();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickClientSide(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.level().isClientSide()) {
            tac$aim.tickAimingProgress();
            tac$data.tickStateLock();
            tac$bolt.tickAutoBolt();
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V"))
    public void cancelSprint(LocalPlayer player, boolean sprinting) {
        tac$aim.cancelSprint(player, sprinting);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    public void onRespawn(CallbackInfo ci) {
        tac$data.reset();
        draw(ItemStack.EMPTY);
    }

    @Override
    public boolean isAim() {
        return tac$aim.isAim();
    }
}
