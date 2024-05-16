package com.tacz.guns.init;

import com.mojang.serialization.Codec;
import com.tacz.guns.GunMod;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GunMod.MOD_ID);

    public static final RegistryObject<ParticleType<BulletHoleOption>> BULLET_HOLE = PARTICLE_TYPES.register("bullet_hole", () -> new ModParticleType<>(false, BulletHoleOption.DESERIALIZER, BulletHoleOption.CODEC));

    @SuppressWarnings("deprecation")
    private static class ModParticleType<T extends ParticleOptions> extends ParticleType<T> {
        private final Codec<T> codec;

        public ModParticleType(boolean overrideLimiter, ParticleOptions.Deserializer<T> deserializer, Codec<T> codec) {
            super(overrideLimiter, deserializer);
            this.codec = codec;
        }

        @Override
        public @NotNull Codec<T> codec() {
            return this.codec;
        }
    }
}
