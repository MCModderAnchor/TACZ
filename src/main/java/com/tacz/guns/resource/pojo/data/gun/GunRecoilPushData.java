package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Mth;

public class GunRecoilPushData {
    private static final double MAX_RECOIL_PUSH_STRENGTH = 4.0D;

    @SerializedName("ground_strength")
    private double groundStrength = 0.0D;

    @SerializedName("air_strength")
    private double airStrength = 0.0D;

    @SerializedName("per_projectile")
    private boolean perProjectile = false;

    @SerializedName("projectile_multiplier")
    private double projectileMultiplier = 1.0D;

    public GunRecoilPushData() {
    }

    public GunRecoilPushData(double groundStrength, double airStrength, boolean perProjectile, double projectileMultiplier) {
        this.groundStrength = groundStrength;
        this.airStrength = airStrength;
        this.perProjectile = perProjectile;
        this.projectileMultiplier = projectileMultiplier;
    }

    public double getGroundStrength() {
        return clampStrength(groundStrength);
    }

    public double getAirStrength() {
        return clampStrength(airStrength);
    }

    public boolean isPerProjectile() {
        return perProjectile;
    }

    public double getProjectileMultiplier() {
        return Math.max(projectileMultiplier, 0.0D);
    }

    public double getFinalStrength(boolean onGround, int bulletAmount) {
        double strength = onGround ? getGroundStrength() : getAirStrength();
        if (perProjectile) {
            strength *= Math.max(bulletAmount, 1) * getProjectileMultiplier();
        }
        return clampStrength(strength);
    }

    private double clampStrength(double strength) {
        return Mth.clamp(strength, 0.0D, MAX_RECOIL_PUSH_STRENGTH);
    }
}
