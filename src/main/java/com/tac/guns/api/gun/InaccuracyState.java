package com.tac.guns.api.gun;

import com.tac.guns.api.entity.IGunOperator;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class InaccuracyState {
    /**
     * 获取当前的不准确度状态
     *
     * @param livingEntity 射手
     * @return 不准度情况
     */
    public static StateType getInaccuracyState(LivingEntity livingEntity) {
        float aimingProgress = IGunOperator.fromLivingEntity(livingEntity).getSynAimingProgress();
        // 瞄准优先级最高
        if (aimingProgress == 1.0f) {
            return StateType.AIM;
        }
        // MOJANG 的奇妙设计，趴下的姿势名称是 SWIMMING
        if (!livingEntity.isSwimming() && livingEntity.getPose() == Pose.SWIMMING) {
            return StateType.LIE;
        }
        if (livingEntity.getPose() == Pose.CROUCHING) {
            return StateType.SNEAK;
        }
        if (isMove(livingEntity)) {
            return StateType.MOVE;
        }
        return StateType.STAND;
    }

    private static boolean isMove(LivingEntity livingEntity) {
        double distance = Math.abs(livingEntity.walkDist - livingEntity.walkDistO);
        // FIXME 该判断并不总是起效，有很大的问题
        livingEntity.sendMessage(new TextComponent(String.valueOf(distance)), Util.NIL_UUID);
        return distance > 0.05f;
    }

    public enum StateType {
        /**
         * 站立不动
         */
        STAND("stand"),
        /**
         * 移动
         */
        MOVE("move"),
        /**
         * 潜行，认为是其他 FPS 游戏中的半蹲
         */
        SNEAK("sneak"),
        /**
         * 趴下，原版确实可以趴下
         */
        LIE("lie"),
        /**
         * 瞄准状态
         */
        AIM("aim");

        private final String name;

        StateType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
