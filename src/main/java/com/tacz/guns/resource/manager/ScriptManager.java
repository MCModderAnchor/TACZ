package com.tacz.guns.resource.manager;

import com.google.common.collect.Maps;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.vmlib.LuaLibrary;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ScriptManager extends SimplePreparableReloadListener< List<Map.Entry<String, Supplier<LuaTable>>> > {
    private static final Marker MARKER = MarkerManager.getMarker("ScriptLoader");
    private Globals globals;
    private final Map<String, LuaTable> scriptMap = Maps.newHashMap();
    private final FileToIdConverter filetoidconverter;
    private final List<LuaLibrary> libraries;

    public ScriptManager(FileToIdConverter converter, List<LuaLibrary> libraries) {
        this.filetoidconverter = converter;
        this.libraries = libraries;
    }

    @Override
    @NotNull
    protected List<Map.Entry<String, Supplier<LuaTable>>> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        // 初始化 globals
        initGlobals();
        // 打包加载函数，设置 globals 的 preload
        List<Map.Entry<String, Supplier<LuaTable>>> output = new ArrayList<>();
        for(Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(pResourceManager).entrySet()) {
            var wrappedEntry = wrapLoadingFunction(entry.getKey(), entry.getValue());
            output.add(wrappedEntry);
            globals.get("package").get("preload").set(wrappedEntry.getKey(), new LuaFunction() {
                @Override
                public LuaValue call(LuaValue modname, LuaValue env) {
                    return wrappedEntry.getValue().get();
                }
            });
        }
        return output;
    }

    @Override
    protected void apply(List<Map.Entry<String, Supplier<LuaTable>>> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        scriptMap.clear();
        pObject.forEach(entry -> scriptMap.put(entry.getKey(), entry.getValue().get()));
    }

    private Map.Entry<String, Supplier<LuaTable>> wrapLoadingFunction(ResourceLocation rawResourceLocation, Resource resource) {
        ResourceLocation resourceLocation = filetoidconverter.fileToId(rawResourceLocation);
        String moduleName = getModuleName(resourceLocation);
        return new AbstractMap.SimpleEntry<>(moduleName, new Supplier<>() {
            private LuaTable loaded = null;
            @Override
            public LuaTable get() {
                if (loaded != null) {
                    return loaded;
                }
                try (Reader reader = resource.openAsReader()) {
                    LuaValue chunk = globals.load(reader, moduleName);
                    loaded = chunk.call().checktable(1);
                    return loaded;
                } catch (IllegalArgumentException | IOException | JsonParseException | LuaError jsonparseexception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read script file: {}", resourceLocation);
                }
                return null;
            }
        });
    }

    private void initGlobals() {
        globals = JsePlatform.standardGlobals();
        //LuaJC.install(globals);
        if (libraries != null) {
            libraries.forEach(library -> library.install(globals));
        }
    }

    private String getModuleName(ResourceLocation resourceLocation) {
        return resourceLocation.getNamespace() + "_" + resourceLocation.getPath();
    }

    public LuaTable getScript(ResourceLocation id) {
        return scriptMap.get(getModuleName(id));
    }
}
