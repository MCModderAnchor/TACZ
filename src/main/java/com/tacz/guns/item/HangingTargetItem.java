package com.tacz.guns.item; // 如果你的包名不一样请改回来

import com.tacz.guns.entity.HangingTargetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class HangingTargetItem extends Item {

    public HangingTargetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // 限制只能在服务端处理放置逻辑
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 1. ✨ 核心限制：获取玩家右键点击的方块面
        Direction clickedFace = context.getClickedFace();

        // 如果点击的不是方块的【底部】（也就是天花板、横梁底下），直接拒绝放置并提示玩家
        if (clickedFace != Direction.DOWN) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("§c该标靶只能悬挂在方块底部！"), true);
            }
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        ItemStack itemStack = context.getItemInHand();

        // 2. 计算召唤实体的精准位置
        // 既然我们把物理锚点移到了头顶，那么实体的 XZ 应该在方块中心，Y 应该正好在被点方块的下方边缘
        double spawnX = clickedPos.getX() + 0.5D;
        double spawnY = clickedPos.getY()-1.5D; // 恰好是方块底部的 Y 坐标
        double spawnZ = clickedPos.getZ() + 0.5D;

        // 3. 召唤我们的吊靶实体
        if (level instanceof ServerLevel serverLevel) {
            HangingTargetEntity target = HangingTargetEntity.TYPE.create(serverLevel);
            if (target != null) {
                // 设置位置并摆正
                target.moveTo(spawnX, spawnY, spawnZ, 0.0F, 0.0F);
                serverLevel.addWithUUID(target);

                // 扣除玩家手里的物品（非创造模式下）
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    itemStack.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.FAIL;
    }
}