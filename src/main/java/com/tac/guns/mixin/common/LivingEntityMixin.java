package com.tac.guns.mixin.common;

import com.tac.guns.api.entity.IShooter;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements IShooter {
    @Unique
    private long tac$ShootTimestamp = -1L;

    @Unique
    private long tac$DrawTimestamp = -1L;

    @Unique
    private long tac$ReloadTimestamp = -1L;

    @Override
    @Unique
    public void recordShootTime() {
        this.tac$ShootTimestamp = System.currentTimeMillis();
    }

    @Override
    @Unique
    public long getShootTime() {
        return this.tac$ShootTimestamp;
    }

    @Override
    @Unique
    public void recordDrawTime() {
        this.tac$DrawTimestamp = System.currentTimeMillis();
    }

    @Override
    @Unique
    public long getDrawTime() {
        return this.tac$DrawTimestamp;
    }

    @Override
    public void recordReloadTime() {
        this.tac$ReloadTimestamp = System.currentTimeMillis();
    }

    @Override
    public long getReloadTime() {
        return this.tac$ReloadTimestamp;
    }
}
