package com.tacz.guns.resource.manager;

import com.tacz.guns.GunMod;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

/**
 * 配件内联函数的编译与执行。预先编译并保存函数，复用共享的受限环境。
 */
public final class AttachmentScriptManager {
    private static final String[] UNSAFE_GLOBALS = {
            "collectgarbage", "dofile", "loadfile", "load", "print", "getmetatable"
    };
    private static final Globals GLOBALS = secureStandardGlobals();

    private AttachmentScriptManager() {
    }

    @Nullable
    public static CompiledScript compile(@Nullable String script) {
        if (script == null || script.isEmpty()) {
            return null;
        }
        try {
            Prototype prototype = GLOBALS.compilePrototype(
                    new StringReader(script.toLowerCase(Locale.ENGLISH)), "attachment_modifier");
            return new CompiledScript(prototype);
        } catch (IOException | LuaError e) {
            GunMod.LOGGER.warn("Failed to compile attachment modifier function", e);
            return null;
        }
    }

    private static Globals secureStandardGlobals() {
        Globals globals = new Globals();
        globals.load(new BaseLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new JseStringLib());
        globals.load(new JseMathLib());
        // 不加载 package、io、os、luajava、debug 或 coroutine，也不提供动态加载入口。
        for (String name : UNSAFE_GLOBALS) {
            globals.set(name, LuaValue.NIL);
        }
        LuaC.install(globals);
        return globals;
    }

    public static final class CompiledScript {
        private final LuaClosure function;

        private CompiledScript(Prototype prototype) {
            this.function = new LuaClosure(prototype, GLOBALS);
        }

        public double eval(double value, double defaultValue) {
            synchronized (GLOBALS) {
                GLOBALS.rawset("x", LuaValue.valueOf(value));
                GLOBALS.rawset("r", LuaValue.valueOf(defaultValue));
                GLOBALS.rawset("y", LuaValue.NIL);
                try {
                    function.call();
                    LuaValue result = GLOBALS.rawget("y");
                    return result instanceof LuaNumber ? result.todouble() : value;
                } catch (LuaError e) {
                    GunMod.LOGGER.warn("Failed to execute attachment modifier function", e);
                    return value;
                }
            }
        }
    }
}
