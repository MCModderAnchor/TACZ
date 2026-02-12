package com.tacz.guns;

import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.compat.kubejs.TimelessKubeJSPlugin;
import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.ServerConfig;
import com.tacz.guns.init.*;
import com.tacz.guns.resource.GunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.ShootBus;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GunMod.MOD_ID)
public class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    /**
     * 默认模型包文件夹
     */
    public static final String DEFAULT_GUN_PACK_NAME = "tacz_default_gun";
    public GunMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.init());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.init());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());

        Dist side = FMLLoader.getDist();
        GunPackLoader.INSTANCE.packType = side.isClient() ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA;

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.TILE_ENTITIES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModRecipe.RECIPE_SERIALIZERS.register(bus);
        ModRecipe.RECIPE_TYPES.register(bus);
        ModContainer.CONTAINER_TYPE.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModParticles.PARTICLE_TYPES.register(bus);
        ModAttributes.ATTRIBUTES.register(bus);
        ModPainting.PAINTINGS.register(bus);
        if (ModList.get().isLoaded("kubejs")) {
            bus.register(new TimelessKubeJSPlugin());
        }

        registerDefaultExtraGunPack();
        AttachmentPropertyManager.registerModifier();
        ShootBus shootBus = new ShootBus();
    }

    private static void registerDefaultExtraGunPack() {
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        ResourceManager.registerExportResource(GunMod.class, jarDefaultPackPath);
    }
}
