package com.tacz.guns.api.client.animation.statemachine;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LuaAnimationState<T extends AnimationStateContext> implements AnimationState<T> {
    private final @Nonnull LuaTable stateTable;
    private final @Nonnull LuaTable scriptTable;
    private final @Nullable LuaFunction updateFunction;
    private final @Nullable LuaFunction enterFunction;
    private final @Nullable LuaFunction exitFunction;
    private final @Nullable LuaFunction transitionFunction;

    /**
     * 此方法用于通过 lua 脚本生成状态。不应该被直接调用，而是通过工厂生成。
     *
     * @param stateTable 包含各个函数的表
     * @see LuaStateMachineFactory
     */
    LuaAnimationState(@Nonnull LuaTable stateTable, @Nonnull LuaTable scriptTable) {
        this.stateTable = stateTable;
        this.scriptTable = scriptTable;
        this.updateFunction = checkLuaFunction("update");
        this.enterFunction = checkLuaFunction("entry");
        this.exitFunction = checkLuaFunction("exit");
        this.transitionFunction = checkLuaFunction("transition");
    }

    @Override
    public void update(T context) {
        if (updateFunction != null) {
            updateFunction.call(scriptTable, CoerceJavaToLua.coerce(context));
        }
    }

    @Override
    public void entryAction(T context) {
        if (enterFunction != null) {
            enterFunction.call(scriptTable, CoerceJavaToLua.coerce(context));
        }
    }

    @Override
    public void exitAction(T context) {
        if (exitFunction != null) {
            exitFunction.call(scriptTable, CoerceJavaToLua.coerce(context));
        }
    }

    @Override
    public AnimationState<T> transition(T context, String condition) {
        if (transitionFunction != null) {
            LuaString conditionToLua = LuaString.valueOf(condition);
            LuaValue nextStateTable = transitionFunction.call(scriptTable, CoerceJavaToLua.coerce(context), conditionToLua);
            if (nextStateTable.istable()) {
                return new LuaAnimationState<>((LuaTable) nextStateTable, scriptTable);
            } else if (nextStateTable.isnil()) {
                return null;
            }
            throw new LuaError("the return of function 'transition' must be table or nil");
        }
        return null;
    }

    private LuaFunction checkLuaFunction(String funcName) {
        LuaValue value = stateTable.get(funcName);
        if (value.isfunction()) {
            return (LuaFunction) value;
        } else if (value.isnil()) {
            return null;
        }
        throw new LuaError("the type of field '" + funcName + "' must be function or nil");
    }
}
