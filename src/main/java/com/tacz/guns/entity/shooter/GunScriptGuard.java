package com.tacz.guns.entity.shooter;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.LuaError;

import javax.annotation.Nullable;
import java.util.function.Supplier;

// 兜底，防止脚本爆炸连服务端一起扬了
public class GunScriptGuard {
    private GunScriptGuard() {
    }

    public static void run(ShooterDataHolder data, LivingEntity shooter, @Nullable ResourceLocation gunId,
                           String operation, Runnable action) {
        run(data, shooter, gunId, operation, () -> {
            action.run();
            return null;
        }, null);
    }

    public static <T> T run(ShooterDataHolder data, LivingEntity shooter, @Nullable ResourceLocation gunId,
                            String operation, Supplier<T> action, T fallback) {
        if (data.gunScriptFailed) {
            return fallback;
        }
        if (gunId == null || !gunId.equals(data.currentGunId)) {
            return fallback;
        }
        ItemStack current = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        if (!hasGunId(current, gunId) || !hasGunId(shooter.getMainHandItem(), gunId)) {
            data.cancelGunScriptState();
            return fallback;
        }
        try {
            T result = action.get();
            return data.gunScriptFailed ? fallback : result;
        } catch (LuaError exception) {
            data.cancelGunScriptState();
            data.gunScriptFailed = true;
            GunMod.LOGGER.error("Gun script failed for {} during {} (entity {}); paused until next draw",
                    gunId, operation, shooter.getUUID(), exception);
            return fallback;
        }
    }

    private static boolean hasGunId(@Nullable ItemStack stack, ResourceLocation gunId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        IGun gun = IGun.getIGunOrNull(stack);
        return gun != null && gunId.equals(gun.getGunId(stack));
    }
}
