package com.tacz.guns.entity.shooter;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;
import org.luaj.vm2.script.LuaScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Locale;

/**
 * 所有与配件缓存计算相关的都在这里
 */
public class AttachmentProperty {
    private static final ScriptEngine LUAJ_ENGINE = new LuaScriptEngineFactory().getScriptEngine();

    public float ads;

    public AttachmentProperty(GunData gunData) {
        this.ads = gunData.getAimTime();
    }

    @SuppressWarnings("deprecation")
    public void eval(GunData gunData, AttachmentData data) {
        if (data.getAds() == null) {
            // 兼容旧版本写法
            this.ads = this.ads + data.getAdsAddendTime();
        } else {
            this.ads = (float) eval(data.getAds(), this.ads, gunData.getAimTime());
        }
    }

    public static void postChangeEvent(LivingEntity shooter, ItemStack gunItem) {
        if (!(gunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            AttachmentProperty property = new AttachmentProperty(index.getGunData());
            MinecraftForge.EVENT_BUS.post(new AttachmentPropertyEvent(gunItem, property));
            IGunOperator.fromLivingEntity(shooter).updateAttachmentProperty(property);
        });
    }

    private static double eval(ModifiedValue modifiedValue, double value, double defaultValue) {
        String script = modifiedValue.getFunction();
        // 如果没有脚本 function，那么检查乘和加
        if (StringUtils.isBlank(script)) {
            Double multiply = modifiedValue.getMultiply();
            // 没有乘，那么为加
            if (multiply == null) {
                return value + modifiedValue.getAddend();
            }
            return value * multiply;
        }
        return functionEval(value, defaultValue, script);
    }

    private static double functionEval(double value, double defaultValue, String script) {
        script = script.toLowerCase(Locale.ENGLISH);
        LUAJ_ENGINE.put("x", value);
        LUAJ_ENGINE.put("r", defaultValue);
        try {
            LUAJ_ENGINE.eval(script);
        } catch (ScriptException e) {
            GunMod.LOGGER.catching(e);
        }
        if (LUAJ_ENGINE.get("y") instanceof Double number) {
            return number;
        }
        return value;
    }
}
