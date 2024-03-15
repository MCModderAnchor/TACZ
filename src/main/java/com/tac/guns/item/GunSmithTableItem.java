package com.tac.guns.item;

import com.tac.guns.client.renderer.item.GunSmithTableItemRenderer;
import com.tac.guns.init.ModBlocks;
import com.tac.guns.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.IItemRenderProperties;

import java.util.function.Consumer;

public class GunSmithTableItem extends BlockItem {
    public GunSmithTableItem() {
        super(ModBlocks.GUN_SMITH_TABLE.get(), (new Item.Properties()).stacksTo(1).tab(ModItems.OTHER_TAB));
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new GunSmithTableItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }
}
