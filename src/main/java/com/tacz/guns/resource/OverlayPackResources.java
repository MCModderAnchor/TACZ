package com.tacz.guns.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackResources;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

/** Uses vanilla overlay precedence while giving the shared archive a single owner. */
public final class OverlayPackResources extends CompositePackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private final AutoCloseable archive;

    public OverlayPackResources(PackResources base, List<PackResources> overlays,
                                @Nullable AutoCloseable archive) {
        super(base, overlays);
        this.archive = archive;
    }

    @Override
    public void close() {
        if (archive != null) {
            try {
                archive.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close gun pack archive {}", packId(), e);
            }
        } else {
            super.close();
        }
    }
}
