package com.tacz.guns.command.sub;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class PegboardStressCommand {
    private static final int MAX_GUNS = 16_384;
    private static final int BOARDS_PER_ROW = 32;
    private static final int[] DISPLAY_SLOTS = {1, 3, 9, 11};
    private static final String COUNT = "count";
    private static final String POS = "pos";

    private PegboardStressCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var pegboard = Commands.literal("pegboard");
        var count = Commands.argument(COUNT, IntegerArgumentType.integer(1, MAX_GUNS));
        for (boolean same : new boolean[]{true, false}) {
            var mode = Commands.literal(same ? "same" : "different");
            mode.executes(context -> generate(context, same, false));
            mode.then(Commands.argument(POS, BlockPosArgument.blockPos())
                    .executes(context -> generate(context, same, true)));
            count.then(mode);
        }
        return pegboard.then(count);
    }

    private static int generate(CommandContext<CommandSourceStack> context, boolean same, boolean hasPos) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int requested = IntegerArgumentType.getInteger(context, COUNT);

        // The index sort is the order used by the creative gun list; the id makes ties deterministic.
        List<ItemStack> guns = TimelessAPI.getAllCommonGunIndex().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, CommonGunIndex> entry) -> entry.getValue().getSort())
                        .thenComparing(entry -> entry.getKey().toString()))
                .map(entry -> GunItemBuilder.create()
                        .setId(entry.getKey())
                        .setFireMode(entry.getValue().getGunData().getFireModeSet().get(0))
                        .build())
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (guns.isEmpty()) {
            source.sendFailure(Component.translatable("commands.tacz.debug.pegboard.no_guns"));
            return 0;
        }

        int boardCount = (requested + DISPLAY_SLOTS.length - 1) / DISPLAY_SLOTS.length;
        int columns = Math.min(BOARDS_PER_ROW, boardCount);
        Direction forward = Direction.fromYRot(source.getRotation().y);
        Direction right = forward.getClockWise();
        Direction facing = forward.getOpposite();
        BlockPos origin = hasPos
                ? BlockPosArgument.getBlockPos(context, POS)
                : BlockPos.containing(source.getPosition()).relative(forward, 4).above().relative(right, -(columns - 1) / 2);

        List<BlockPos> positions = new ArrayList<>(boardCount);
        for (int i = 0; i < boardCount; i++) {
            BlockPos pos = origin.relative(right, i % columns).above(i / columns);
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                    || !level.isLoaded(pos) || !level.getBlockState(pos).isAir()) {
                source.sendFailure(Component.translatable("commands.tacz.debug.pegboard.blocked", pos.toShortString()));
                return 0;
            }
            positions.add(pos);
        }

        BlockState state = ModBlocks.GUN_PEGBOARD.get().defaultBlockState().setValue(GunPegboardBlock.FACING, facing);
        int placed = 0;
        for (int board = 0; board < boardCount; board++) {
            BlockPos pos = positions.get(board);
            if (!level.setBlock(pos, state, Block.UPDATE_CLIENTS)
                    || !(level.getBlockEntity(pos) instanceof GunPegboardBlockEntity pegboard)) {
                source.sendFailure(Component.translatable("commands.tacz.debug.pegboard.failed", placed, pos.toShortString()));
                return placed;
            }
            int slots = Math.min(DISPLAY_SLOTS.length, requested - placed);
            Map<Integer, ItemStack> contents = new LinkedHashMap<>(slots);
            for (int slot = 0; slot < slots; slot++) {
                contents.put(DISPLAY_SLOTS[slot], guns.get(same ? 0 : (placed + slot) % guns.size()));
            }
            if (!pegboard.setItems(contents)) {
                source.sendFailure(Component.translatable("commands.tacz.debug.pegboard.failed", placed, pos.toShortString()));
                return placed;
            }
            placed += slots;
        }

        int distinct = same ? 1 : Math.min(requested, guns.size());
        int placedCount = placed;
        source.sendSuccess(() -> Component.translatable("commands.tacz.debug.pegboard.success",
                placedCount, boardCount, distinct, origin.toShortString()), false);
        return placed;
    }
}
