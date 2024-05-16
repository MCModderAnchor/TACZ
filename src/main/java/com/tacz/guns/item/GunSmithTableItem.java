package com.tacz.guns.item;

import com.tacz.guns.client.renderer.item.GunSmithTableItemRenderer;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class GunSmithTableItem extends BlockItem {
    public GunSmithTableItem() {
        super(ModBlocks.GUN_SMITH_TABLE.get(), (new Item.Properties()).stacksTo(1).tab(ModItems.OTHER_TAB));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new GunSmithTableItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }
}
