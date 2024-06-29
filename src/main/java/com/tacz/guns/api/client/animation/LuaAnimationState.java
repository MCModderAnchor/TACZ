package com.tacz.guns.api.client.animation;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import javax.annotation.Nullable;

public class LuaAnimationState<T extends StateContext> implements AnimationState<T> {
    private final LuaTable luaTable;
    private final @Nullable LuaFunction updateFunction;
    private final @Nullable LuaFunction enterFunction;
    private final @Nullable LuaFunction exitFunction;
    private final @Nullable LuaFunction transitionFunction;

    public LuaAnimationState(LuaTable luaTable) {
        this.luaTable = luaTable;
        this.updateFunction = checkLuaFunction("update");
        this.enterFunction = checkLuaFunction("entry");
        this.exitFunction = checkLuaFunction("exit");
        this.transitionFunction = checkLuaFunction("transition");
    }

    @Override
    public void update(T context) {
        if (updateFunction != null) {
            LuaValue contextToLua = CoerceJavaToLua.coerce(context);
            updateFunction.call(contextToLua);
        }
    }

    @Override
    public void entryAction(T context) {
        if (enterFunction != null) {
            LuaValue contextToLua = CoerceJavaToLua.coerce(context);
            enterFunction.call(contextToLua);
        }
    }

    @Override
    public void exitAction(T context) {
        if (exitFunction != null) {
            LuaValue contextToLua = CoerceJavaToLua.coerce(context);
            exitFunction.call(contextToLua);
        }
    }

    @Override
    public AnimationState<T> transition(T context, String condition) {
        if (transitionFunction != null) {
            LuaValue contextToLua = CoerceJavaToLua.coerce(context);
            LuaString conditionToLua = LuaString.valueOf(condition);
            LuaValue nextStateTable = transitionFunction.call(contextToLua, conditionToLua);
            if (nextStateTable.istable()) {
                return new LuaAnimationState<>((LuaTable) nextStateTable);
            } else if (nextStateTable.isnil()) {
                return null;
            }
            throw new LuaError("the return of function 'transition' must be table or nil");
        }
        return null;
    }

    private LuaFunction checkLuaFunction(String funcName) {
        LuaValue value = luaTable.get(funcName);
        if (value.isfunction()) {
            return (LuaFunction) value;
        } else if (value.isnil()) {
            return null;
        }
        throw new LuaError("the type of field '" + funcName + "' must be function or nil");
    }
}
