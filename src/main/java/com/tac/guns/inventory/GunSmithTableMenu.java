package com.tac.guns.inventory;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.crafting.GunSmithTableIngredient;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ServerMessageCraft;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.List;

public class GunSmithTableMenu extends AbstractContainerMenu {
    public static final MenuType<GunSmithTableMenu> TYPE = IForgeMenuType.create((windowId, inv, data) -> new GunSmithTableMenu(windowId, inv));

    public GunSmithTableMenu(int id, Inventory inventory) {
        super(TYPE, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    public void doCraft(ResourceLocation recipeId, Player player) {
        TimelessAPI.getRecipe(recipeId).ifPresent(recipe -> player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(handler -> {
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
            player.inventoryMenu.broadcastChanges();
            NetworkHandler.sendToClientPlayer(new ServerMessageCraft(this.containerId), player);
        }));
    }
}
