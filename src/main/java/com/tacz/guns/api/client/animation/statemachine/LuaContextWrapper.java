package com.tacz.guns.api.client.animation.statemachine;

import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaContextWrapper<T extends AnimationStateContext> implements AnimationStateContext {
    private T context;
    private LuaValue luaContext;

    public LuaContextWrapper(T context) {
        setContext(context);
    }

    public T getContext() {
        return context;
    }

    public LuaValue getLuaContext() {
        return luaContext;
    }

    void setContext(T context) {
        this.context = context;
        if (luaContext instanceof LuaUserdata userdata) {
            userdata.m_instance = context;
        } else {
            this.luaContext = CoerceJavaToLua.coerce(context);
        }
    }
}
