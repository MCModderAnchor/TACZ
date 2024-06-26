package com.tacz.guns.api.client.animation.lua;

import com.tacz.guns.api.client.animation.AnimationState;
import com.tacz.guns.api.client.animation.StateContext;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaAnimationState implements AnimationState {
    private final LuaTable luaTable;

    public LuaAnimationState(LuaTable luaTable) {
        this.luaTable = luaTable;
    }

    @Override
    public void update(StateContext context) {
        callLuaAction("update", context);
    }

    @Override
    public void entryAction(StateContext context) {
        callLuaAction("entry", context);
    }

    @Override
    public void exitAction(StateContext context) {
        callLuaAction("exit", context);
    }

    @Override
    public AnimationState transition(StateContext context, String condition) {
        LuaValue contextToLua = CoerceJavaToLua.coerce(context);
        LuaString conditionToLua = LuaString.valueOf(condition);
        LuaValue transitionFunc = luaTable.get("transition");
        LuaValue nextStateTable = transitionFunc.call(contextToLua, conditionToLua);
        if (nextStateTable.istable()) {
            return new LuaAnimationState((LuaTable) nextStateTable);
        } else if (nextStateTable.isnil()) {
            return null;
        }
        throw new LuaError("the return of function 'transition' must be table or nil");
    }

    private void callLuaAction(String funcName, StateContext context) {
        LuaValue actionFunc = luaTable.get(funcName);
        if (actionFunc.isfunction()) {
            LuaValue contextToLua = CoerceJavaToLua.coerce(context);
            actionFunc.call(contextToLua);
        } else if (actionFunc.isnil()) {
            return;
        }
        throw new LuaError("the type of field '" + funcName + "' must be function or nil");
    }
}
