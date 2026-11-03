package com.tacz.guns.api.modifier;


import com.google.common.collect.ImmutableList;
import com.tacz.guns.resource.manager.AttachmentScriptManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;

import java.util.List;
/**
 * 参数化的属性缓存。将每个乘区的结果保存在缓存中，以便快速计算。
 * 适用于一些初始值无法确定直接确定的属性，如后坐力
 */
public class ParameterizedCache<T> {
    private final T defaultValue;
    private final List<AttachmentScriptManager.CompiledScript> scripts;
    private final double addend;
    private final double percent;
    private final double multiplier;

    public ParameterizedCache(List<Modifier> modifiers, T defaultValue) {
        double addend = 0;
        double percent = 1;
        double multiplier = 1;

        ImmutableList.Builder<AttachmentScriptManager.CompiledScript> builder = new ImmutableList.Builder<>();
        for (Modifier mod : modifiers) {
            addend += mod.getAddend();
            percent += mod.getPercent();
            multiplier *= Math.max(mod.getMultiplier(), 0f);
            if (mod.getCompiledFunction() != null) {
                builder.add(mod.getCompiledFunction());
            }
        }

        this.addend = addend;
        this.percent = percent;
        this.multiplier = multiplier;
        this.scripts = builder.build();
        this.defaultValue = defaultValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public double eval(double input) {
        double percent = Math.max(this.percent, 0);
        double value = (input + addend) * percent * multiplier;
        for (AttachmentScriptManager.CompiledScript function : scripts) {
            value = function.eval(value, input);
        }
        return value;
    }

    public double eval(double input, double extraAddend, double extraPercent, double extraMultiplier) {
        double percent = Math.max(this.percent + extraPercent, 0);
        extraMultiplier = Math.max(extraMultiplier, 0);
        double value = (input + addend + extraAddend) * percent * multiplier * extraMultiplier;
        for (AttachmentScriptManager.CompiledScript function : scripts) {
            value = function.eval(value, input);
        }
        return value;
    }

    public static <T> ParameterizedCache<T> of(T defaultValue) {
        return new ParameterizedCache<>(List.of(), defaultValue);
    }

    public static <T> ParameterizedCache<T> of(List<Modifier> modifiers, T defaultValue) {
        return new ParameterizedCache<>(modifiers, defaultValue);
    }

}
