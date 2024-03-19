package com.tac.guns.client.init;

import com.tac.guns.GunMod;
import com.tac.guns.client.particle.BulletHoleParticle;
import com.tac.guns.init.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleFactoryRegistry {
    @SubscribeEvent
    public static void onRegisterParticleFactory(ParticleFactoryRegisterEvent event) {
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
        particleEngine.register(ModParticles.BULLET_HOLE.get(), (typeIn, worldIn, x, y, z, xSpeed, ySpeed, zSpeed) -> new BulletHoleParticle(worldIn, x, y, z, typeIn.getDirection(), typeIn.getPos()));
    }
}