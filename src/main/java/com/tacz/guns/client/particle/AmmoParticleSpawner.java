package com.tacz.guns.client.particle;

import com.mojang.math.Vector3f;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.entity.EntityBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class AmmoParticleSpawner {
    public static void addParticle(Level level, EntityBullet bullet) {
        TimelessAPI.getClientAmmoIndex(bullet.getAmmoId()).ifPresent(ammoIndex -> {
            AmmoParticle particle = ammoIndex.getParticle();
            if (particle == null) {
                return;
            }
            ParticleOptions particleOptions = particle.getParticleOptions();
            if (particleOptions == null) {
                return;
            }
            int count = particle.getCount();
            Vector3f delta = particle.getDelta();
            float particleSpeed = particle.getSpeed();
            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
            if (count == 0) {
                double xSpeed = particleSpeed * delta.x();
                double ySpeed = particleSpeed * delta.y();
                double zSpeed = particleSpeed * delta.z();
                Particle result = particleEngine.createParticle(particleOptions, bullet.getX(), bullet.getY(), bullet.getZ(), xSpeed, ySpeed, zSpeed);
                if (result != null) {
                    result.setLifetime(particle.getLifeTime());
                }
            } else {
                Random random = bullet.getRandom();
                for (int i = 0; i < count; ++i) {
                    Vec3 deltaMovement = bullet.getDeltaMovement();
                    double deltaMovementRandom = random.nextDouble();
                    double offsetX = random.nextGaussian() * delta.x() + deltaMovementRandom * deltaMovement.x;
                    double offsetY = random.nextGaussian() * delta.y() + deltaMovementRandom * deltaMovement.y;
                    double offsetZ = random.nextGaussian() * delta.z() + deltaMovementRandom * deltaMovement.z;
                    double xSpeed = random.nextGaussian() * particleSpeed;
                    double ySpeed = random.nextGaussian() * particleSpeed;
                    double zSpeed = random.nextGaussian() * particleSpeed;
                    Particle result = particleEngine.createParticle(particleOptions, bullet.getX() + offsetX, bullet.getY() + offsetY, bullet.getZ() + offsetZ, xSpeed, ySpeed, zSpeed);
                    if (result != null) {
                        result.setLifetime(particle.getLifeTime());
                    }
                }
            }
        });
    }
}
