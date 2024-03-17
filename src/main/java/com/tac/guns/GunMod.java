package com.tac.guns;

import com.tac.guns.config.GeneralConfig;
import com.tac.guns.init.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GunMod.MOD_ID)
public class GunMod {
    public static final String MOD_ID = "tac";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public GunMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.TILE_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModEntities.DATA_SERIALIZERS.register(bus);
        ModRecipe.RECIPE_SERIALIZERS.register(bus);
        ModContainer.CONTAINER_TYPE.register(bus);
        ModSounds.SOUNDS.register(bus);
    }
}
