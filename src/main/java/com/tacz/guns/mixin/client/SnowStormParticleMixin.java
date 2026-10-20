package com.tacz.guns.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Match 1.21.1's front-facing particle winding on both the vanilla and optimized vertex paths. */
@Mixin(value = SnowStormParticle.class, remap = false)
public class SnowStormParticleMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", ordinal = 0), index = 0)
    private float tacz$frontFacingAxisX(float x) {
        return -x;
    }

    // Reversing the X axis changes winding; reverse U with it to preserve the image's orientation.
    @Redirect(method = "render", at = @At(value = "FIELD",
            target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v1/particle/runtime/ParticleInstance;u0:F"))
    private float tacz$rightU(ParticleInstance particle) {
        return particle.u1;
    }

    @Redirect(method = "render", at = @At(value = "FIELD",
            target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v1/particle/runtime/ParticleInstance;u1:F"))
    private float tacz$leftU(ParticleInstance particle) {
        return particle.u0;
    }
}
