package com.tacz.guns.api.vmlib;

import com.google.common.collect.Maps;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.gun.FireMode;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Map;

public class LuaGunLogicConstant implements LuaLibrary{
    private final Map<String, Object> constantMap = Maps.newHashMap();

    public LuaGunLogicConstant() {
        // 映射 ReloadState.StateType 枚举
        for (ReloadState.StateType stateType : ReloadState.StateType.values()) {
            constantMap.put(stateType.name(), stateType.ordinal());
        }
        // 映射 FireMode 枚举
        for (var fireMode : FireMode.values()) {
            constantMap.put(fireMode.name(), fireMode.ordinal());
        }
    }

    @Override
    public void install(LuaValue chunk) {
        for(Map.Entry<String, Object> entry : constantMap.entrySet()) {
            chunk.set(entry.getKey(), CoerceJavaToLua.coerce(entry.getValue()));
        }
    }
}
