package com.tacz.guns.api.client.animation;

import com.tacz.guns.client.animation.AnimationController;
import net.minecraftforge.common.util.NonNullSupplier;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;

import java.util.LinkedList;
import java.util.function.Supplier;

public class LuaStateMachineFactory<T extends StateContext> {
    private LuaFunction initializeFunc;
    private LuaFunction exitFunc;
    private LuaFunction statesFunc;
    private AnimationController controller;

    public AnimationStateMachine<T> build(NonNullSupplier<T> contextSupplier) {
        Supplier<Iterable<? extends AnimationState<T>>> statesSupplier = null;
        if (statesFunc != null) {
            statesSupplier = () -> {
                LuaTable statesTable = statesFunc.call().checktable();
                LinkedList<LuaAnimationState<T>> states = new LinkedList<>();
                for (int f = 0; f < statesTable.length(); f++) {
                    LuaTable stateTable = statesTable.get(f).checktable();
                    states.add(new LuaAnimationState<>(stateTable));
                }
                return states;
            };
        }
        return new AnimationStateMachine<>(controller, contextSupplier, statesSupplier);
    }

    public LuaStateMachineFactory<T> setController(AnimationController controller) {
        this.controller = controller;
        return this;
    }

    public LuaStateMachineFactory<T> installLuaChunk(LuaFunction luaFunction) {
        LuaTable table = luaFunction.call().checktable();
        return this;
    }
}
