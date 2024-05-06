package com.tacz.guns.api.event.server;

import com.tacz.guns.entity.EntityBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.Event;

/**
 * 子弹击中方块时触发的事件，目前仅在服务端触发
 */
public class AmmoHitBlockEvent extends Event {
    private final Level level;
    private final BlockHitResult hitResult;
    private final BlockState state;
    private final EntityBullet ammo;

    public AmmoHitBlockEvent(Level level, BlockHitResult hitResult, BlockState state, EntityBullet ammo) {
        this.level = level;
        this.hitResult = hitResult;
        this.state = state;
        this.ammo = ammo;
    }

    public Level getLevel() {
        return level;
    }

    public BlockHitResult getHitResult() {
        return hitResult;
    }

    public BlockState getState() {
        return state;
    }

    public EntityBullet getAmmo() {
        return ammo;
    }
}
