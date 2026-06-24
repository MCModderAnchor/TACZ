package com.tacz.guns.item;

import com.tacz.guns.inventory.tooltip.AmmoStorageTooltip;
import com.tacz.guns.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AmmoStorageBlockItem extends BlockItem {
    private static final int MAX_TOOLTIP_ITEMS = 5;
    private static final String ENTRIES_TAG = "Entries";
    private static final String STACK_TAG = "Stack";
    private static final String COUNT_TAG = "Count";

    public AmmoStorageBlockItem(Block block) {
        super(block, new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        if (getBlock() == ModBlocks.AMMO_STORAGE_GOLD.get()) {
            return name.copy().withStyle(ChatFormatting.YELLOW);
        }
        if (getBlock() == ModBlocks.AMMO_STORAGE_DIAMOND.get()) {
            return name.copy().withStyle(ChatFormatting.AQUA);
        }
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, level, components, flag);
        components.add(Component.translatable("tooltip.tacz.ammo_storage.description").withStyle(ChatFormatting.GRAY));
        components.add(Component.translatable("tooltip.tacz.ammo_storage.description.extra").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        CompoundTag blockEntityTag = BlockItem.getBlockEntityData(stack);
        if (blockEntityTag == null || !blockEntityTag.contains(ENTRIES_TAG, Tag.TAG_LIST)) {
            return Optional.empty();
        }
        ListTag entries = blockEntityTag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        List<AmmoStorageTooltip.Entry> mergedEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            if (!entryTag.contains(STACK_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack entryStack = ItemStack.of(entryTag.getCompound(STACK_TAG));
            int count = entryTag.getInt(COUNT_TAG);
            if (entryStack.isEmpty() || count <= 0) {
                continue;
            }
            mergeTooltipEntry(mergedEntries, entryStack, count);
        }
        if (mergedEntries.isEmpty()) {
            return Optional.empty();
        }
        int displayed = Math.min(MAX_TOOLTIP_ITEMS, mergedEntries.size());
        List<AmmoStorageTooltip.Entry> tooltipEntries = new ArrayList<>(mergedEntries.subList(0, displayed));
        return Optional.of(new AmmoStorageTooltip(tooltipEntries, mergedEntries.size() - displayed));
    }

    private static void mergeTooltipEntry(List<AmmoStorageTooltip.Entry> entries, ItemStack stack, int count) {
        for (int i = 0; i < entries.size(); i++) {
            AmmoStorageTooltip.Entry entry = entries.get(i);
            if (ItemStack.isSameItemSameTags(entry.ammo(), stack)) {
                int mergedCount = saturatedAdd(entry.count(), count);
                entries.set(i, new AmmoStorageTooltip.Entry(entry.ammo(), mergedCount));
                return;
            }
        }
        ItemStack sample = stack.copy();
        sample.setCount(1);
        entries.add(new AmmoStorageTooltip.Entry(sample, count));
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}
