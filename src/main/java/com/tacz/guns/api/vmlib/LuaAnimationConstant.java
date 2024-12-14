package com.tacz.guns.api.vmlib;

import com.google.common.collect.Maps;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.statemachine.AnimationConstant;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * 此类用于在 Lua 脚本中引入 ContextConstant 定义的常量，如播放类型等。
 * 调用 install 方法直接将常量注入环境
 * @see AnimationConstant
 */
public class LuaAnimationConstant implements LuaLibrary {
    private final Map<String, Object> constantMap = Maps.newHashMap();

    public LuaAnimationConstant() {
        // 获取 AnimationConstant 中所有 public 字段
        Field[] fields = AnimationConstant.class.getFields();
        // 将 static final 的常量字段提取到 constantMap
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                try {
                    // 获取变量名和值
                    String name = field.getName();
                    Object value = field.get(null);
                    constantMap.put(name, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        // 映射 PlayType 枚举
        for (var playType : ObjectAnimation.PlayType.values()) {
            constantMap.put(playType.name(), playType.ordinal());
        }
    }

    @Override
    public void install(LuaValue chunk) {
        for(Map.Entry<String, Object> entry : constantMap.entrySet()) {
            chunk.set(entry.getKey(), CoerceJavaToLua.coerce(entry.getValue()));
        }
    }
}
