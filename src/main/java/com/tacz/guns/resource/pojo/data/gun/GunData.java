package com.tacz.guns.resource.pojo.data.gun;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.RpmModifier;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GunData {
    @SerializedName("ammo")
    private ResourceLocation ammoId = null;

    @SerializedName("ammo_amount")
    private int ammoAmount = 30;

    @SerializedName("extended_mag_ammo_amount")
    private int @Nullable [] extendedMagAmmoAmount = null;

    @SerializedName("bolt")
    private Bolt bolt = Bolt.OPEN_BOLT;

    @SerializedName("rpm")
    private int roundsPerMinute = 300;

    @SerializedName("bullet")
    private BulletData bulletData = new BulletData();

    @SerializedName("draw_time")
    private float drawTime = 0.4f;

    @SerializedName("put_away_time")
    private float putAwayTime = 0.4f;

    @SerializedName("sprint_time")
    private float sprintTime = 0.2f;

    @SerializedName("aim_time")
    private float aimTime = 0.2f;

    @SerializedName("bolt_action_time")
    private float boltActionTime = 0;

    @SerializedName("reload")
    private GunReloadData reloadData = new GunReloadData();

    @SerializedName("fire_mode")
    private List<FireMode> fireModeSet = Collections.singletonList(FireMode.UNKNOWN);

    @SerializedName("fire_mode_adjust")
    private EnumMap<FireMode, GunFireModeAdjustData> fireModeAdjust = Maps.newEnumMap(FireMode.class);

    @SerializedName("burst_data")
    private BurstData burstData = new BurstData();

    @SerializedName("crawl_recoil_multiplier")
    private float crawlRecoilMultiplier = 0.5f;

    @SerializedName("recoil")
    private GunRecoil recoil = new GunRecoil();

    @SerializedName("hurt_bob_tweak_multiplier")
    private float hurtBobTweakMultiplier = 0.05f;

    @SerializedName("inaccuracy")
    private Map<InaccuracyType, Float> inaccuracy = null;

    @SerializedName("movement_speed")
    private MoveSpeed moveSpeed = new MoveSpeed();

    @SerializedName("melee")
    private GunMeleeData gunMeleeData = new GunMeleeData();

    @SerializedName("allow_attachment_types")
    private List<AttachmentType> allowAttachments = Lists.newArrayList();

    @SerializedName("exclusive_attachments")
    private Map<ResourceLocation, AttachmentData> exclusiveAttachments = Maps.newHashMap();

    @SerializedName("weight")
    private float weight = 0f;

    @SerializedName("builtin_attachments")
    private Map<AttachmentType, ResourceLocation> builtInAttachments = Maps.newHashMap();

    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    public int getAmmoAmount() {
        return ammoAmount;
    }

    public int @Nullable [] getExtendedMagAmmoAmount() {
        return extendedMagAmmoAmount;
    }

    public Bolt getBolt() {
        return bolt;
    }

    @Deprecated
    public int getRoundsPerMinute() {
        return roundsPerMinute;
    }

    public int getRoundsPerMinute(FireMode fireMode) {
        int rpm = roundsPerMinute;
        GunFireModeAdjustData fireModeAdjustData = getFireModeAdjustData(fireMode);
        if (fireModeAdjustData != null) {
            rpm += fireModeAdjustData.getRoundsPerMinute();
        }
        // 为避免非法运算，随意返回一个默认值。
        if (rpm <= 0) {
            return 300;
        }
        return rpm;
    }

    public BulletData getBulletData() {
        return bulletData;
    }

    public float getDrawTime() {
        return drawTime;
    }

    public float getPutAwayTime() {
        return putAwayTime;
    }

    public float getAimTime() {
        return aimTime;
    }

    public float getSprintTime() {
        return sprintTime;
    }

    public float getBoltActionTime() {
        return boltActionTime;
    }

    public GunReloadData getReloadData() {
        return reloadData;
    }

    public List<FireMode> getFireModeSet() {
        return fireModeSet;
    }

    public BurstData getBurstData() {
        return burstData;
    }

    public float getWeight() {
        return weight;
    }

    @Nullable
    public GunFireModeAdjustData getFireModeAdjustData(FireMode fireMode) {
        if (fireModeAdjust != null && fireModeAdjust.containsKey(fireMode)) {
            return fireModeAdjust.get(fireMode);
        }
        return null;
    }

    public float getCrawlRecoilMultiplier() {
        return crawlRecoilMultiplier;
    }

    public GunRecoil getRecoil() {
        return recoil;
    }

    public float getHurtBobTweakMultiplier() {
        return hurtBobTweakMultiplier;
    }

    public Map<InaccuracyType, Float> getInaccuracy() {
        return inaccuracy;
    }

    public void setInaccuracy(Map<InaccuracyType, Float> inaccuracy) {
        this.inaccuracy = inaccuracy;
    }

    public float getInaccuracy(InaccuracyType type) {
        return Math.max(inaccuracy.get(type), 0F);
    }

    public float getInaccuracy(InaccuracyType type, float addend) {
        return Math.max(inaccuracy.get(type) + addend, 0F);
    }

    public MoveSpeed getMoveSpeed() {
        return moveSpeed;
    }

    public GunMeleeData getMeleeData() {
        return gunMeleeData;
    }

    @Nullable
    public List<AttachmentType> getAllowAttachments() {
        return allowAttachments;
    }

    public Map<AttachmentType, ResourceLocation> getBuiltInAttachments() {
        return builtInAttachments;
    }

    public Map<ResourceLocation, AttachmentData> getExclusiveAttachments() {
        return exclusiveAttachments;
    }

    /**
     * @return 枪械开火的间隔，单位为 ms 。
     */
    public long getShootInterval(LivingEntity shooter, FireMode fireMode) {
        int rpm = this.getRoundsPerMinute(fireMode);
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
        if (cacheProperty != null) {
            rpm = Mth.clamp(cacheProperty.<Integer>getCache(RpmModifier.ID), 1, 1200);
        }
        return 60_000L / rpm;
    }

    /**
     * @return 枪械开火的间隔，单位为 ms 。
     */
    public long getBurstShootInterval() {
        // 为避免非法运算，随意返回一个默认值。
        if (burstData == null || burstData.getBpm() <= 0) {
            return 300;
        }
        return 60_000L / burstData.getBpm();
    }
}
