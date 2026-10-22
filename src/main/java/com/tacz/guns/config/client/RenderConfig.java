package com.tacz.guns.config.client;

import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.client.renderer.scope.ScopeVisionMode;
import net.minecraftforge.common.ForgeConfigSpec;

public class RenderConfig {
    public static ForgeConfigSpec.BooleanValue ENABLE_LASER_FADE_OUT;
    public static ForgeConfigSpec.BooleanValue RENDER_MUZZLE_SMOKE;
    public static ForgeConfigSpec.BooleanValue DISABLE_THIRD_PERSON_OFFHAND_AND_HOTBAR_GUN_RENDER;
    public static ForgeConfigSpec.IntValue GUN_LOD_RENDER_DISTANCE;
    public static ForgeConfigSpec.IntValue BULLET_HOLE_PARTICLE_LIFE;
    public static ForgeConfigSpec.DoubleValue BULLET_HOLE_PARTICLE_FADE_THRESHOLD;
    public static ForgeConfigSpec.BooleanValue HIT_PARTICLE_ENABLE;
    public static ForgeConfigSpec.BooleanValue HIT_EXTRA_PARTICLE_ENABLE;
    public static ForgeConfigSpec.IntValue HIT_PARTICLE_MAX_DISTANCE;
    public static ForgeConfigSpec.EnumValue<CrosshairType> CROSSHAIR_TYPE;
    public static ForgeConfigSpec.DoubleValue HIT_MARKET_START_POSITION;
    public static ForgeConfigSpec.BooleanValue HEAD_SHOT_DEBUG_HITBOX;
    public static ForgeConfigSpec.BooleanValue GUN_HUD_ENABLE;
    public static ForgeConfigSpec.BooleanValue KILL_AMOUNT_ENABLE;
    public static ForgeConfigSpec.DoubleValue KILL_AMOUNT_DURATION_SECOND;
    public static ForgeConfigSpec.IntValue TARGET_RENDER_DISTANCE;
    public static ForgeConfigSpec.BooleanValue FIRST_PERSON_BULLET_TRACER_ENABLE;
    public static ForgeConfigSpec.BooleanValue DISABLE_INTERACT_HUD_TEXT;
    public static ForgeConfigSpec.BooleanValue AUTO_SELECT_GUN_SMITH_TABLE_FILTER;
    public static ForgeConfigSpec.BooleanValue RENDER_CREATIVE_PACK_SIDEBAR;
    public static ForgeConfigSpec.IntValue DAMAGE_COUNTER_RESET_TIME;
    public static ForgeConfigSpec.BooleanValue DISABLE_MOVEMENT_ATTRIBUTE_FOV;
    public static ForgeConfigSpec.DoubleValue GUN_HURT_BOB_MULTIPLIER;
    public static ForgeConfigSpec.BooleanValue ENABLE_TACZ_ID_IN_TOOLTIP;
    public static ForgeConfigSpec.BooleanValue BLOCK_ENTITY_TRANSLUCENT;
    public static ForgeConfigSpec.BooleanValue DISPLAY_GUN_VBO;
    public static ForgeConfigSpec.BooleanValue STATIC_ITEM_GUN_VBO;
    public static ForgeConfigSpec.BooleanValue EXPERIMENTAL_SCOPE_LENS;
    public static ForgeConfigSpec.DoubleValue SCOPE_LENS_DISTORTION;
    public static ForgeConfigSpec.DoubleValue SCOPE_LENS_ABERRATION;
    public static ForgeConfigSpec.DoubleValue SCOPE_LENS_EDGE_WIDTH;
    public static ForgeConfigSpec.IntValue SCOPE_LENS_TINT;
    public static ForgeConfigSpec.DoubleValue SCOPE_LENS_TINT_STRENGTH;
    public static ForgeConfigSpec.EnumValue<ScopeVisionMode> SCOPE_VISION_MODE;
    public static ForgeConfigSpec.DoubleValue SCOPE_VISION_GAIN;
    public static ForgeConfigSpec.DoubleValue SCOPE_VISION_NOISE;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("render");

        builder.comment("Whether or not apply fadeout effect on the laser beam. Close this may improve laser performance under some shaders.");
        ENABLE_LASER_FADE_OUT = builder.define("EnableLaserFadeOut", true);

        builder.comment("Whether or not to render the renew (snowstorm particle) muzzle smoke. Disable this if translucent smoke has issues with shaders.");
        RENDER_MUZZLE_SMOKE = builder.define("RenderMuzzleSmoke", true);

        builder.comment("Disable third-person rendering of guns stored in the offhand and unselected hotbar slots. Main-hand gun rendering is unaffected.");
        DISABLE_THIRD_PERSON_OFFHAND_AND_HOTBAR_GUN_RENDER = builder.define("DisableThirdPersonOffhandAndHotbarGunRender", false);

        builder.comment("How far to display the lod model, 0 means always display");
        GUN_LOD_RENDER_DISTANCE = builder.defineInRange("GunLodRenderDistance", 0, 0, Integer.MAX_VALUE);

        builder.comment("The existence time of bullet hole particles, in tick");
        BULLET_HOLE_PARTICLE_LIFE = builder.defineInRange("BulletHoleParticleLife", 400, 0, Integer.MAX_VALUE);

        builder.comment("The threshold for fading out when rendering bullet hole particles");
        BULLET_HOLE_PARTICLE_FADE_THRESHOLD = builder.defineInRange("BulletHoleParticleFadeThreshold", 0.98, 0, 1);

        builder.comment("Whether or not to spawn extra hit particles when bullets leave bullet holes");
        HIT_PARTICLE_ENABLE = builder.define("HitParticleEnable", true);

        builder.comment("Whether or not to spawn display-configured extra hit particles. Disabling this keeps the basic block hit particles.");
        HIT_EXTRA_PARTICLE_ENABLE = builder.define("HitExtraParticleEnable", true);

        builder.comment("The farthest distance to spawn extra hit particles, 0 means always spawn");
        HIT_PARTICLE_MAX_DISTANCE = builder.defineInRange("HitParticleMaxDistance", 64, 0, Integer.MAX_VALUE);

        builder.comment("The crosshair when holding a gun");
        CROSSHAIR_TYPE = builder.defineEnum("CrosshairType", CrosshairType.DOT_1);

        builder.comment("The starting position of the hit marker");
        HIT_MARKET_START_POSITION = builder.defineInRange("HitMarketStartPosition", 4d, -1024d, 1024d);

        builder.comment("Whether or not to display the head shot's hitbox");
        HEAD_SHOT_DEBUG_HITBOX = builder.define("HeadShotDebugHitbox", false);

        builder.comment("Whether or not to display the gun's HUD");
        GUN_HUD_ENABLE = builder.define("GunHUDEnable", true);

        builder.comment("Whether or not to display the kill amount");
        KILL_AMOUNT_ENABLE = builder.define("KillAmountEnable", true);

        builder.comment("The duration of the kill amount, in second");
        KILL_AMOUNT_DURATION_SECOND = builder.defineInRange("KillAmountDurationSecond", 3, 0, Double.MAX_VALUE);

        builder.comment("The farthest render distance of the target, including minecarts type");
        TARGET_RENDER_DISTANCE = builder.defineInRange("TargetRenderDistance", 128, 0, Integer.MAX_VALUE);

        builder.comment("Whether or not to render first person bullet trail");
        FIRST_PERSON_BULLET_TRACER_ENABLE = builder.define("FirstPersonBulletTracerEnable", true);

        builder.comment("Disable the interact hud text in center of the screen");
        DISABLE_INTERACT_HUD_TEXT = builder.define("DisableInteractHudText", false);

        builder.comment("Whether or not to automatically select the gun smith table's held item filter when opening it with a gun, attachment or ammo in main hand");
        AUTO_SELECT_GUN_SMITH_TABLE_FILTER = builder.define("AutoSelectGunSmithTableFilter", true);

        builder.comment("Whether or not to display the content pack sidebar in TACZ creative tabs. Disabling this also restores all items in the current tab.");
        RENDER_CREATIVE_PACK_SIDEBAR = builder.define("RenderCreativePackSidebar", true);

        builder.comment("Max time the damage counter will reset");
        DAMAGE_COUNTER_RESET_TIME = builder.defineInRange("DamageCounterResetTime", 2000, 10, Integer.MAX_VALUE);

        builder.comment("Disable the fov effect from the movement speed attribute while holding a gun");
        DISABLE_MOVEMENT_ATTRIBUTE_FOV = builder.define("DisableMovementAttributeFov", true);

        builder.comment("First-person vanilla hurt tilt multiplier while holding a gun in the main hand. 0 disables it, 1 preserves vanilla side-hit strength. Front/back hits use half this multiplier, with smooth transitions for diagonal hits. Multiplies the vanilla Damage Tilt setting; gunshot-specific hurt effects keep their gun data settings.");
        GUN_HURT_BOB_MULTIPLIER = builder.defineInRange("GunHurtBobMultiplier", 0.1, 0, 1);

        builder.comment("Enable the display of the TACZ ID in the tooltip when Advanced Tooltip is enabled");
        ENABLE_TACZ_ID_IN_TOOLTIP = builder.define("EnableTaczIdInTooltip", true);

        builder.comment("Enable translucent while render block entity or not. Enable this option will result in ADDITIONAL PERFORMANCE OVERHEAD.");
        BLOCK_ENTITY_TRANSLUCENT = builder.define("EnableBlockEntityTranslucent", false);

        builder.comment("Experimental SBM VBO rendering for guns in display cases and pegboards.");
        DISPLAY_GUN_VBO = builder.define("EnableExperimentalDisplayGunVbo", true);

        builder.comment("Experimental SBM VBO rendering for third-person held, dropped and fixed guns outside GUI screens.");
        STATIC_ITEM_GUN_VBO = builder.define("EnableStaticItemGunVbo", true);

        builder.comment("Experimental scope lens tint, chromatic aberration and edge distortion. Uses the existing scope window and keeps the normal aiming magnification.");
        EXPERIMENTAL_SCOPE_LENS = builder.define("EnableExperimentalScopeLens", true);

        builder.comment("Radial distortion at the edge of a scope lens. 0 disables distortion.");
        SCOPE_LENS_DISTORTION = builder.defineInRange("ScopeLensDistortion", 0.05, 0, 0.2);

        builder.comment("Radial RGB separation at the edge of a scope lens. 0 disables chromatic aberration.");
        SCOPE_LENS_ABERRATION = builder.defineInRange("ScopeLensAberration", 0.007, 0, 0.03);

        builder.comment("Scope lens vignette width. Also controls how far edge distortion and aberration extend into the lens. 0 disables all edge effects.");
        SCOPE_LENS_EDGE_WIDTH = builder.defineInRange("ScopeLensEdgeWidth", 0.35, 0, 2);

        builder.comment("Scope lens RGB tint, as a decimal integer. Default 7384808 is #70AEE8.");
        SCOPE_LENS_TINT = builder.defineInRange("ScopeLensTint", 0x70AEE8, 0, 0xFFFFFF);

        builder.comment("Scope lens tint strength. 0 preserves the original scene colour, 1 applies the full tint.");
        SCOPE_LENS_TINT_STRENGTH = builder.defineInRange("ScopeLensTintStrength", 0.2, 0, 1);

        builder.comment("Experimental vision preset for all scope lenses. Thermal modes highlight visible living entity models with scene depth occlusion; they do not simulate temperature. NORMAL preserves the optical lens.");
        SCOPE_VISION_MODE = builder.defineEnum("ScopeVisionMode", ScopeVisionMode.NORMAL);

        builder.comment("Night vision luminance gain. Enhances existing scene detail; cannot recover detail from completely black pixels.");
        SCOPE_VISION_GAIN = builder.defineInRange("ScopeVisionGain", 3.0, 1.0, 12.0);

        builder.comment("Sensor grain strength for night vision and thermal presets. 0 disables grain.");
        SCOPE_VISION_NOISE = builder.defineInRange("ScopeVisionNoise", 0.025, 0, 0.2);

        builder.pop();
    }
}
