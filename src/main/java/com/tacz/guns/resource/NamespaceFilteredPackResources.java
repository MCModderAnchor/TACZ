package com.tacz.guns.resource;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraftforge.resource.DelegatingPackResources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Filters enumeration by namespace without changing Forge's delegate order or resource ownership. */
public class NamespaceFilteredPackResources extends DelegatingPackResources {
    private final Map<String, List<PackResources>> assets;
    private final Map<String, List<PackResources>> data;

    public NamespaceFilteredPackResources(String id, boolean builtin, PackMetadataSection metadata,
                                          List<? extends PackResources> packs) {
        super(id, builtin, metadata, packs);
        assets = indexNamespaces(PackType.CLIENT_RESOURCES, packs);
        data = indexNamespaces(PackType.SERVER_DATA, packs);
    }

    private static Map<String, List<PackResources>> indexNamespaces(PackType type, List<? extends PackResources> packs) {
        Map<String, List<PackResources>> namespaces = new HashMap<>();
        for (PackResources pack : packs) {
            for (String namespace : pack.getNamespaces(type)) {
                namespaces.computeIfAbsent(namespace, key -> new ArrayList<>()).add(pack);
            }
        }
        namespaces.replaceAll((namespace, candidates) -> List.copyOf(candidates));
        return Map.copyOf(namespaces);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        Map<String, List<PackResources>> namespaces = type == PackType.CLIENT_RESOURCES ? assets : data;
        // Forge 1.20 enumerates forwards; retain that order for overrides and resource stacks.
        for (PackResources pack : namespaces.getOrDefault(namespace, List.of())) {
            pack.listResources(type, namespace, path, output);
        }
    }
}
