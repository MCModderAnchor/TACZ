package com.tacz.guns.resource;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class PackMeta {
    @SerializedName("namespace")
    private String name;

    @SerializedName("dependencies")
    private HashMap<String, String> dependencies = Maps.newHashMap();

    @SerializedName("overlays")
    private Overlays overlays;

    public PackMeta(String name, HashMap<String, String> dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    public HashMap<String, String> getDependencies() {
        return dependencies;
    }

    public String getName() {
        return name;
    }

    public Overlays getOverlays() {
        return overlays;
    }

    public record Overlays(List<OverlayEntry> entries) { }

    public record OverlayEntry(String directory, String minecraft) { }
}
