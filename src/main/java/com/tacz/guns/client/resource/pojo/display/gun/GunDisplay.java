package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.resource.pojo.display.SoundData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GunDisplay implements IDisplay {
    @SerializedName("model_type")
    private String modelType = "default";
    @SerializedName("model")
    private ResourceLocation modelLocation;
    @SerializedName("texture")
    private ResourceLocation modelTexture;
    @SerializedName("iron_zoom")
    private float ironZoom = 1.2f;
    @SerializedName("model_fov")
    private float modelFov = 70f;
    @SerializedName("zoom_model_fov")
    private float zoomModelFov = 70f;
    @Nullable
    @SerializedName("ads_path")
    private AdsPath adsPath;
    @Nullable
    @SerializedName("ads_fov")
    private AdsFov adsFov;
    @Nullable
    @SerializedName("ads_preset")
    private ResourceLocation adsPreset;
    @Nullable
    @SerializedName("lod")
    private GunLod gunLod;
    @Nullable
    @SerializedName("hud")
    private ResourceLocation hudTextureLocation;
    @Nullable
    @SerializedName("hud_empty")
    private ResourceLocation hudEmptyTextureLocation;
    @Nullable
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;
    @NotNull
    @SerializedName("ammo_count_style")
    private AmmoCountStyle ammoCountStyle = AmmoCountStyle.NORMAL;
    @NotNull
    @SerializedName("damage_style")
    private DamageStyle damageStyle = DamageStyle.PER_PROJECTILE;
    @Nullable
    @SerializedName("third_person_animation")
    private String thirdPersonAnimation;
    @Nullable
    @SerializedName("animation")
    private ResourceLocation animationLocation;
    @Nullable
    @SerializedName("state_machine")
    private ResourceLocation stateMachineLocation;
    @Nullable
    @SerializedName("state_machine_param")
    private Map<String, Object> stateMachineParam = null;
    @Nullable
    @SerializedName("use_default_animation")
    // 弃用，优先使用 default_animation 指定默认动画
    private DefaultAnimationType defaultAnimationType;
    @Nullable
    @SerializedName("default_animation")
    private ResourceLocation defaultAnimation = null;
    @Nullable
    @SerializedName("player_animator_3rd")
    private ResourceLocation playerAnimator3rd;
    @Nullable
    @SerializedName("3rd_fixed_hand")
    private boolean playerAnimator3rdFixedHand = false;
    @Nullable
    @SerializedName("sounds")
    private Map<String, SoundData> sounds;
    @Nullable
    @SerializedName("preload_sounds")
    private List<String> preloadSounds;
    @Nullable
    @SerializedName("transform")
    private GunTransform transform;
    @Nullable
    @SerializedName("shell")
    private ShellEjection shellEjection;
    @Nullable
    @SerializedName("ammo")
    private GunAmmo gunAmmo;
    @Nullable
    @SerializedName("muzzle_flash")
    private MuzzleFlash muzzleFlash;
    @SerializedName("offhand_show")
    private LayerGunShow offhandShow = new LayerGunShow();
    @Nullable
    @SerializedName("hotbar_show")
    private Map<String, LayerGunShow> hotbarShow = null;
    @SerializedName("text_show")
    private Map<String, TextShow> textShows = Maps.newHashMap();
    @SerializedName("show_crosshair")
    private boolean showCrosshair = false;
    @SerializedName("controllable")
    private EnumMap<FireMode, ControllableData> controllableData = Maps.newEnumMap(FireMode.class);
    @SerializedName("laser")
    private LaserConfig laserConfig;

    /**
     * @since 1.1.8
     */
    @SerializedName("enable_transparency")
    private boolean enableTransparency;

    public String getModelType() {
        return modelType;
    }

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public GunLod getGunLod() {
        return gunLod;
    }

    @Nullable
    public ResourceLocation getHudTextureLocation() {
        return hudTextureLocation;
    }

    @Nullable
    public ResourceLocation getHudEmptyTextureLocation() {
        return hudEmptyTextureLocation;
    }

    @Nullable
    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }

    @Nullable
    public ResourceLocation getAnimationLocation() {
        return animationLocation;
    }

    @Nullable
    public ResourceLocation getStateMachineLocation() {
        return stateMachineLocation;
    }

    @Nullable
    public Map<String, Object> getStateMachineParam() {
        return stateMachineParam;
    }

    @Nullable
    public DefaultAnimationType getDefaultAnimationType() {
        return defaultAnimationType;
    }

    @Nullable
    public ResourceLocation getDefaultAnimation() {
        return defaultAnimation;
    }

    @Nullable
    public ResourceLocation getPlayerAnimator3rd() {
        return playerAnimator3rd;
    }

    @Nullable
    public String getThirdPersonAnimation() {
        return thirdPersonAnimation;
    }

    @Nullable
    public Map<String, SoundData> getSounds() {
        return sounds;
    }

    @Nullable
    public List<String> getPreloadSounds() {
        return preloadSounds;
    }

    @Nullable
    public GunTransform getTransform() {
        return transform;
    }

    @Nullable
    public ShellEjection getShellEjection() {
        return shellEjection;
    }

    @Nullable
    public GunAmmo getGunAmmo() {
        return gunAmmo;
    }

    @Nullable
    public MuzzleFlash getMuzzleFlash() {
        return muzzleFlash;
    }

    public LayerGunShow getOffhandShow() {
        return offhandShow;
    }

    @Nullable
    public Map<String, LayerGunShow> getHotbarShow() {
        return hotbarShow;
    }

    public float getIronZoom() {
        return ironZoom;
    }

    public float getModelFov() {
        return modelFov;
    }

    public float getZoomModelFov() {
        return zoomModelFov;
    }

    @Nullable
    public AdsPath getAdsPath() {
        return adsPath;
    }

    public @Nullable AdsFov getAdsFov() {
        return adsFov;
    }

    public @Nullable ResourceLocation getAdsPreset() {
        return adsPreset;
    }

    public Map<String, TextShow> getTextShows() {
        return textShows;
    }

    public boolean isShowCrosshair() {
        return showCrosshair;
    }

    public EnumMap<FireMode, ControllableData> getControllableData() {
        return controllableData;
    }

    public @NotNull AmmoCountStyle getAmmoCountStyle() {
        return ammoCountStyle;
    }

    public @NotNull DamageStyle getDamageStyle() {
        return damageStyle;
    }

    @Nullable
    public LaserConfig getLaserConfig() {
        return laserConfig;
    }

    public boolean enablesTransparency() {
        return enableTransparency;
    }

    public boolean is3rdFixedHand() {
        return playerAnimator3rdFixedHand;
    }

    @Override
    public void init() {
        if (modelTexture != null) {
            modelTexture = converter.idToFile(modelTexture);
        }
        if (hudTextureLocation != null) {
            hudTextureLocation = converter.idToFile(hudTextureLocation);
        }
        if (hudEmptyTextureLocation != null) {
            hudEmptyTextureLocation = converter.idToFile(hudEmptyTextureLocation);
        }
        if (slotTextureLocation != null) {
            slotTextureLocation = converter.idToFile(slotTextureLocation);
        }
        if (gunLod != null && gunLod.modelTexture != null) {
            gunLod.modelTexture = converter.idToFile(gunLod.modelTexture);
        }
        if (muzzleFlash != null && muzzleFlash.texture != null) {
            muzzleFlash.texture = converter.idToFile(muzzleFlash.texture);
        }
        if (shellEjection != null) {
            shellEjection.init();
        }
    }
}
