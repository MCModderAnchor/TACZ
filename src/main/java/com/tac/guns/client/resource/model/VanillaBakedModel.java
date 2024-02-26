package com.tac.guns.client.resource.model;

import com.tac.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VanillaBakedModel implements CacheableModel {
    public static final VanillaBakedModel MISSING_MODEL = new VanillaBakedModel(null) {
        @Override
        public BakedModel getModel() {
            return Minecraft.getInstance().getModelManager().getMissingModel();
        }

        @Override
        public void cleanCache() {
        }
    };
    public final ResourceLocation modelLocation;
    private BakedModel cachedModel;

    public VanillaBakedModel(ResourceLocation location) {
        this.modelLocation = location;
    }

    @OnlyIn(Dist.CLIENT)
    public BakedModel getModel() {
        if (this.cachedModel == null) {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(this.modelLocation);
            if (model == Minecraft.getInstance().getModelManager().getMissingModel())
                return model;
            this.cachedModel = model;
        }
        return this.cachedModel;
    }

    @Override
    public void cleanCache() {
        this.cachedModel = null;
    }
}
