package com.tacz.guns.item;

import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.block.GunPegboardBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GunPegboardItem extends BlockItem {
    public static boolean hasCustomColor(ItemStack stack) { return stack.has(net.minecraft.core.component.DataComponents.DYED_COLOR); }
    public static int getColor(ItemStack stack) { return stack.getOrDefault(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(-1, false)).rgb(); }
    public static void setColor(ItemStack stack, int color) { stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(color, false)); }

    public GunPegboardItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        int color = hasCustomColor(stack) ? getColor(stack) : -1;
        boolean inheritedColor = false;
        BlockPos placedPos = context.getClickedPos();
        BlockPos targetPos = placedPos.relative(context.getClickedFace().getOpposite());
        boolean linking = !context.replacingClickedOnBlock()
                && context.getPlayer() != null && !context.getPlayer().isShiftKeyDown()
                && context.getLevel().getBlockState(targetPos).getBlock() instanceof GunPegboardBlock;
        if (color < 0 && linking && context.getLevel().getBlockEntity(targetPos) instanceof GunPegboardBlockEntity target) {
            color = target.getColor();
            inheritedColor = color >= 0;
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && context.getLevel().getBlockEntity(placedPos) instanceof GunPegboardBlockEntity pegboard
                && pegboard.getBlockState().is(getBlock())) {
            pegboard.setColor(color, inheritedColor);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.tacz.gun_pegboard.dye").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tacz.gun_pegboard.link_color").withStyle(ChatFormatting.GRAY));
    }
}
