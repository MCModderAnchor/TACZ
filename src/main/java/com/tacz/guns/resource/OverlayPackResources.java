package com.tacz.guns.resource;

import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;

import java.util.List;

/** Uses vanilla overlay precedence while giving the shared ZIP a single owner. */
public final class OverlayPackResources extends CompositePackResources {
    private final FilePackResources.SharedZipFileAccess zip;

    public OverlayPackResources(PackResources base, List<PackResources> overlays,
                                FilePackResources.SharedZipFileAccess zip) {
        super(base, overlays);
        this.zip = zip;
    }

    @Override
    public void close() {
        if (zip != null) {
            zip.close();
        } else {
            super.close();
        }
    }
}
