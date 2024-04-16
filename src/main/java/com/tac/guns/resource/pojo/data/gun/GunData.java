package com.tac.guns.resource.pojo.data.gun;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.resource.pojo.data.attachment.AttachmentData;
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
    private float sprintTime = 0.3f;

    @SerializedName("aim_time")
    private float aimTime = 0.2f;

    @SerializedName("reload")
    private GunReloadData reloadData = new GunReloadData();

    @SerializedName("fire_mode")
    private List<FireMode> fireModeSet = Collections.singletonList(FireMode.UNKNOWN);

    @SerializedName("recoil")
    private GunRecoil recoil = new GunRecoil();

    @SerializedName("inaccuracy")
    private Map<InaccuracyType, Float> inaccuracy = null;

    @SerializedName("move_speed")
    private MoveSpeed moveSpeed = new MoveSpeed();

    @SerializedName("allow_attachments")
    private Map<AttachmentType, AttachmentPass> allowAttachments = Maps.newEnumMap(AttachmentType.class);

    @SerializedName("exclusive_attachments")
    private Map<ResourceLocation, AttachmentData> exclusiveAttachments = Maps.newHashMap();

    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    public int getAmmoAmount() {
        return ammoAmount;
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

    public GunReloadData getReloadData() {
        return reloadData;
    }

    public List<FireMode> getFireModeSet() {
        return fireModeSet;
    }

    public GunRecoil getRecoil() {
        return recoil;
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

    @Nullable
    public Map<AttachmentType, AttachmentPass> getAllowAttachments() {
        return allowAttachments;
    }

    public boolean allowAttachmentType(AttachmentType type) {
        return allowAttachments.containsKey(type);
    }

    public Map<ResourceLocation, AttachmentData> getExclusiveAttachments() {
        return exclusiveAttachments;
    }

    /**
     * @return 枪械开火的间隔，单位为 ms 。
     */
    public long getShootInterval() {
        // 为避免非法运算，随意返回一个默认值。
        if (roundsPerMinute == 0) {
            return 300;
        }
        return 60_000L / roundsPerMinute;
    }
}
