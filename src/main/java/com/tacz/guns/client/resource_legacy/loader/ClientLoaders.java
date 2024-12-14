package com.tacz.guns.client.resource_legacy.loader;

import com.tacz.guns.client.resource_legacy.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;

public class ClientLoaders {
    public static final DisplayLoader<BlockDisplay> BLOCK_DISPLAY = new DisplayLoader<>(
            BlockDisplay.class,
            "BlockDisplayLoader",
            "blocks/display",
            ClientAssetManager.INSTANCE::putBlockDisplay);
}
