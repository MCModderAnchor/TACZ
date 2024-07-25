package com.tacz.guns.resource.modifier;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;
import org.luaj.vm2.script.LuaScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Locale;
import java.util.Map;

public class AttachmentPropertyManager {
    private static final ScriptEngine LUAJ_ENGINE = new LuaScriptEngineFactory().getScriptEngine();
    private static final Map<String, IAttachmentModifier<?, ?>> MODIFIERS = Maps.newHashMap();

    public static void registerModifier() {
        MODIFIERS.put(AdsModifier.ID, new AdsModifier());
        MODIFIERS.put(InaccuracyModifier.ID, new InaccuracyModifier());
    }

    public static Map<String, IAttachmentModifier<?, ?>> getModifiers() {
        return MODIFIERS;
    }

    public static void postChangeEvent(LivingEntity shooter, ItemStack gunItem) {
        if (!(gunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            AttachmentCacheProperty cacheProperty = new AttachmentCacheProperty(gunItem, index.getGunData());
            MinecraftForge.EVENT_BUS.post(new AttachmentPropertyEvent(gunItem, cacheProperty));
            IGunOperator.fromLivingEntity(shooter).updateCacheProperty(cacheProperty);
        });
    }

    public static double eval(ModifiedValue modifiedValue, double value, double defaultValue) {
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
        if (LUAJ_ENGINE.get("y") instanceof Number number) {
            return number.doubleValue();
        }
        return value;
    }
}
