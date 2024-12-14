package com.tacz.guns.inventory;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonBlockIndex;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public class GunSmithTableMenu extends AbstractContainerMenu {
    public static final MenuType<GunSmithTableMenu> TYPE = IForgeMenuType.create((windowId, inv, data) -> {
        ResourceLocation blockId = data.readResourceLocation();
        return new GunSmithTableMenu(windowId, inv, blockId);
    });

    private final ResourceLocation blockId;
    private final RecipeFilter filter;

    public GunSmithTableMenu(int id, Inventory inventory, @Nullable ResourceLocation resourceLocation) {
        super(TYPE, id);
        this.blockId = resourceLocation;
        this.filter = TimelessAPI.getCommonBlockIndex(getBlockId()).map(CommonBlockIndex::getFilter).orElse(null);
    }

    @Nullable
    public ResourceLocation getBlockId() {
        return blockId;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Nullable
    private GunSmithTableRecipe getRecipe(ResourceLocation recipeId, RecipeManager recipeManager) {
        if (filter != null && !filter.contains(recipeId)) {
            return null;
        }
        Recipe<?> recipe = recipeManager.byKey(recipeId).orElse(null);
        if (recipe instanceof GunSmithTableRecipe) {
            return (GunSmithTableRecipe) recipe;
        }
        return null;
    }

    public void doCraft(ResourceLocation recipeId, Player player) {
        GunSmithTableRecipe recipe = getRecipe(recipeId, player.level.getRecipeManager());
        if (recipe == null) {
            return;
        }
        player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(handler -> {
            Int2IntArrayMap recordCount = new Int2IntArrayMap();
            List<GunSmithTableIngredient> ingredients = recipe.getInputs();

            for (GunSmithTableIngredient ingredient : ingredients) {
                int count = 0;
                for (int slotIndex = 0; slotIndex < handler.getSlots(); slotIndex++) {
                    ItemStack stack = handler.getStackInSlot(slotIndex);
                    int stackCount = stack.getCount();
                    if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                        count = count + stackCount;
                        // 记录扣除的 slot 和数量
                        if (count <= ingredient.getCount()) {
                            // 如果数量不足，全扣
                            recordCount.put(slotIndex, stackCount);
                        } else {
                            //  数量够了，只扣需要的数量
                            int remaining = count - ingredient.getCount();
                            recordCount.put(slotIndex, stackCount - remaining);
                            break;
                        }
                    }
                }
                // 数量不够，不执行后续逻辑，合成失败
                if (count < ingredient.getCount()) {
                    return;
                }
            }

            // 开始扣材料
            for (int slotIndex : recordCount.keySet()) {
                handler.extractItem(slotIndex, recordCount.get(slotIndex), false);
            }

            // 给玩家对应的物品
            Level level = player.level;
            if (!level.isClientSide) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), recipe.getResultItem().copy());
                itemEntity.setPickUpDelay(0);
                level.addFreshEntity(itemEntity);
            }
            // 更新，否则客户端显示不正确
            player.inventoryMenu.broadcastFullState();
            NetworkHandler.sendToClientPlayer(new ServerMessageCraft(this.containerId), player);
        });
    }
}
