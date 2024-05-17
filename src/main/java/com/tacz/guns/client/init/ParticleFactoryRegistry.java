package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleFactoryRegistry {
    @SubscribeEvent
    public static void onRegisterParticleFactory(RegisterEvent event) {
        //todo 没看懂这个，过会再看
//        if(event.getRegistryKey().equals(ForgeRegistries.Keys.PARTICLE_TYPES)){
//            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
//            particleEngine.register(ModParticles.BULLET_HOLE.get(), (typeIn, worldIn, x, y, z, xSpeed, ySpeed, zSpeed) -> new BulletHoleParticle(worldIn, x, y, z, typeIn.getDirection(), typeIn.getPos()));
//
//        }

    }
}