package com.tacz.guns.resource.pojo.data.gun;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
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

    @SerializedName("burst_data")
    private BurstData burstData = new BurstData();

    @SerializedName("recoil")
    private GunRecoil recoil = new GunRecoil();

    @SerializedName("hurt_bob_tweak_multiplier")
    private float hurtBobTweakMultiplier = 0.05f;

    @SerializedName("inaccuracy")
    private Map<InaccuracyType, Float> inaccuracy = null;

    @SerializedName("move_speed")
    private MoveSpeed moveSpeed = new MoveSpeed();

    @SerializedName("bayonet_melee_distance")
    private double bayonetMeleeDistance = 0d;

    @SerializedName("allow_attachment_types")
    private List<AttachmentType> allowAttachments = Lists.newArrayList();

    @SerializedName("exclusive_attachments")
    private Map<ResourceLocation, AttachmentData> exclusiveAttachments = Maps.newHashMap();

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

    public int getRoundsPerMinute() {
        return roundsPerMinute;
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
        return inaccuracy.get(type);
    }

    public MoveSpeed getMoveSpeed() {
        return moveSpeed;
    }

    public double getBayonetMeleeDistance() {
        return bayonetMeleeDistance;
    }

    @Nullable
    public List<AttachmentType> getAllowAttachments() {
        return allowAttachments;
    }

    public Map<ResourceLocation, AttachmentData> getExclusiveAttachments() {
        return exclusiveAttachments;
    }

    /**
     * @return 枪械开火的间隔，单位为 ms 。
     */
    public long getShootInterval() {
        // 为避免非法运算，随意返回一个默认值。
        if (roundsPerMinute <= 0) {
            return 300;
        }
        return 60_000L / roundsPerMinute;
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
