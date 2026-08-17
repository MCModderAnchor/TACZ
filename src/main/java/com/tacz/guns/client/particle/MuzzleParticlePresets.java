package com.tacz.guns.client.particle;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 枪械类型 → 第一人称枪口粒子定义的映射。
 * <p>
 * 内置只有一组通用定义（{@code tacz:muzzle_flash} 与配套的 {@code tacz:muzzle_flash_smoke}），
 * 所有枪型统一使用；枪与枪之间的表现差异由枪械 display 的 {@code muzzle_flash.scale}
 * （随配件倍率一起作用于发射器缩放）驱动。
 * <p>
 * 若未来想给特定枪型单独做效果：在包里提供同名定义（如 {@code tacz:muzzle_flash_shotgun}）
 * 并在本方法恢复对应分支即可，无需改动渲染层。
 */
public final class MuzzleParticlePresets {
    private MuzzleParticlePresets() {
    }

    /** 内置通用火光定义。 */
    public static final ResourceLocation GENERIC = new ResourceLocation("tacz", "muzzle_flash");

    @Nullable
    public static ResourceLocation forGunType(@Nullable String gunType) {
        return GENERIC;
    }

    /** 由火光定义 id 推导配套烟雾定义 id。 */
    public static ResourceLocation smokeOf(ResourceLocation flashId) {
        return flashId.withSuffix("_smoke");
    }
}
