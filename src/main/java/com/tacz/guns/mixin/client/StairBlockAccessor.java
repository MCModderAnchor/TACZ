package com.tacz.guns.mixin.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StairBlock.class)
public interface StairBlockAccessor {
    /**
     * 用来修正 forge 提供的 getModelBlock 方法访问不正确的问题
     */
    @Invoker(remap = false)
    Block invokeGetModelBlock();
}
