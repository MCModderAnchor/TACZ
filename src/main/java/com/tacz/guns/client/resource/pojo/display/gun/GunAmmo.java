package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.client.resource.pojo.display.ammo.HitSurfaceParticle;

import javax.annotation.Nullable;
import java.util.List;

public class GunAmmo {
    @Nullable
    @SerializedName("tracer_color")
    private String tracerColor = null;

    @Nullable
    @SerializedName("particle")
    private AmmoParticle particle = null;

    @Nullable
    @SerializedName("hit_surface_particles")
    private List<HitSurfaceParticle> hitSurfaceParticles = null;

    @Nullable
    @SerializedName("enable_bullet_hole")
    private Boolean bulletHoleEnabled = null;

    @Nullable
    public String getTracerColor() {
        return tracerColor;
    }

    @Nullable
    public AmmoParticle getParticle() {
        return particle;
    }

    @Nullable
    public List<HitSurfaceParticle> getHitSurfaceParticles() {
        return hitSurfaceParticles;
    }

    @Nullable
    public Boolean getBulletHoleEnabled() {
        return bulletHoleEnabled;
    }
}
