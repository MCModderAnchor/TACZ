package com.tacz.guns.util;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

/**
 * 该方法主要用于检查方块实体有没有 Override 右键交互，借此作为能否右键交互的判断
 */
public class InheritanceChecker<T> {
    public static final InheritanceChecker<Block> BLOCK_INHERITANCE_CHECKER = new InheritanceChecker<>(
            Block.class,
            ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_6227_"),
            BlockState.class, Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class
    );

    public static final InheritanceChecker<Entity> ENTITY_INHERITANCE_CHECKER = new InheritanceChecker<>(
            Entity.class,
            ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_6096_"),
            Player.class, InteractionHand.class
    );

    public static final InheritanceChecker<Mob> MOB_INHERITANCE_CHECKER = new InheritanceChecker<>(
            Mob.class,
            ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_6071_"),
            Player.class, InteractionHand.class
    );

    private final Class<T> givenBaseType;
    private final String methodName;
    private final Class<?>[] paramTypes;
    private final Class<? super T> realBaseType;
    private final ClassValue<Boolean> cacheClassValue = new CacheClassValue();

    public InheritanceChecker(Class<T> baseType, String methodName, Class<?>... paramTypes) {
        this.givenBaseType = baseType;
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.realBaseType = getDeclareClass(baseType, methodName, paramTypes);
    }

    private static Method getDeclaredMethod(Class<?> type, String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method;
        try {
            method = type.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ignored) {
            method = type.getDeclaredMethod(methodName, paramTypes);
            if (!method.trySetAccessible()) {
                throw new IllegalArgumentException("Inaccessible Method");
            }
        }
        return method;
    }

    public Class<T> getBaseType() {
        return givenBaseType;
    }

    public boolean isInherited(Class<? extends T> typeToCheck) {
        return cacheClassValue.get(typeToCheck);
    }

    @SuppressWarnings("unchecked")
    private Class<? super T> getDeclareClass(Class<T> type, String methodName, Class<?>[] paramTypes) {
        Method method;
        try {
            method = getDeclaredMethod(type, methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            // 当 MobEntity.mobInteract(...) 是一个 protected 方法时抛出 NoSuchMethodException 错误，意味着这个方法并没有被 Override
            method = getDeclaredMethodFromSuperClass(type, methodName, paramTypes);
        }
        return (Class<? super T>) method.getDeclaringClass();
    }

    /**
     * 遍历超类以检查 Override 情况
     */
    private Method getDeclaredMethodFromSuperClass(Class<?> type, String methodName, Class<?>... paramTypes) {
        Class<?> temp = type.getSuperclass();
        while (temp != this.realBaseType && temp != Object.class && temp != null) {
            Method method;
            try {
                method = temp.getMethod(methodName, paramTypes);
                return method;
            } catch (NoSuchMethodException ignored) {
                try {
                    method = temp.getDeclaredMethod(methodName, paramTypes);
                    if (!method.trySetAccessible()) {
                        throw new IllegalArgumentException("Inaccessible Method");
                    }
                    return method;
                } catch (NoSuchMethodException pass) {
                    temp = temp.getSuperclass();
                }
            }
        }
        try {
            return realBaseType.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            // 一般来说不太可能触发这个
            throw new RuntimeException(e);
        }
    }

    public class CacheClassValue extends ClassValue<Boolean> {
        @Override
        @SuppressWarnings("unchecked")
        protected Boolean computeValue(Class<?> type) {
            return !realBaseType.equals(getDeclareClass((Class<T>) type, methodName, paramTypes));
        }
    }
}
