package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Mth;

public class GunRecoilPushData {
    private static final double MAX_RECOIL_PUSH_STRENGTH = 4.0D;

    @SerializedName("ground_strength")
    private double groundStrength = 0.0D;

    @SerializedName("air_strength")
    private double airStrength = 0.0D;

    @SerializedName("sneak_strength")
    private Double sneakStrength = null;

    @SerializedName("crawl_strength")
    private Double crawlStrength = null;

    @SerializedName("per_projectile")
    private boolean perProjectile = false;

    @SerializedName("projectile_multiplier")
    private double projectileMultiplier = 1.0D;

    public GunRecoilPushData() {
    }

    public GunRecoilPushData(double groundStrength, double airStrength, boolean perProjectile, double projectileMultiplier) {
        this(groundStrength, airStrength, null, null, perProjectile, projectileMultiplier);
    }

    public GunRecoilPushData(double groundStrength, double airStrength, Double sneakStrength, Double crawlStrength, boolean perProjectile, double projectileMultiplier) {
        this.groundStrength = groundStrength;
        this.airStrength = airStrength;
        this.sneakStrength = sneakStrength;
        this.crawlStrength = crawlStrength;
        this.perProjectile = perProjectile;
        this.projectileMultiplier = projectileMultiplier;
    }

    public double getGroundStrength() {
        return clampStrength(groundStrength);
    }

    public double getAirStrength() {
        return clampStrength(airStrength);
    }

    public double getSneakStrength() {
        return clampStrength(sneakStrength != null ? sneakStrength : groundStrength);
    }

    public double getCrawlStrength() {
        return clampStrength(crawlStrength != null ? crawlStrength : groundStrength);
    }

    public boolean isPerProjectile() {
        return perProjectile;
    }

    public double getProjectileMultiplier() {
        return Math.max(projectileMultiplier, 0.0D);
    }

    public double getFinalStrength(boolean onGround, int bulletAmount) {
        return getFinalStrength(onGround, false, false, bulletAmount);
    }

    public double getFinalStrength(boolean onGround, boolean sneaking, boolean crawling, int bulletAmount) {
        double strength;
        if (!onGround) {
            strength = getAirStrength();
        } else if (crawling) {
            strength = getCrawlStrength();
        } else if (sneaking) {
            strength = getSneakStrength();
        } else {
            strength = getGroundStrength();
        }
        if (perProjectile) {
            strength *= Math.max(bulletAmount, 1) * getProjectileMultiplier();
        }
        return clampStrength(strength);
    }

    private double clampStrength(double strength) {
        return Mth.clamp(strength, 0.0D, MAX_RECOIL_PUSH_STRENGTH);
    }
}
